package com.dysphagiaguard.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareUtils {

    /** Share PDF via WhatsApp. Falls back to generic share if WhatsApp is not installed. */
    fun shareViaWhatsApp(context: Context, pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, "DysphagiaGuard Session Report — please review the attached PDF.")
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // WhatsApp not installed — fall through to generic share
            shareGeneric(context, pdfUri)
        }
    }

    /**
     * Nearby Share works via Android's built-in share sheet (ACTION_SEND).
     * The user selects "Nearby Share" from the chooser manually.
     * We pass an explicit title so the chooser is clear.
     */
    fun shareNearby(context: Context, pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TITLE, "DysphagiaGuard Report — Nearby Share")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Nearby Share / Share Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Generic share — opens the system share sheet for all available apps. */
    fun shareGeneric(context: Context, pdfUri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_SUBJECT, "DysphagiaGuard Session Report")
            putExtra(Intent.EXTRA_TEXT, "Please find the DysphagiaGuard session report attached.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Share Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
