package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Question::class, SavedQuestion::class, SimulationResult::class, AppSetting::class],
    version = 1,
    exportSchema = false
)
abstract class CfcDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun savedQuestionDao(): SavedQuestionDao
    abstract fun simulationResultDao(): SimulationResultDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: CfcDatabase? = null

        fun getDatabase(context: Context): CfcDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CfcDatabase::class.java,
                    "cfc_quest_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
