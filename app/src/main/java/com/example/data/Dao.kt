package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions")
    fun getAllQuestionsFlow(): Flow<List<Question>>

    @Query("SELECT * FROM questions")
    suspend fun getAllQuestions(): List<Question>

    @Query("SELECT * FROM questions WHERE edition = :edition")
    suspend fun getQuestionsByEdition(edition: String): List<Question>

    @Query("SELECT * FROM questions WHERE id IN (:ids)")
    suspend fun getQuestionsByIds(ids: List<Int>): List<Question>

    @Query("SELECT COUNT(*) FROM questions")
    suspend fun getQuestionCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<Question>)

    @Query("DELETE FROM questions")
    suspend fun deleteAllQuestions()
}

@Dao
interface SavedQuestionDao {
    @Query("SELECT questionId FROM saved_questions")
    fun getAllSavedQuestionIdsFlow(): Flow<List<Int>>

    @Query("SELECT * FROM saved_questions")
    suspend fun getAllSavedQuestions(): List<SavedQuestion>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_questions WHERE questionId = :questionId)")
    fun isSavedQuestionFlow(questionId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM saved_questions WHERE questionId = :questionId)")
    suspend fun isSavedQuestion(questionId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedQuestion(saved: SavedQuestion)

    @Query("DELETE FROM saved_questions WHERE questionId = :questionId")
    suspend fun deleteSavedQuestion(questionId: Int)

    @Query("DELETE FROM saved_questions")
    suspend fun deleteAllSavedQuestions()
}

@Dao
interface SimulationResultDao {
    @Query("SELECT * FROM simulation_results ORDER BY timestamp DESC")
    fun getAllResultsFlow(): Flow<List<SimulationResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: SimulationResult)

    @Query("DELETE FROM simulation_results")
    suspend fun deleteAllResults()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    suspend fun getSetting(key: String): AppSetting?

    @Query("SELECT * FROM app_settings WHERE `key` = :key")
    fun getSettingFlow(key: String): Flow<AppSetting?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSetting)
}
