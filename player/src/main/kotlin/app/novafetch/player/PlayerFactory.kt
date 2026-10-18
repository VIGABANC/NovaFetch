package app.novafetch.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class PlayerFactory(private val context: Context) {
    fun create(uri: String): ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(Uri.parse(uri)))
        prepare()
        playWhenReady = true
    }
}
