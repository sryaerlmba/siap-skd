package com.belajar.cpns.di

import android.content.Context
import com.belajar.cpns.data.MaterialRepository
import com.belajar.cpns.data.QuestionRepository
import com.belajar.cpns.data.SessionRepository
import com.belajar.cpns.data.db.SkdDatabase
import com.belajar.cpns.data.prefs.AppPreferences

interface AppContainer {
    val preferences: AppPreferences
    val questions: QuestionRepository
    val sessions: SessionRepository
    val materials: MaterialRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    private val appContext = context.applicationContext
    private val database: SkdDatabase by lazy { SkdDatabase.build(appContext) }

    override val preferences: AppPreferences by lazy { AppPreferences(appContext) }
    override val questions: QuestionRepository by lazy { QuestionRepository(appContext.assets, database) }
    override val sessions: SessionRepository by lazy { SessionRepository(database, questions) }
    override val materials: MaterialRepository by lazy { MaterialRepository(appContext.assets) }
}
