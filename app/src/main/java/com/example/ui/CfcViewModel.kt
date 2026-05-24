package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    Home,
    Simulator,
    Result,
    Performance,
    Review
}

class CfcViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CfcRepository
    
    // UI Screen state
    var currentScreen by mutableStateOf(Screen.Home)
        private set

    // Saved Questions flow
    val savedQuestionIds: StateFlow<List<Int>>
    val simulationHistory: StateFlow<List<SimulationResult>>
    val allQuestions: StateFlow<List<Question>>

    // Dark theme preference
    var isDarkMode by mutableStateOf(true)
        private set

    // Streak and Daily Progress
    var userStreak by mutableStateOf(0)
        private set
    var dailyGoalCount by mutableStateOf(0) // questions answered today
    val dailyGoalTarget = 10 // target questions per day

    // Active Simulator states
    var currentExamQuestions by mutableStateOf<List<Question>>(emptyList())
    var currentQuestionIndex by mutableStateOf(0)
    var selectedAnswers by mutableStateOf<Map<Int, String>>(emptyMap()) // index -> option ("A", "B", "C", "D")
    var elapsedSeconds by mutableStateOf(0)
    var isTimerActive by mutableStateOf(true)
    var isTimerVisible by mutableStateOf(true)
    private var timerJob: Job? = null

    // Simulation filter
    var selectedSimulationEdition by mutableStateOf<String?>("Mista")

    // Last Simulation result states for detail review
    var lastExamQuestions by mutableStateOf<List<Question>>(emptyList())
    var lastAnswers by mutableStateOf<Map<Int, String>>(emptyMap())
    var lastCorrectCount by mutableStateOf(0)
    var lastIncorrectCount by mutableStateOf(0)
    var lastPercentRating by mutableStateOf(0.0)
    var lastApproved by mutableStateOf(false)
    var lastDurationSeconds by mutableStateOf(0)
    var reviewSelectedQuestionIndex by mutableStateOf(0) // screen index in result review details

    // Review Mode states
    var bookmarkedQuestionsList by mutableStateOf<List<Question>>(emptyList())
    var savedQuestionReviewIndex by mutableStateOf(0)

    init {
        val database = CfcDatabase.getDatabase(application)
        repository = CfcRepository(database)

        savedQuestionIds = repository.allSavedQuestionIdsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        simulationHistory = repository.allResultsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allQuestions = repository.allQuestionsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Initial seeding and config loading
        viewModelScope.launch {
            repository.initializeAndSeed()
            loadUserPreferences()
            syncDailyTracking()
        }
    }

    private suspend fun loadUserPreferences() {
        val themeVal = repository.getPreference("dark_mode", "true")
        isDarkMode = themeVal == "true"

        val streakVal = repository.getPreference("user_streak", "0")
        userStreak = streakVal.toIntOrNull() ?: 0

        val goalCountVal = repository.getPreference("daily_goal_count", "0")
        dailyGoalCount = goalCountVal.toIntOrNull() ?: 0
    }

    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        viewModelScope.launch {
            repository.setPreference("dark_mode", isDarkMode.toString())
        }
    }

    fun navigateTo(screen: Screen) {
        currentScreen = screen
        if (screen == Screen.Review) {
            loadBookmarkedQuestions()
        }
    }

    // Daily active tracking
    private suspend fun syncDailyTracking() {
        val lastActiveDate = repository.getPreference("last_active_date", "")
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (lastActiveDate != todayStr) {
            // It's a new day! Check streak continuity
            if (lastActiveDate.isNotEmpty()) {
                val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date(System.currentTimeMillis() - 86400000)
                )
                if (lastActiveDate != yesterdayStr) {
                    // Streak broken
                    userStreak = 0
                    repository.setPreference("user_streak", "0")
                }
            }
            // Reset today's daily count
            dailyGoalCount = 0
            repository.setPreference("daily_goal_count", "0")
        }
    }

    private fun registerStudyActivity(answeredCount: Int) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val lastActiveDate = repository.getPreference("last_active_date", "")

            if (lastActiveDate != todayStr) {
                // Increment streak
                userStreak += 1
                repository.setPreference("user_streak", userStreak.toString())
                repository.setPreference("last_active_date", todayStr)
            }

            dailyGoalCount += answeredCount
            repository.setPreference("daily_goal_count", dailyGoalCount.toString())
        }
    }

    // SIMULATION ENGINE

    fun startSimulation(edition: String?) {
        viewModelScope.launch {
            val questions = repository.getSimulationQuestions(edition)
            if (questions.isEmpty()) return@launch

            // If we have less than 50 questions, take all, but since we seeded 60 questions, it will be exactly 50!
            currentExamQuestions = questions
            currentQuestionIndex = 0
            selectedAnswers = emptyMap()
            elapsedSeconds = 0
            isTimerActive = true
            reviewSelectedQuestionIndex = 0

            startTimer()
            navigateTo(Screen.Simulator)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isTimerActive) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    fun toggleTimerVisibility() {
        isTimerVisible = !isTimerVisible
    }

    fun selectAnswer(questionIndex: Int, option: String) {
        val updated = selectedAnswers.toMutableMap()
        updated[questionIndex] = option
        selectedAnswers = updated
    }

    fun nextQuestion() {
        if (currentQuestionIndex < currentExamQuestions.size - 1) {
            currentQuestionIndex++
        }
    }

    fun previousQuestion() {
        if (currentQuestionIndex > 0) {
            currentQuestionIndex--
        }
    }

    fun finishSimulation() {
        isTimerActive = false
        timerJob?.cancel()

        var correct = 0
        var incorrect = 0

        val answersJsonMap = mutableMapOf<Int, String>()

        currentExamQuestions.forEachIndexed { index, question ->
            val selected = selectedAnswers[index]
            answersJsonMap[question.id] = selected ?: ""
            if (selected == question.correctAnswer) {
                correct++
            } else {
                incorrect++
            }
        }

        val total = currentExamQuestions.size
        val percent = if (total > 0) (correct.toDouble() / total) * 100.0 else 0.0
        // Minimum passing grade criteria in CFC: 50% correct, which is 25 questions out of 50
        val approved = correct >= 25 

        lastCorrectCount = correct
        lastIncorrectCount = incorrect
        lastPercentRating = percent
        lastApproved = approved
        lastDurationSeconds = elapsedSeconds
        lastExamQuestions = currentExamQuestions
        lastAnswers = selectedAnswers

        // Convert answer mapping to JSON string to save
        val m = Moshi.Builder().build()
        val type = Types.newParameterizedType(Map::class.java, java.lang.Integer::class.java, String::class.java)
        val adapter = m.adapter<Map<Int, String>>(type)
        val answersJson = adapter.toJson(answersJsonMap)

        val resultEntity = SimulationResult(
            edition = selectedSimulationEdition ?: "Mista",
            correctAnswers = correct,
            incorrectAnswers = incorrect,
            percentRating = percent,
            approved = approved,
            durationSeconds = elapsedSeconds,
            answersDataJson = answersJson
        )

        viewModelScope.launch {
            repository.saveSimulationResult(resultEntity)
            registerStudyActivity(selectedAnswers.size)
        }

        navigateTo(Screen.Result)
    }

    // BOOKMARKS / REVIEW MODE

    fun loadBookmarkedQuestions() {
        viewModelScope.launch {
            bookmarkedQuestionsList = repository.getBookmarkedQuestions()
            savedQuestionReviewIndex = 0
        }
    }

    fun toggleBookmark(questionId: Int) {
        viewModelScope.launch {
            val currentlySaved = repository.isQuestionSaved(questionId)
            if (currentlySaved) {
                repository.removeFeedbackAndReview(questionId)
            } else {
                repository.saveForReview(questionId)
            }
            // Update bookmarked list if in Review screen
            if (currentScreen == Screen.Review) {
                val updatedBookmarks = repository.getBookmarkedQuestions()
                if (savedQuestionReviewIndex >= updatedBookmarks.size && updatedBookmarks.isNotEmpty()) {
                    savedQuestionReviewIndex = updatedBookmarks.size - 1
                }
                bookmarkedQuestionsList = updatedBookmarks
            }
        }
    }

    fun isQuestionSavedFlow(questionId: Int) = repository.isQuestionSavedFlow(questionId)

    fun removeAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // QUESTION IMPORT VIA JSON
    suspend fun importQuestionsJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val type = Types.newParameterizedType(List::class.java, ImportedQuestion::class.java)
            val adapter = moshi.adapter<List<ImportedQuestion>>(type)

            val parsedList = adapter.fromJson(jsonString) ?: return@withContext Result.failure(Exception("JSON inválido ou vazio"))

            val questionsList = parsedList.map { imp ->
                Question(
                    statement = imp.statement ?: "",
                    optionA = imp.optionA ?: "",
                    optionB = imp.optionB ?: "",
                    optionC = imp.optionC ?: "",
                    optionD = imp.optionD ?: "",
                    correctAnswer = imp.correctAnswer ?: "A",
                    subject = imp.subject ?: "Contabilidade Geral",
                    difficulty = imp.difficulty ?: "Médio",
                    edition = imp.edition ?: "2026.1",
                    correctExplanation = imp.correctExplanation ?: "Esclarecimento geral.",
                    explanationA = imp.explanationA ?: "Opção A.",
                    explanationB = imp.explanationB ?: "Opção B.",
                    explanationC = imp.explanationC ?: "Opção C.",
                    explanationD = imp.explanationD ?: "Opção D.",
                    summary = imp.summary ?: "Mini resumo contábil."
                )
            }

            repository.importQuestions(questionsList)
            return@withContext Result.success(questionsList.size)
        } catch (e: Exception) {
            Log.e("CfcViewModel", "Error parsing import JSON: ${e.message}", e)
            return@withContext Result.failure(e)
        }
    }
}

// Intermediary schema matching the JSON fields exactly for safe imports
data class ImportedQuestion(
    val statement: String?,
    val optionA: String?,
    val optionB: String?,
    val optionC: String?,
    val optionD: String?,
    val correctAnswer: String?,
    val subject: String?,
    val difficulty: String?,
    val edition: String?,
    val correctExplanation: String?,
    val explanationA: String?,
    val explanationB: String?,
    val explanationC: String?,
    val explanationD: String?,
    val summary: String?
)
