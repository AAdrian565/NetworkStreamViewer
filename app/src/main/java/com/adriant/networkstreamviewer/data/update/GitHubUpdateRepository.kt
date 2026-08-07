package com.adriant.networkstreamviewer.data.update

import android.content.Context
import com.adriant.networkstreamviewer.domain.model.AppUpdate
import com.adriant.networkstreamviewer.domain.repository.UpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class GitHubUpdateRepository(context: Context) : UpdateRepository {
    private val updateDirectory = File(context.cacheDir, "updates")

    override suspend fun findLatestUpdate(currentVersion: String): AppUpdate? =
        withContext(Dispatchers.IO) {
            val connection = openConnection(LATEST_RELEASE_URL)
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub returned HTTP ${connection.responseCode}")
                }

                val release = connection.inputStream.bufferedReader().use { reader ->
                    JSONObject(reader.readText())
                }
                val latestVersion = release.getString("tag_name").removePrefix("v")
                if (compareVersions(latestVersion, currentVersion) <= 0) return@withContext null

                val apkUrl = release.getJSONArray("assets")
                    .let { assets ->
                        (0 until assets.length())
                            .asSequence()
                            .map { assets.getJSONObject(it) }
                            .firstOrNull {
                                it.optString("name").endsWith(".apk", ignoreCase = true)
                            }
                    }
                    ?.getString("browser_download_url")
                    ?: throw IOException("The GitHub release does not contain an APK")

                AppUpdate(
                    version = latestVersion,
                    downloadUrl = apkUrl,
                    releaseUrl = release.optString("html_url")
                )
            } finally {
                connection.disconnect()
            }
        }

    override suspend fun downloadUpdate(update: AppUpdate): String = withContext(Dispatchers.IO) {
        updateDirectory.mkdirs()
        val temporaryFile = File(updateDirectory, "update.apk.download")
        val apkFile = File(updateDirectory, "NetworkStreamViewer-v${update.version}.apk")
        temporaryFile.delete()

        val connection = openConnection(update.downloadUrl)
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("GitHub returned HTTP ${connection.responseCode}")
            }
            val contentLength = connection.contentLengthLong
            if (contentLength > MAX_APK_SIZE_BYTES) {
                throw IOException("The downloaded APK is too large")
            }

            connection.inputStream.use { input ->
                temporaryFile.outputStream().use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }
            if (!temporaryFile.renameTo(apkFile)) {
                throw IOException("Could not prepare the downloaded APK")
            }
            apkFile.absolutePath
        } finally {
            connection.disconnect()
            temporaryFile.delete()
        }
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECTION_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "NetworkStreamViewer")
        }

    private companion object {
        const val LATEST_RELEASE_URL =
            "https://api.github.com/repos/AAdrian565/NetworkStreamViewer/releases/latest"
        const val CONNECTION_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 60_000
        const val BUFFER_SIZE = 16 * 1024
        const val MAX_APK_SIZE_BYTES = 300L * 1024L * 1024L
    }
}

internal fun compareVersions(first: String, second: String): Int {
    val firstParts = first.substringBefore('-').removePrefix("v").split('.')
    val secondParts = second.substringBefore('-').removePrefix("v").split('.')
    val partCount = maxOf(firstParts.size, secondParts.size)

    for (index in 0 until partCount) {
        val firstPart = firstParts.getOrNull(index)?.toIntOrNull() ?: 0
        val secondPart = secondParts.getOrNull(index)?.toIntOrNull() ?: 0
        if (firstPart != secondPart) return firstPart.compareTo(secondPart)
    }
    return 0
}
