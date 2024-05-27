    package com.example.composetutorial

    import android.Manifest
    import android.app.NotificationChannel
    import android.app.NotificationManager
    import android.app.PendingIntent
    import android.content.Context
    import android.content.Intent
    import android.content.pm.PackageManager
    import android.os.Bundle
    import androidx.activity.ComponentActivity
    import androidx.activity.compose.setContent
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.layout.*
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.tooling.preview.Preview
    import com.example.composetutorial.ui.theme.ComposeTutorialTheme
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.unit.dp
    import androidx.compose.foundation.border
    import android.content.res.Configuration
    import android.hardware.Sensor
    import android.hardware.SensorEvent
    import android.hardware.SensorEventListener
    import android.hardware.SensorManager
    import android.net.Uri
    import android.os.Build
    import android.util.Log
    import androidx.compose.foundation.lazy.LazyColumn
    import androidx.compose.foundation.lazy.items
    import androidx.compose.foundation.clickable
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.animation.animateColorAsState
    import androidx.compose.animation.animateContentSize
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.Surface
    import androidx.activity.compose.BackHandler
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.PickVisualMediaRequest
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Settings
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.Text
    import androidx.compose.material3.TopAppBar
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.core.net.toUri
    import coil.compose.rememberAsyncImagePainter
    import kotlinx.coroutines.launch
    import androidx.compose.runtime.rememberCoroutineScope
    import androidx.compose.ui.Alignment
    import androidx.core.app.NotificationCompat
    import androidx.core.app.NotificationManagerCompat
    import androidx.core.content.ContextCompat
    import kotlin.math.pow


    enum class ViewType { VIEW_A, VIEW_B }

    class MainActivity : ComponentActivity(), SensorEventListener {
        private lateinit var sensorManager: SensorManager
        private lateinit var accelerometer: Sensor
        private val CHANNEL_ID = "rotation_channel"
        private val notificationId = 101

        private val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
                // Send an initial notification to confirm permission
                sendNotification("Permission Granted", "You will receive notifications.")
            } else {
                Log.d("MainActivity", "Notification permission denied")
                sendNotification("Permission Denied", "Notifications are disabled.")
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            createNotificationChannel()
            setContent {
                ComposeTutorialTheme {
                    var currentView by remember { mutableStateOf(ViewType.VIEW_B) }
                    val messages = SampleData.conversationSample

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentView) {
                            ViewType.VIEW_A -> ViewA(onNavigateToViewB = { currentView = ViewType.VIEW_B })
                            ViewType.VIEW_B -> ViewB(onNavigateToViewA = { currentView = ViewType.VIEW_A }, messages = messages)
                        }
                    }

                    BackHandler {
                        if (currentView == ViewType.VIEW_A) {
                            currentView = ViewType.VIEW_B
                        } else {
                            finish()
                        }
                    }
                }
            }

            requestNotificationPermission()

            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        }

        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            Log.d("MainActivity", "Orientation changed")
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Log.d("MainActivity", "Orientation changed to Landscape")
                    sendNotification("Orientation Changed", "Landscape mode")
                } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    Log.d("MainActivity", "Orientation changed to Portrait")
                    sendNotification("Orientation Changed", "Portrait mode")
                }
            } else {
                Log.d(
                    "MainActivity",
                    "Notification permission not granted, cannot send notification on orientation change"
                )
            }
        }

        private fun requestNotificationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                when {
                    ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                        Log.d("MainActivity", "Notification permission already granted")
                    }
                    shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                        Log.d("MainActivity", "Showing permission rationale")
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    else -> {
                        Log.d("MainActivity", "Requesting notification permission")
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }

        private fun createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Rotation Channel"
                val descriptionText = "Channel for rotation notifications"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }
                val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        private fun sendNotification(title: String, content: String) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // replace with your app icon
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            Log.d("MainActivity", "Sending notification")
            with(NotificationManagerCompat.from(this)) {
                notify(notificationId, builder.build())
            }
        }

        override fun onResume() {
            super.onResume()
            accelerometer.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }

        override fun onPause() {
            super.onPause()
            sensorManager.unregisterListener(this)
        }

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor == accelerometer) {
                // Check for significant changes in accelerometer data
                if (event.sensor == accelerometer) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    // Calculate the total acceleration
                    val acceleration = Math.sqrt(x.toDouble().pow(2.0) + y.toDouble().pow(2.0) + z.toDouble().pow(2.0))

                    // Define a threshold for significant acceleration change
                    val threshold = 15.0 // Adjust this threshold as needed

                    // Check if the acceleration change exceeds the threshold
                    if (acceleration > threshold) {
                        showNotification()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Check if the sensor is the accelerometer
            if (sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                // Handle accuracy changes for the accelerometer
                when (accuracy) {
                    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> {
                        // Accelerometer accuracy is high
                        // You can perform additional actions here
                    }

                    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> {
                        // Accelerometer accuracy is medium
                        // You can perform additional actions here
                    }

                    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> {
                        // Accelerometer accuracy is low
                        // You can perform additional actions here
                    }

                    SensorManager.SENSOR_STATUS_UNRELIABLE -> {
                        // Accelerometer data is unreliable
                        // You can perform additional actions here
                    }
                }
            }
        }

        private fun showNotification() {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("Notification Title")
                .setContentText("Notification Body")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        companion object {
            private const val CHANNEL_ID = "channel_id"
            private const val NOTIFICATION_ID = 1
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ViewA(onNavigateToViewB: () -> Unit) {
        val context = LocalContext.current
        val db = remember { UserDatabase.getDatabase(context) }
        var username by remember { mutableStateOf("") }
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                val user = db.userDao().getAll().firstOrNull()
                user?.let {
                    username = it.username
                    imageUri = it.imageUri.toUri()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Settings", Modifier.weight(1f)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateToViewB) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Go back")
                    }
                }
            )

            TextField(
                value = username,
                onValueChange = { newValue ->
                    username = newValue
                    db.userDao().setUsername(0, username)
                },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth()
            )

            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri: Uri? ->
                    if (uri != null) {
                        val newUri = copying(context, uri)
                        db.userDao().setimageUri(0, newUri.toString())
                        imageUri = newUri
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.Gray, CircleShape)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val users = db.userDao().getAll()
                        users.forEach { user ->
                            db.userDao().setUsername(user.id, username)
                            db.userDao().setimageUri(user.id, imageUri.toString())
                        }
                    }
                },
            ) {
                Text("Save Changes")
            }
        }
    }

    fun copying(context: Context, newUri: Uri): Uri {
        context.contentResolver.openInputStream(newUri).use { stream ->
            val outputFile = context.filesDir.resolve("profile_picture.jpg" + System.currentTimeMillis())
            stream!!.copyTo(outputFile.outputStream())
            return outputFile.toUri()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ViewB(onNavigateToViewA: () -> Unit, messages: List<Message>) {
        val context = LocalContext.current
        val db = remember { UserDatabase.getDatabase(context) }
        var username by remember { mutableStateOf("") }
        var imageUri by remember { mutableStateOf<Uri?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                val user = db.userDao().getAll().firstOrNull()
                user?.let {
                    username = it.username
                    imageUri = it.imageUri.toUri()
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Messages") },
                actions = {
                    IconButton(onClick = onNavigateToViewA) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                    }
                }
            )

            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = username,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Conversation(messages)
        }
    }

    data class Message(val author: String, val body: String)

    @Composable
    fun MessageCard(msg: Message) {
        Row(modifier = Modifier.padding(all = 8.dp)) {
            Image(
                painter = painterResource(R.drawable.profile_picture),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            var isExpanded by remember { mutableStateOf(false) }
            val surfaceColor by animateColorAsState(
                if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )

            Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
                Text(
                    text = msg.author,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 1.dp,
                    color = surfaceColor,
                    modifier = Modifier
                        .animateContentSize()
                        .padding(1.dp)
                ) {
                    Text(
                        text = msg.body,
                        modifier = Modifier.padding(all = 4.dp),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Composable
    fun Conversation(messages: List<Message>) {
        LazyColumn {
            items(messages) { message ->
                MessageCard(message)
            }
        }
    }


    /*
    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        ComposeTutorialTheme {
            Greeting("Android")
        }
    }
    */
