package com.example.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.export.pdf.PdfReportRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val displayName: String,
    val subtitle: String,
    val description: String
) {
    MARKDOWN(
        extension = "md",
        mimeType = "text/markdown",
        displayName = "Markdown (.md)",
        subtitle = "For AI agents / LLMs",
        description = "Self-contained report with YAML front matter, drill handbook, Wilson 95% confidence bounds, and item-level mistake log."
    ),
    PDF(
        extension = "pdf",
        mimeType = "application/pdf",
        displayName = "PDF (.pdf)",
        subtitle = "For humans",
        description = "Multi-page A4 visual report with KPI cards, 26-week calendar heatmap, accuracy & trend charts, and complete drill guide."
    )
}

object ExportFileManager {

    fun defaultFileName(format: ExportFormat, localDate: String): String {
        val safeDate = localDate.ifBlank { "report" }
        return "zen-math-stats-$safeDate.${format.extension}"
    }

    fun renderBytes(snapshot: StatsSnapshot, format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.MARKDOWN -> MarkdownReportRenderer.render(snapshot).toByteArray(Charsets.UTF_8)
            ExportFormat.PDF -> PdfReportRenderer.renderToBytes(snapshot)
        }
    }

    suspend fun writeToUri(
        context: Context,
        uri: Uri,
        snapshot: StatsSnapshot,
        format: ExportFormat
    ) = withContext(Dispatchers.IO) {
        val bytes = renderBytes(snapshot, format)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: error("Unable to open output stream for URI: $uri")
    }

    suspend fun writeToCacheFile(
        context: Context,
        snapshot: StatsSnapshot,
        format: ExportFormat
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
        // Clean up older cached exports
        exportsDir.listFiles()?.forEach { it.delete() }
        val file = File(exportsDir, defaultFileName(format, snapshot.generatedLocalDate))
        val bytes = renderBytes(snapshot, format)
        file.writeBytes(bytes)
        file
    }

    suspend fun shareExport(
        context: Context,
        snapshot: StatsSnapshot,
        format: ExportFormat
    ) {
        val file = writeToCacheFile(context, snapshot, format)
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "ZenMath Performance Report (${snapshot.generatedLocalDate})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Export ${format.displayName}").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
