package com.example.cambiateappkmp.data.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cambiateappkmp.domain.notification.Notificador
import com.example.cambiateappkmp.presentation.permisos.tienePermisoNotificaciones
import java.util.Calendar
import java.util.concurrent.TimeUnit

class NotificadorAndroid(
    private val contexto: Context,
) : Notificador {

    override fun programarDiaria(hora: Int, minuto: Int) {
        // Sin restricción de red: el aviso sale a la hora aunque no haya internet
        // (si no se puede consultar el clima, el Worker manda un aviso general).
        val pedido = PeriodicWorkRequestBuilder<RecordatorioClimaWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(milisHastaProximaHora(hora, minuto), TimeUnit.MILLISECONDS)
            .build()

        // CANCEL_AND_REENQUEUE reinicia el calendario: con UPDATE, cambiar la hora no movía el aviso.
        WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(
            NOMBRE_TRABAJO,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            pedido,
        )
    }

    override fun cancelarDiaria() {
        WorkManager.getInstance(contexto).cancelUniqueWork(NOMBRE_TRABAJO)
    }

    @SuppressLint("MissingPermission") // el permiso se verifica justo antes de notificar
    override fun mostrarAhora(titulo: String, mensaje: String) {
        if (!tienePermisoNotificaciones(contexto)) return

        crearCanal()

        val notificacion = NotificationCompat.Builder(contexto, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titulo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intentAbrirApp())
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion)
    }

    /** Al tocar la notificación se abre la app (la pantalla principal). */
    private fun intentAbrirApp(): PendingIntent? {
        val intent = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        } ?: return null

        return PendingIntent.getActivity(
            contexto,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_ID,
            "Consejos del clima",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Qué ponerte según el clima de mañana"
        }
        contexto.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /** Milisegundos hasta la próxima vez que el reloj del dispositivo marque hora:minuto:00. */
    private fun milisHastaProximaHora(hora: Int, minuto: Int): Long {
        val ahora = Calendar.getInstance()
        val proximo = (ahora.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hora)
            set(Calendar.MINUTE, minuto)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!proximo.after(ahora)) proximo.add(Calendar.DAY_OF_YEAR, 1)
        return proximo.timeInMillis - ahora.timeInMillis
    }

    private companion object {
        const val NOMBRE_TRABAJO = "recordatorio_diario_clima"
        const val CANAL_ID = "consejos_clima"
        const val ID_NOTIFICACION = 1001
    }
}
