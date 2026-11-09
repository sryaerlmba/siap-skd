package com.belajar.cpns.ui

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.belajar.cpns.SiapSkdApp
import com.belajar.cpns.ui.home.HomeViewModel
import com.belajar.cpns.ui.latihan.LatihanViewModel
import com.belajar.cpns.ui.materi.MateriDetailViewModel
import com.belajar.cpns.ui.materi.MateriViewModel
import com.belajar.cpns.ui.progres.ProgresViewModel
import com.belajar.cpns.ui.quiz.QuizViewModel
import com.belajar.cpns.ui.result.ResultViewModel
import com.belajar.cpns.ui.review.ReviewViewModel
import com.belajar.cpns.ui.settings.SettingsViewModel
import com.belajar.cpns.ui.tryout.TryoutViewModel

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer { HomeViewModel(c().questions, c().sessions, c().preferences, c().materials) }
        initializer { TryoutViewModel(c().questions, c().sessions, c().preferences, createSavedStateHandle()) }
        initializer { ResultViewModel(c().questions, c().sessions, createSavedStateHandle()) }
        initializer { ReviewViewModel(c().sessions, createSavedStateHandle()) }
        initializer { SettingsViewModel(c().questions, c().sessions, c().preferences) }
        initializer { QuizViewModel(c().sessions, c().preferences, createSavedStateHandle()) }
        initializer { LatihanViewModel(c().questions, c().sessions, c().preferences) }
        initializer { MateriViewModel(c().materials, c().sessions, c().preferences) }
        initializer { MateriDetailViewModel(c().materials, c().questions, c().sessions, c().preferences, createSavedStateHandle()) }
        initializer { ProgresViewModel(c().questions, c().sessions, c().preferences) }
    }
}

private fun CreationExtras.c() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as SiapSkdApp).container
