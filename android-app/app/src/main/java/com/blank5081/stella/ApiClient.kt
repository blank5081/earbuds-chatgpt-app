package com.blank5081.stella

import okhttp3.*
import java.io.File
import org.json.JSONObject

object ApiClient {
    private val client = OkHttpClient.Builder().build()

    /**
     * Uploads an audio file to the server. The server should return:
     * - binary MP3 bytes as the body (200) OR
     * - a JSON object like: { "audio_url":"https://...", "text":"..." }.
     *
     * This function returns a JSONObject if server returned JSON,
     * otherwise it returns a JSON object with { "audio_bytes": "<base64 or empty>" }.
     */
    fun uploadAudio(serverUrl: String, filePath: String): JSONObject {
        val file = File(filePath)
        val mediaType = "audio/3gp".toMediaTypeOrNull()
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mediaType))
            .build()

        val req = Request.Builder()
            .url(serverUrl)
            .post(body)
            .build()

        client.newCall(req).execute().use { res ->
            val contentType = res.body?.contentType()
            val bodyBytes = res.body?.bytes()
            if (contentType != null && contentType.type == "application" && contentType.subtype == "json") {
                val s = bodyBytes?.toString(Charsets.UTF_8) ?: "{}"
                return JSONObject(s)
            } else if (contentType != null && contentType.type == "audio") {
                // Server returned raw audio bytes (MP3). Save to temp file and return JSON with path.
                val tmp = File.createTempFile("stella_resp", ".mp3")
                tmp.writeBytes(bodyBytes ?: ByteArray(0))
                val obj = JSONObject()
                obj.put("audio_path", tmp.absolutePath)
                return obj
            } else {
                // Try parse as JSON fallback
                val s = bodyBytes?.toString(Charsets.UTF_8) ?: "{}"
                return try {
                    JSONObject(s)
                } catch (e: Exception) {
                    JSONObject().put("raw", s)
                }
            }
        }
    }
}
