package com.example.onthejob.data.aiformat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val WORKER_URL = "https://onthejob-ai-proxy.elmntr.workers.dev"

sealed class AiFormatResult {
    data class Success(val formattedText: String) : AiFormatResult()
    /** reason is one of: "quota_exhausted" | "rate_limited" | "unauthorized" | "failed_other" */
    data class Failure(val reason: String) : AiFormatResult()
}

class AiFormatClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS) // hard ceiling on the whole call, belt-and-suspenders
        .build(),
) {
    /**
     * Calls the Cloudflare Worker to turn a raw description into a polished
     * OJT entry. Requires a valid Firebase ID token — the Worker rejects
     * unauthenticated requests with reason "unauthorized".
     *
     * Never throws: any network failure, timeout, or unexpected shape is
     * mapped to Failure("failed_other") so the caller can always fall back
     * to saving the raw text.
     */
    suspend fun format(rawText: String, idToken: String): AiFormatResult =
        withContext(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().put("rawText", rawText).toString()
                val requestBody = jsonBody.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(WORKER_URL)
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()
                        ?: return@withContext AiFormatResult.Failure("failed_other")

                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) {
                        AiFormatResult.Success(json.getString("formattedText"))
                    } else {
                        AiFormatResult.Failure(json.optString("reason", "failed_other"))
                    }
                }
            } catch (e: IOException) {
                // Offline, timeout, DNS failure, etc. — never lose the entry.
                AiFormatResult.Failure("failed_other")
            } catch (e: Exception) {
                AiFormatResult.Failure("failed_other")
            }
        }
}