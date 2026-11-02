package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.presentation.auth.LoginViewModel
import com.example.cambiateappkmp.presentation.auth.RegisterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}