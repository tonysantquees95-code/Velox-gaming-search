package tech.vetorial.velox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/** Mantém o Velox vivo (com aviso fixo na barra) para o som do YouTube continuar com a tela apagada ou em outro app. */
class SegundoPlano : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("segundo_plano", "Tocando em segundo plano", NotificationManager.IMPORTANCE_LOW))
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, "segundo_plano")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Velox tocando em segundo plano")
            .setContentText("Toque para voltar ao Velox")
            .setContentIntent(abrir)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(7, n)
        return START_NOT_STICKY
    }

    companion object {
        fun ligar(c: Context) { try { c.startForegroundService(Intent(c, SegundoPlano::class.java)) } catch (e: Exception) { /* sem permissão agora */ } }
        fun desligar(c: Context) { try { c.stopService(Intent(c, SegundoPlano::class.java)) } catch (e: Exception) { /* já parado */ } }
    }
}
