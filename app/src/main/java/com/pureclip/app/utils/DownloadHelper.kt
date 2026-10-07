package com.pureclip.app.utils

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.pureclip.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Handles video and audio file downloads using Android's DownloadManager.
 * Saves files to public storage (Movies/PureClip or Music/PureClip).
 * Displays a real-time progress dialog tracking DownloadManager progress.
 */
object DownloadHelper {

    fun downloadVideo(
        context: Context,
        videoUrl: String,
        title: String,
        platform: String,
        format: String = "mp4",
        onComplete: (() -> Unit)? = null
    ) {
        try {
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Toast.makeText(context, context.getString(R.string.toast_no_internet), Toast.LENGTH_SHORT).show()
                return
            }

            if (videoUrl.isBlank()) {
                Toast.makeText(context, "Đường dẫn tải xuống không hợp lệ", Toast.LENGTH_SHORT).show()
                return
            }

            // Hỗ trợ tiếng Việt và các ký tự Unicode, chỉ loại bỏ các ký tự cấm trong tên file hệ thống
            val cleanTitle = title
                .replace("[\\\\/:*?\"<>|]".toRegex(), "")
                .trim()
                .take(60)
                .ifEmpty { "media" }

            val fileExt = if (format.lowercase() == "mp3") "mp3" else "mp4"
            val directoryType = if (fileExt == "mp3") Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
            val subDir = "PureClip"
            val fileName = "PureClip_${platform}_${cleanTitle}_${System.currentTimeMillis()}.$fileExt"

            val request = DownloadManager.Request(Uri.parse(videoUrl)).apply {
                setTitle("PureClip - Tải $platform ${fileExt.uppercase()}")
                setDescription("Đang tải: $title")
                setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
                setDestinationInExternalPublicDir(
                    directoryType,
                    "$subDir/$fileName"
                )
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = downloadManager.enqueue(request)

            showDownloadProgressDialog(context, downloadManager, downloadId, title, onComplete)

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Tải không thành công: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun showDownloadProgressDialog(
        context: Context,
        downloadManager: DownloadManager,
        downloadId: Long,
        title: String,
        onComplete: (() -> Unit)?
    ) {
        val hostActivity = context as? Activity
        if (hostActivity != null && (hostActivity.isFinishing || hostActivity.isDestroyed)) {
            return
        }

        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null)
        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDialogDetails = dialogView.findViewById<TextView>(R.id.tvDialogDetails)
        val progressBar = dialogView.findViewById<LinearProgressIndicator>(R.id.progressBar)
        val tvDownloadSize = dialogView.findViewById<TextView>(R.id.tvDownloadSize)
        val tvProgressPercent = dialogView.findViewById<TextView>(R.id.tvProgressPercent)
        val btnCancelDownload = dialogView.findViewById<MaterialButton>(R.id.btnCancelDownload)

        tvDialogDetails.text = title

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        if (hostActivity != null && (hostActivity.isFinishing || hostActivity.isDestroyed)) {
            return
        }
        dialog.show()

        val scope = CoroutineScope(Dispatchers.IO)
        var userCancelled = false

        val trackingJob: Job = scope.launch {
            var downloading = true
            while (downloading && !userCancelled) {
                if (hostActivity != null && hostActivity.isDestroyed) {
                    downloading = false
                    break
                }
                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)
                            val downloadedBytes = if (downloadedIndex != -1) cursor.getLong(downloadedIndex) else 0L
                            val totalBytes = if (totalIndex != -1) cursor.getLong(totalIndex) else -1L

                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    downloading = false
                                    withContext(Dispatchers.Main) {
                                        progressBar.isIndeterminate = false
                                        progressBar.progress = 100
                                        tvProgressPercent.text = "100%"
                                        tvDialogTitle.text = "Tải xuống hoàn tất!"
                                        val sizeText = if (totalBytes > 0) {
                                            "${formatBytes(totalBytes)} / ${formatBytes(totalBytes)}"
                                        } else if (downloadedBytes > 0) {
                                            "${formatBytes(downloadedBytes)} đã tải"
                                        } else {
                                            "Hoàn tất"
                                        }
                                        tvDownloadSize.text = sizeText

                                        delay(600) // Đợi hiển thị trọn vẹn 100% trước khi đóng
                                        safelyDismissDialog(dialog, hostActivity)
                                        Toast.makeText(context, "Tải xuống thành công! File đã lưu vào máy.", Toast.LENGTH_SHORT).show()
                                        onComplete?.invoke()
                                    }
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    downloading = false
                                    withContext(Dispatchers.Main) {
                                        tvDialogTitle.text = "Tải xuống thất bại"
                                        progressBar.isIndeterminate = false
                                        progressBar.progress = 0
                                        tvProgressPercent.text = "Lỗi"
                                        delay(1000)
                                        safelyDismissDialog(dialog, hostActivity)
                                        Toast.makeText(context, "Tải xuống thất bại. Vui lòng thử lại!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                DownloadManager.STATUS_PENDING -> {
                                    withContext(Dispatchers.Main) {
                                        tvDialogTitle.text = "Đang kết nối..."
                                        tvDownloadSize.text = "Đang chuẩn bị tải..."
                                        progressBar.isIndeterminate = true
                                        tvProgressPercent.text = "0%"
                                    }
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    withContext(Dispatchers.Main) {
                                        tvDialogTitle.text = "Tạm dừng tải..."
                                        tvDownloadSize.text = "Đang chờ kết nối..."
                                    }
                                }
                                DownloadManager.STATUS_RUNNING -> {
                                    withContext(Dispatchers.Main) {
                                        tvDialogTitle.text = "Đang tải xuống..."
                                        if (totalBytes > 0) {
                                            val progress = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                                            progressBar.isIndeterminate = false
                                            progressBar.progress = progress
                                            tvProgressPercent.text = "$progress%"
                                            tvDownloadSize.text = "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}"
                                        } else {
                                            progressBar.isIndeterminate = true
                                            if (downloadedBytes > 0) {
                                                tvDownloadSize.text = "${formatBytes(downloadedBytes)} đã tải"
                                                tvProgressPercent.text = "Đang tải..."
                                            } else {
                                                tvDownloadSize.text = "Đang nhận dữ liệu..."
                                                tvProgressPercent.text = "0%"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                delay(300)
            }
        }

        btnCancelDownload.setOnClickListener {
            userCancelled = true
            trackingJob.cancel()
            downloadManager.remove(downloadId) // Hủy thực tế tác vụ tải trong hệ thống Android
            safelyDismissDialog(dialog, hostActivity)
            Toast.makeText(context, "Đã hủy tải xuống", Toast.LENGTH_SHORT).show()
        }

        dialog.setOnDismissListener {
            userCancelled = true
            trackingJob.cancel()
        }
    }

    private fun safelyDismissDialog(dialog: androidx.appcompat.app.AlertDialog, hostActivity: Activity?) {
        if (dialog.isShowing && hostActivity?.isFinishing != true && hostActivity?.isDestroyed != true) {
            try {
                dialog.dismiss()
            } catch (_: Exception) {
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return if (mb >= 1.0) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.0f KB", kb)
        }
    }
}
