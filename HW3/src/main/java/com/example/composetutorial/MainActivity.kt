    package com.example.composetutorial

    import android.content.Context
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
    import android.net.Uri
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



    enum class ViewType { VIEW_A, VIEW_B }

    class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
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
