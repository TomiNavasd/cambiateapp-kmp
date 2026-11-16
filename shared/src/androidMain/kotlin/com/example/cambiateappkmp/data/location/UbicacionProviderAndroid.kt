package com.example.cambiateappkmp.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.example.cambiateappkmp.domain.error.UbicacionException
import com.example.cambiateappkmp.domain.model.Ubicacion
import com.example.cambiateappkmp.domain.repository.UbicacionProvider
import com.example.cambiateappkmp.presentation.permisos.tienePermisoUbicacion
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class UbicacionProviderAndroid(
    private val contexto: Context,
) : UbicacionProvider {

    private val cliente by lazy { LocationServices.getFusedLocationProviderClient(contexto) }

    override suspend fun obtenerUbicacionActual(): Result<Ubicacion> {
        if (!tienePermisoUbicacion(contexto)) return Result.failure(UbicacionException.SinPermiso())

        return try {
            val ubicacion = withTimeoutOrNull(TIEMPO_MAXIMO_MS) { pedirAlGps() }
            if (ubicacion == null) {
                Result.failure(UbicacionException.NoDisponible())
            } else {
                Result.success(Ubicacion(lat = ubicacion.latitude, lng = ubicacion.longitude))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(UbicacionException.SinPermiso())
        } catch (e: Exception) {
            Result.failure(UbicacionException.NoDisponible())
        }
    }

    /** El permiso ya se comprobó en obtenerUbicacionActual(). */
    @SuppressLint("MissingPermission")
    private suspend fun pedirAlGps(): Location? =
        suspendCancellableCoroutine { continuacion ->
            val fuenteDeCancelacion = CancellationTokenSource()
            continuacion.invokeOnCancellation { fuenteDeCancelacion.cancel() }

            cliente.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, fuenteDeCancelacion.token)
                .addOnSuccessListener { continuacion.resume(it) }
                .addOnFailureListener { continuacion.resume(null) }
        }

    private companion object {
        const val TIEMPO_MAXIMO_MS = 15_000L
    }
}
