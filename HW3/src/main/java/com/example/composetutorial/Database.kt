package com.example.composetutorial

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import android.content.Context
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.room.Database
import androidx.room.Delete
import androidx.room.OnConflictStrategy
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val username: String,
    val imageUri: String
)

@Dao
interface UserDao {
    @Query("SELECT * FROM user WHERE id = :id")
    fun getUser(id: Int): User

    @Query("SELECT * FROM user")
    fun getAll(): List<User>

    @Query("UPDATE user SET username = :newUsername WHERE id = :uid")
    fun setUsername(uid: Int, newUsername: String)

    @Query("SELECT imageUri FROM user WHERE id LIKE :userID LIMIT 1")
    fun getimageUri(userID: Int): String

    @Query("UPDATE user SET imageUri = :newUri WHERE id = :uid")
    fun setimageUri(uid: Int, newUri: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: User)
}

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries() // For simplicity, allow queries on the main thread.
                    .build()
                INSTANCE = instance

                if (instance.userDao().getAll().isEmpty()) {
                    instance.userDao().insertAll(User(0, "Nick", ""))
                }
                instance
            }
        }

        private fun UserDatabase.prepopulateDatabase() {
            if (this.userDao().getAll().isEmpty()) {
                this.userDao().insertAll(User(0, "Nick", ""))
            }
        }
    }
}
