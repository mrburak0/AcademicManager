package com.example.academicmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [CourseEntity::class, LecturerEntity::class, UserEntity::class],
    version = 8,
    exportSchema = false
)
abstract class UniversityDatabase : RoomDatabase() {

    abstract fun universityDao(): UniversityDao

    companion object {
        @Volatile
        private var INSTANCE: UniversityDatabase? = null

        fun getDatabase(context: Context): UniversityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UniversityDatabase::class.java,
                    "university_database"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getDatabase(context).universityDao()
                            if (dao.getUser("user") == null) {
                                dao.insertUser(UserEntity(
                                    username = "user",
                                    password = "root",
                                    fullName = "System Admin",
                                    role = UserRole.ADMIN,
                                    status = UserStatus.APPROVED
                                ))
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
