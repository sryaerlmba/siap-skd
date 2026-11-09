package com.belajar.cpns.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.model.ApplicantCategory
import com.belajar.cpns.data.prefs.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val questions: QuestionRepository,
    private val sessions: SessionRepository,
    val preferences: AppPreferences
) : ViewModel() {

    val config get() = questions.scoringConfig

    private val _bankInfo = MutableStateFlow("")
    val bankInfo: StateFlow<String> = _bankInfo.asStateFlow()

    init {
        viewModelScope.launch { _bankInfo.value = "Versi ${questions.bankVersion()} · ${questions.count()} soal" }
    }

    fun setCategory(c: ApplicantCategory) = preferences.setCategory(c)
    fun setExamDate(millis: Long) = preferences.setExamDate(millis)
    fun setExtendedTime(v: Boolean) = preferences.setExtendedTime(v)
    fun setDailyTimer(v: Boolean) = preferences.setDailyTimer(v)
    fun setUserName(v: String) = preferences.setUserName(v)

    fun clearProgress(onDone: () -> Unit) {
        viewModelScope.launch { sessions.clearProgress(); onDone() }
    }
}
