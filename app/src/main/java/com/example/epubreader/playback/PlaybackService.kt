package com.example.epubreader.playback

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.epubreader.MainActivity
import com.example.epubreader.R

/**
 * Keeps narration alive when the app leaves the foreground, and gives it a media
 * notification with play/pause, chapter skips and stop.
 */
class PlaybackService : Service() {

    private lateinit var session: MediaSessionCompat

    override fun onCreate() {
        super.onCreate()
        createChannel()
        session = MediaSessionCompat(this, "OpenBook").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = PlaybackBridge.onToggle?.invoke() ?: Unit
                override fun onPause() = PlaybackBridge.onToggle?.invoke() ?: Unit
                override fun onStop() = PlaybackBridge.onStop?.invoke() ?: Unit
                override fun onSkipToNext() = PlaybackBridge.onNext?.invoke() ?: Unit
                override fun onSkipToPrevious() = PlaybackBridge.onPrevious?.invoke() ?: Unit
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> PlaybackBridge.onToggle?.invoke()
            ACTION_STOP -> PlaybackBridge.onStop?.invoke()
            ACTION_NEXT -> PlaybackBridge.onNext?.invoke()
            ACTION_PREVIOUS -> PlaybackBridge.onPrevious?.invoke()
            ACTION_OPEN -> {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                )
            }
            else -> Unit
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        session.isActive = false
        session.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val state = PlaybackBridge.state
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, state.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, state.chapter)
                .build(),
        )
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS,
                )
                .setState(
                    if (state.playing) PlaybackStateCompat.STATE_PLAYING
                    else PlaybackStateCompat.STATE_PAUSED,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1f,
                )
                .build(),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(state.title.ifBlank { getString(R.string.app_name) })
            .setContentText(state.chapter)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, getString(R.string.player_previous_chapter), action(ACTION_PREVIOUS))
            .addAction(
                if (state.playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if (state.playing) R.string.pause else R.string.play),
                action(ACTION_TOGGLE),
            )
            .addAction(android.R.drawable.ic_media_next, getString(R.string.player_next_chapter), action(ACTION_NEXT))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.stop_reading), action(ACTION_STOP))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        this,
        name.hashCode(),
        Intent(this, PlaybackService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Narration", NotificationManager.IMPORTANCE_LOW),
        )
    }

    companion object {
        private const val CHANNEL = "narration"
        private const val NOTIFICATION_ID = 4101
        const val ACTION_TOGGLE = "com.example.epubreader.TOGGLE"
        const val ACTION_STOP = "com.example.epubreader.STOP"
        const val ACTION_NEXT = "com.example.epubreader.NEXT"
        const val ACTION_PREVIOUS = "com.example.epubreader.PREVIOUS"
        const val ACTION_OPEN = "com.example.epubreader.OPEN"

        /** Called by the reader whenever the voice starts, pauses or stops. */
        fun update(context: Context, playing: Boolean, title: String, chapter: String) {
            PlaybackBridge.state = PlaybackBridge.State(playing, title, chapter)
            ContextCompat.startForegroundService(
                context,
                Intent(context, PlaybackService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}

/** The one place the notification's buttons reach the reader. */
object PlaybackBridge {
    data class State(val playing: Boolean, val title: String, val chapter: String)

    @Volatile
    var state: State = State(false, "", "")

    @Volatile var onToggle: (() -> Unit)? = null
    @Volatile var onStop: (() -> Unit)? = null
    @Volatile var onNext: (() -> Unit)? = null
    @Volatile var onPrevious: (() -> Unit)? = null
}
