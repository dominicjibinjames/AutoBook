package com.example.appointmentmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CallRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object{
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase{
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "appointment_database"
                    ).build()
                }
            }
            return INSTANCE!!
        }
    }

}