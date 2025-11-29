package com.example.appointmentmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CallRecord::class,
        SlotConfiguration::class
               ], version = 3, exportSchema = false)
abstract class AppDatabase: RoomDatabase() {
    abstract fun callDao(): CallDao

    companion object{

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new columns to the existing table
                db.execSQL("ALTER TABLE call_records ADD COLUMN appointmentDate TEXT")
                db.execSQL("ALTER TABLE call_records ADD COLUMN appointmentSlot TEXT")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create the new slot_configurations table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS slot_configurations (
                        slotTime TEXT PRIMARY KEY NOT NULL,
                        capacity INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
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
                        .addMigrations(MIGRATION_1_2,MIGRATION_2_3)
                        .build()
                }
            }
            return INSTANCE!!
        }
    }

}