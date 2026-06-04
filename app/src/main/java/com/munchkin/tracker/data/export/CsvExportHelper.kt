package com.munchkin.tracker.data.export

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Writes CSV content to the app's cache directory and returns a content:// Uri
 * that can be shared via Android's share sheet.
 */
object CsvExportHelper {

    fun saveCsvAndShare(context: Context, csv: String, fileName: String): Intent {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "${fileName}_$timestamp.csv")
        file.writeText(csv, Charsets.UTF_8)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Манчкин – экспорт данных")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
