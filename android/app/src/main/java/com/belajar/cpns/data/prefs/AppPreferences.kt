package com.belajar.cpns.data.prefs

import android.content.Context
import com.belajar.cpns.data.model.ApplicantCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {

    private val prefs = context.getSharedPreferences("siap_skd_prefs", Context.MODE_PRIVATE)

    private val _category = MutableStateFlow(
        runCatching { ApplicantCategory.valueOf(prefs.getString(KEY_CATEGORY, null) ?: "") }
            .getOrDefault(ApplicantCategory.UMUM)
    )
    val category: StateFlow<ApplicantCategory> = _category.asStateFlow()

    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    /** Tanggal ujian dalam epoch millis, 0 jika belum diisi. */
    private val _examDate = MutableStateFlow(prefs.getLong(KEY_EXAM_DATE, 0L))
    val examDate: StateFlow<Long> = _examDate.asStateFlow()

    private val _extendedTime = MutableStateFlow(prefs.getBoolean(KEY_EXTENDED_TIME, false))
    val extendedTime: StateFlow<Boolean> = _extendedTime.asStateFlow()

    private val _dailyTimer = MutableStateFlow(prefs.getBoolean(KEY_DAILY_TIMER, true))
    val dailyTimer: StateFlow<Boolean> = _dailyTimer.asStateFlow()

    private val _userName = MutableStateFlow(prefs.getString(KEY_NAME, "") ?: "")
    val userName: StateFlow<String> = _userName.asStateFlow()

    /** Skala huruf halaman materi (1.0 = normal), diubah lewat cubit atau tombol A-/A+. */
    private val _readFontScale = MutableStateFlow(prefs.getFloat(KEY_READ_FONT_SCALE, 1f))
    val readFontScale: StateFlow<Float> = _readFontScale.asStateFlow()

    fun setReadFontScale(value: Float) {
        val v = value.coerceIn(MIN_FONT_SCALE, MAX_FONT_SCALE)
        prefs.edit().putFloat(KEY_READ_FONT_SCALE, v).apply(); _readFontScale.value = v
    }

    /** Id halaman materi yang sudah ditandai dibaca. */
    private val _readPages = MutableStateFlow(prefs.getStringSet(KEY_READ_PAGES, emptySet()) ?: emptySet())
    val readPages: StateFlow<Set<String>> = _readPages.asStateFlow()

    fun toggleRead(pageId: String) {
        val next = if (pageId in _readPages.value) _readPages.value - pageId else _readPages.value + pageId
        prefs.edit().putStringSet(KEY_READ_PAGES, next).apply(); _readPages.value = next
    }

    fun setCategory(value: ApplicantCategory) {
        prefs.edit().putString(KEY_CATEGORY, value.name).apply(); _category.value = value
    }

    fun markOnboardingDone() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply(); _onboardingDone.value = true
    }

    fun setExamDate(epochMillis: Long) {
        prefs.edit().putLong(KEY_EXAM_DATE, epochMillis).apply(); _examDate.value = epochMillis
    }

    fun setExtendedTime(value: Boolean) {
        prefs.edit().putBoolean(KEY_EXTENDED_TIME, value).apply(); _extendedTime.value = value
    }

    fun setDailyTimer(value: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_TIMER, value).apply(); _dailyTimer.value = value
    }

    fun setUserName(value: String) {
        prefs.edit().putString(KEY_NAME, value).apply(); _userName.value = value
    }

    companion object {
        private const val KEY_CATEGORY = "category"
        private const val KEY_ONBOARDING = "onboarding_done"
        private const val KEY_EXAM_DATE = "exam_date"
        private const val KEY_EXTENDED_TIME = "extended_time"
        private const val KEY_DAILY_TIMER = "daily_timer"
        private const val KEY_NAME = "user_name"
        private const val KEY_READ_PAGES = "read_pages"
        private const val KEY_READ_FONT_SCALE = "read_font_scale"
        const val MIN_FONT_SCALE = 0.8f
        const val MAX_FONT_SCALE = 1.8f
    }
}
