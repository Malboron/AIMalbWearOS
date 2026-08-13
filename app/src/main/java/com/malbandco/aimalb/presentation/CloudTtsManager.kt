package com.malbandco.aimalb.presentation

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.malbandco.aimalb.utils.EdgeTokenGenerator
import okhttp3.*
import okio.ByteString
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Менеджер облачного синтеза речи через Microsoft Edge. 
 * v1.6.3: Упрощенная реализация без метаданных (используется расчетная синхронизация).
 */
class CloudTtsManager(
    private val context: Context,
    private val onStart: () -> Unit,
    private val onCompleted: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient()
    private var exoPlayer: ExoPlayer? = null
    private var webSocket: WebSocket? = null
    private var tempFile: File? = null
    private var outputStream: FileOutputStream? = null

    init {
        exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) onCompleted()
                if (state == Player.STATE_READY) onStart()
            }
        })
    }

    /**
     * Озвучка через Microsoft Edge.
     */
    fun speakEdge(text: String, voice: String, speed: Float = 1.0f) {
        stop()
        
        val requestId = UUID.randomUUID().toString().replace("-", "").uppercase()
        val token = EdgeTokenGenerator.generateToken()
        val muid = EdgeTokenGenerator.generateMuid()
        
        val url = "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                "?TrustedClientToken=6A5AA1D4EAFF4E9FB37E23D68491D6F4" +
                "&ConnectionId=$requestId" +
                "&Sec-MS-GEC=$token" +
                "&Sec-MS-GEC-Version=1-143.0.3650.75"
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0")
            .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
            .header("Cookie", "muid=$muid;")
            .build()

        tempFile = File(context.cacheDir, "edge_v163.mp3")
        outputStream = FileOutputStream(tempFile)

        val ratePercent = ((speed - 1.0f) * 100).toInt()
        val rateString = if (ratePercent >= 0) "+$ratePercent%" else "$ratePercent%"

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Конфигурация: метаданные отключены для стабильности
                webSocket.send("Content-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n" +
                        "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"}," +
                        "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}")
                
                val ssml = "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='ru-RU'>" +
                        "<voice name='$voice'><prosody rate='$rateString' pitch='0%'>$text</prosody></voice></speak>"
                webSocket.send("X-RequestId:$requestId\r\nContent-Type:application/ssml+xml\r\nPath:ssml\r\n\r\n$ssml")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                val data = bytes.toByteArray()
                if (data.size < 2) return
                val headLen = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
                val headers = String(data, 2, headLen, Charsets.UTF_8)
                
                if (headers.contains("Path:audio")) {
                    val audioOffset = 2 + headLen
                    if (data.size > audioOffset) {
                        outputStream?.write(data, audioOffset, data.size - audioOffset)
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (text.contains("turn.end")) {
                    webSocket.close(1000, "Done")
                    playResult()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError("Connection Failed")
            }
        })
    }

    /**
     * Повторное воспроизведение последнего успешного аудио из локального кэша.
     */
    fun restart() {
        stop()
        playResult()
    }

    private fun playResult() {
        tempFile?.let { file ->
            if (file.exists() && file.length() > 0) {
                context.mainExecutor.execute {
                    try {
                        outputStream?.flush()
                        outputStream?.close()
                    } catch (e: Exception) {}
                    outputStream = null
                    
                    exoPlayer?.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
                    exoPlayer?.prepare()
                    exoPlayer?.play()
                }
            }
        }
    }

    fun stop() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        webSocket?.close(1000, "Stop")
        webSocket = null
        try { outputStream?.flush(); outputStream?.close() } catch (e: Exception) {}
        outputStream = null
    }

    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
    }
}
