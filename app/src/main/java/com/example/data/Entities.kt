package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val statement: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val subject: String,
    val difficulty: String, // "Fácil", "Médio", "Difícil"
    val edition: String, // "2025.2", "2026.1"
    val correctExplanation: String,
    val explanationA: String,
    val explanationB: String,
    val explanationC: String,
    val explanationD: String,
    val summary: String
)

@Entity(tableName = "saved_questions")
data class SavedQuestion(
    @PrimaryKey val questionId: Int,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "simulation_results")
data class SimulationResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val edition: String, // "2025.2", "2026.1", "Mista"
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val percentRating: Double,
    val approved: Boolean,
    val durationSeconds: Int,
    val answersDataJson: String // JSON string representantion of Map<Int, String> (questionId -> selectedAnswer)
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)
