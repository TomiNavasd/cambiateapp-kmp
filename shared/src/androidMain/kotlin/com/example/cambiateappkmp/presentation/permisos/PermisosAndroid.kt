package com.example.cambiateappkmp.presentation.permisos

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

internal fun tienePermisoUbicacion(contexto: Context): Boolean =
    ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
