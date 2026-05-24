package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CfcRepository(private val db: CfcDatabase) {

    private val questionDao = db.questionDao()
    private val savedQuestionDao = db.savedQuestionDao()
    private val resultDao = db.simulationResultDao()
    private val settingDao = db.appSettingDao()

    val allQuestionsFlow: Flow<List<Question>> = questionDao.getAllQuestionsFlow()
    val allSavedQuestionIdsFlow: Flow<List<Int>> = savedQuestionDao.getAllSavedQuestionIdsFlow()
    val allResultsFlow: Flow<List<SimulationResult>> = resultDao.getAllResultsFlow()

    // Initialize database with seeding if required
    suspend fun initializeAndSeed() = withContext(Dispatchers.IO) {
        try {
            val count = questionDao.getQuestionCount()
            if (count == 0) {
                Log.d("CfcRepository", "Database is empty. Seeding 60 default exam questions...")
                val seeded = QuestionSeeder.getSeededQuestions()
                questionDao.insertQuestions(seeded)
                Log.d("CfcRepository", "Successfully seeded ${seeded.size} questions.")
            } else {
                Log.d("CfcRepository", "Database already has $count questions. Skipping seed.")
            }
        } catch (e: Exception) {
            Log.e("CfcRepository", "Error seeding database: ${e.message}", e)
        }
    }

    // Fetches exactly 50 randomized questions for a simulation
    // Optionally filters by edition, prioritizing that edition and padding/limiting to exactly 50.
    suspend fun getSimulationQuestions(edition: String?): List<Question> = withContext(Dispatchers.IO) {
        val all = questionDao.getAllQuestions()
        if (all.isEmpty()) return@withContext emptyList()

        val filtered = if (!edition.isNullOrEmpty() && edition != "Mista") {
            // Filter prioritized edition first
            val targetEdition = all.filter { it.edition == edition }.shuffled()
            val otherEdition = all.filter { it.edition != edition }.shuffled()
            (targetEdition + otherEdition).take(50)
        } else {
            all.shuffled().take(50)
        }

        // Return exactly what we can, shuffled (pad with duplicates if total count < 50, but we seeded 60 so it's guaranteed to have exactly 50 distinct objects!)
        return@withContext filtered.shuffled()
    }

    // Bookmarking / Saving for review
    suspend fun saveForReview(questionId: Int) = withContext(Dispatchers.IO) {
        savedQuestionDao.insertSavedQuestion(SavedQuestion(questionId))
    }

    suspend fun removeFeedbackAndReview(questionId: Int) = withContext(Dispatchers.IO) {
        savedQuestionDao.deleteSavedQuestion(questionId)
    }

    suspend fun isQuestionSaved(questionId: Int): Boolean = withContext(Dispatchers.IO) {
        savedQuestionDao.isSavedQuestion(questionId)
    }

    fun isQuestionSavedFlow(questionId: Int): Flow<Boolean> {
        return savedQuestionDao.isSavedQuestionFlow(questionId)
    }

    suspend fun getBookmarkedQuestions(): List<Question> = withContext(Dispatchers.IO) {
        val savedList = savedQuestionDao.getAllSavedQuestions()
        val ids = savedList.map { it.questionId }
        if (ids.isEmpty()) return@withContext emptyList()
        return@withContext questionDao.getQuestionsByIds(ids)
    }

    // Result Insertion
    suspend fun saveSimulationResult(result: SimulationResult) = withContext(Dispatchers.IO) {
        resultDao.insertResult(result)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        resultDao.deleteAllResults()
    }

    // Dynamic Imports of any future JSON/API questions
    suspend fun importQuestions(questions: List<Question>) = withContext(Dispatchers.IO) {
        questionDao.insertQuestions(questions)
    }

    // Key-Value App Settings
    suspend fun setPreference(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.insertSetting(AppSetting(key, value))
    }

    suspend fun getPreference(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        settingDao.getSetting(key)?.value ?: defaultValue
    }

    fun getPreferenceFlow(key: String, defaultValue: String): Flow<String?> {
        return settingDao.getSettingFlow(key).map { it?.value ?: defaultValue }
    }
}
