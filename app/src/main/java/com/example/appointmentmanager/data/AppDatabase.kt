package com.example.appointmentmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [CallRecord::class], version = 2, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object{

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new columns to the existing table
                db.execSQL("ALTER TABLE call_history ADD COLUMN appointmentDate TEXT")
                db.execSQL("ALTER TABLE call_history ADD COLUMN appointmentSlot TEXT")
            }
        }
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase{
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "appointment_database"
                    )
                        .addMigrations(MIGRATION_1_2)
                        .build()
                }
            }
            return INSTANCE!!
        }
    }

}