package tech.vetorial.velox

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder

/**
 * Player do Velox: só existe enquanto há áudio ou vídeo tocando de verdade. Mantém o Velox vivo e aparece como player do Android
 * (barra de notificações, tela de bloqueio e fones), com título da página e botões de tocar/pausar.
 */
class SegundoPlano : Service() {
    private var sessao: MediaSession? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        val acao = i?.action
        if (acao == ACAO_PAUSAR || acao == ACAO_TOCAR) {
            controle?.invoke(acao)
            tocando = acao == ACAO_TOCAR
        } else if (acao == ACAO_PARAR) {
            controle?.invoke(ACAO_PAUSAR)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        } else {
            i?.getStringExtra("titulo")?.let { titulo = it }
            if (i?.hasExtra("tocando") == true) tocando = i.getBooleanExtra("tocando", true)
        }
        publicar()
        return START_NOT_STICKY
    }

    private fun publicar() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel("segundo_plano", "Player do Velox", NotificationManager.IMPORTANCE_LOW))
        val s = sessao ?: MediaSession(this, "Velox").also { ms ->
            ms.setCallback(object : MediaSession.Callback() {
                override fun onPlay() { controle?.invoke(ACAO_TOCAR); tocando = true; publicar() }
                override fun onPause() { controle?.invoke(ACAO_PAUSAR); tocando = false; publicar() }
                override fun onStop() { controle?.invoke(ACAO_PAUSAR); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
            })
            ms.isActive = true
            sessao = ms
        }
        val nome = titulo.ifBlank { "Velox" }
        s.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, nome).putString(MediaMetadata.METADATA_KEY_ARTIST, "Velox").build())
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP)
                .setState(if (tocando) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                .build()
        )
        val abrir = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_IMMUTABLE)
        fun botao(icone: Int, rotulo: String, a: String) = Notification.Action.Builder(Icon.createWithResource(this, icone), rotulo, PendingIntent.getService(this, a.hashCode(), Intent(this, SegundoPlano::class.java).setAction(a), PendingIntent.FLAG_IMMUTABLE)).build()
        val n = Notification.Builder(this, "segundo_plano")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(nome)
            .setContentText(if (tocando) "Tocando no Velox" else "Pausado")
            .setContentIntent(abrir)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(tocando)
            .addAction(if (tocando) botao(android.R.drawable.ic_media_pause, "Pausar", ACAO_PAUSAR) else botao(android.R.drawable.ic_media_play, "Tocar", ACAO_TOCAR))
            .addAction(botao(android.R.drawable.ic_menu_close_clear_cancel, "Fechar", ACAO_PARAR))
            .setStyle(Notification.MediaStyle().setMediaSession(s.sessionToken).setShowActionsInCompactView(0, 1))
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else startForeground(7, n)
    }

    override fun onDestroy() {
        sessao?.release()
        sessao = null
        super.onDestroy()
    }

    companion object {
        const val ACAO_PAUSAR = "tech.vetorial.velox.PAUSAR"
        const val ACAO_TOCAR = "tech.vetorial.velox.TOCAR"
        const val ACAO_PARAR = "tech.vetorial.velox.PARAR"
        /** A tela principal escuta os botões (tocar/pausar) e age na página. */
        @Volatile var controle: ((String) -> Unit)? = null
        @Volatile var titulo: String = ""
        @Volatile var tocando: Boolean = true

        /** Liga (ou atualiza) o player com o título e o estado atuais. */
        fun ligar(c: Context, titulo: String = "", tocando: Boolean = true) {
            try { c.startForegroundService(Intent(c, SegundoPlano::class.java).putExtra("titulo", titulo).putExtra("tocando", tocando)) } catch (e: Exception) { /* sem permissão agora */ }
        }
        fun desligar(c: Context) { try { c.stopService(Intent(c, SegundoPlano::class.java)) } catch (e: Exception) { /* já parado */ } }
    }
}
