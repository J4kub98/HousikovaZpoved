package com.example.housikovazpoved

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import kotlin.random.Random

@Serializable
data class QuestionData(val questions: List<String>)

data class Question(
    val text: String,
    val isBonus: Boolean = false
)

data class GameUiState(
    val currentQuestion: Question? = null,
    val isLoading: Boolean = true,
    val isGameOver: Boolean = false,
    val questionsAvailable: Int = 0,
    val canGoBack: Boolean = false,
    val players: List<String> = emptyList(),
    val currentPlayerIndex: Int = 0
) {
    val currentPlayerName: String?
        get() = players.getOrNull(currentPlayerIndex)
}

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var allQuestions: List<Question> = emptyList()
    private var availableQuestions: MutableList<Question> = mutableListOf()
    private var usedQuestions: MutableList<Question> = mutableListOf()

    fun setPlayers(players: List<String>) {
        _uiState.update { it.copy(players = players, currentPlayerIndex = 0) }
    }

    fun loadQuestions(context: Context) {
        viewModelScope.launch {
            try {
                val jsonString = context.assets.open("questions.json")
                    .bufferedReader()
                    .use { it.readText() }

                val questionData = Json.decodeFromString<QuestionData>(jsonString)
                val questionsAsStrings = questionData.questions

                if (questionsAsStrings.isNotEmpty()) {
                    // ===== NOVÁ LOGIKA PRO BONUSOVÉ OTÁZKY =====
                    // Každá otázka má 10% šanci, že bude bonusová.
                    var tempQuestions = questionsAsStrings.map {
                        Question(it, isBonus = Random.nextInt(1, 11) == 1)
                    }.toMutableList()

                    // Pojistka: Pokud náhodou žádná otázka není bonusová,
                    // vybereme jednu náhodně a uděláme ji bonusovou.
                    if (tempQuestions.none { it.isBonus }) {
                        val bonusIndex = Random.nextInt(0, tempQuestions.size)
                        tempQuestions[bonusIndex] = tempQuestions[bonusIndex].copy(isBonus = true)
                    }

                    allQuestions = tempQuestions
                }

                restartGame()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(currentQuestion = null, isLoading = false, isGameOver = true) }
            }
        }
    }

    fun nextQuestion() {
        if (availableQuestions.isNotEmpty()) {
            _uiState.value.currentQuestion?.let { usedQuestions.add(it) }

            val nextQuestion = availableQuestions.removeFirst()
            val state = _uiState.value

            val nextPlayerIndex = if (nextQuestion.isBonus) {
                state.currentPlayerIndex
            } else if (state.players.isNotEmpty()) {
                (state.currentPlayerIndex + 1) % state.players.size
            } else {
                0
            }

            _uiState.update {
                it.copy(
                    currentQuestion = nextQuestion,
                    questionsAvailable = availableQuestions.size,
                    isGameOver = false,
                    canGoBack = true,
                    currentPlayerIndex = nextPlayerIndex
                )
            }
        } else {
            _uiState.value.currentQuestion?.let { usedQuestions.add(it) }
            _uiState.update { it.copy(isGameOver = true) }
        }
    }

    fun previousQuestion() {
        if (usedQuestions.isNotEmpty()) {
            _uiState.value.currentQuestion?.let { availableQuestions.add(0, it) }
            val previousQuestion = usedQuestions.removeLast()
            val state = _uiState.value

            val previousPlayerIndex = if (previousQuestion.isBonus) {
                state.currentPlayerIndex
            } else if (state.players.isNotEmpty()) {
                (state.currentPlayerIndex - 1 + state.players.size) % state.players.size
            } else {
                0
            }

            _uiState.update {
                it.copy(
                    currentQuestion = previousQuestion,
                    questionsAvailable = availableQuestions.size,
                    isGameOver = false,
                    canGoBack = usedQuestions.isNotEmpty(),
                    currentPlayerIndex = previousPlayerIndex
                )
            }
        }
    }

    fun restartGame() {
        availableQuestions = allQuestions.shuffled().toMutableList()
        usedQuestions.clear()

        if (availableQuestions.isNotEmpty()) {
            val firstQuestion = availableQuestions.removeFirst()
            _uiState.update {
                it.copy(
                    currentQuestion = firstQuestion,
                    isLoading = false,
                    isGameOver = false,
                    questionsAvailable = availableQuestions.size,
                    canGoBack = false,
                    currentPlayerIndex = 0
                )
            }
        } else {
            _uiState.update { it.copy(currentQuestion = null, isLoading = false, isGameOver = true) }
        }
    }
}