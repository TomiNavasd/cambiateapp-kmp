package com.example.cambiateappkmp.di

import com.example.cambiateappkmp.presentation.auth.LoginViewModel
import com.example.cambiateappkmp.presentation.auth.RegisterViewModel
import com.example.cambiateappkmp.presentation.navigation.NavegacionViewModel
import com.example.cambiateappkmp.presentation.session.SessionViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val presentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::SessionViewModel)
    viewModelOf(::NavegacionViewModel)
}