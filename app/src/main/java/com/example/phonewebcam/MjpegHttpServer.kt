package com.example.phonewebcam

import fi.iki.elonen.NanoHTTPD
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.concurrent.thread

class MjpegHttpServer(
    port: Int,
    private val streamManager: CameraStreamManager
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        return when (session.uri) {
            "/" -> {
                newFixedLengthResponse("""
                    <html>
                    <body>
                        <h2>Phone Webcam</h2>
                        <img src="/mjpeg" />
                    </body>
                    </html>
                """.trimIndent())
            }

            "/mjpeg" -> {
                val input = PipedInputStream()
                val output = PipedOutputStream(input)

                thread(start = true) {
                    try {
                        while (!Thread.currentThread().isInterrupted) {
                            val jpeg = streamManager.getLatestJpeg()
                            if (jpeg != null) {
                                val header = buildString {
                                    append("--frame\r\n")
                                    append("Content-Type: image/jpeg\r\n")
                                    append("Content-Length: ${jpeg.size}\r\n")
                                    append("\r\n")
                                }.toByteArray()

                                output.write(header)
                                output.write(jpeg)
                                output.write("\r\n".toByteArray())
                                output.flush()
                            }
                            Thread.sleep(50) // 大约 20fps
                        }
                    } catch (_: Exception) {
                    } finally {
                        try { output.close() } catch (_: Exception) {}
                    }
                }

                newChunkedResponse(
                    Response.Status.OK,
                    "multipart/x-mixed-replace; boundary=frame",
                    input
                )
            }

            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not found")
        }
    }
}