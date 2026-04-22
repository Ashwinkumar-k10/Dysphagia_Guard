package com.dysphagiaguard.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.dysphagiaguard.data.model.SwallowEvent
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val timeSdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    fun generateReport(
        context: Context,
        patientName: String,
        totalSwallows: Int,
        unsafeCount: Int,
        events: List<SwallowEvent> = emptyList()
    ): Uri? {
        return try {
            val fileDir = File(context.filesDir, "reports")
            if (!fileDir.exists()) fileDir.mkdirs()

            val fileName = "DysphagiaGuard_Report_${System.currentTimeMillis()}.pdf"
            val file = File(fileDir, fileName)

            val document = Document(PageSize.A4, 36f, 36f, 54f, 36f)
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()

            // ── Title ──────────────────────────────────────────────────────
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20f, BaseColor.BLACK)
            val title = Paragraph("DysphagiaGuard Session Report\n", titleFont)
            title.alignment = Element.ALIGN_CENTER
            title.spacingAfter = 4f
            document.add(title)

            val subFont = FontFactory.getFont(FontFactory.HELVETICA, 10f, BaseColor.GRAY)
            val subTitle = Paragraph("Generated: ${sdf.format(Date())}\n\n", subFont)
            subTitle.alignment = Element.ALIGN_CENTER
            document.add(subTitle)

            // ── Patient Info ───────────────────────────────────────────────
            val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11f)
            val normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)

            document.add(Paragraph("Patient Information", boldFont).apply { spacingAfter = 4f })
            document.add(Paragraph("Name: $patientName", normalFont))
            document.add(Paragraph("Date: ${SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())}\n", normalFont))

            // ── Summary ────────────────────────────────────────────────────
            document.add(Paragraph("\nSession Summary", boldFont).apply { spacingAfter = 4f })

            val safeCount = events.count { it.classification == "SAFE" }
            val unsafeRate = if (totalSwallows > 0) (unsafeCount * 100) / totalSwallows else 0

            val summaryTable = PdfPTable(4).apply {
                widthPercentage = 100f
                setWidths(floatArrayOf(1f, 1f, 1f, 1f))
                spacingAfter = 12f
            }

            fun headerCell(text: String): PdfPCell = PdfPCell(
                Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f))
            ).apply {
                backgroundColor = BaseColor(30, 30, 30)
                setPadding(6f)
                horizontalAlignment = Element.ALIGN_CENTER
            }
            fun valueCell(text: String, color: BaseColor = BaseColor.BLACK): PdfPCell = PdfPCell(
                Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f, color))
            ).apply {
                setPadding(8f)
                horizontalAlignment = Element.ALIGN_CENTER
            }

            summaryTable.addCell(headerCell("Total Swallows"))
            summaryTable.addCell(headerCell("Safe"))
            summaryTable.addCell(headerCell("Unsafe"))
            summaryTable.addCell(headerCell("Unsafe Rate"))
            summaryTable.addCell(valueCell("$totalSwallows"))
            summaryTable.addCell(valueCell("$safeCount", BaseColor(0, 168, 120)))
            summaryTable.addCell(valueCell("$unsafeCount", BaseColor(220, 50, 50)))
            summaryTable.addCell(valueCell("$unsafeRate%",
                if (unsafeRate > 15) BaseColor(220, 50, 50)
                else if (unsafeRate >= 5) BaseColor(255, 165, 0)
                else BaseColor(0, 168, 120)
            ))

            document.add(summaryTable)

            // ── Risk Assessment ────────────────────────────────────────────
            val riskLabel = when {
                unsafeRate > 15 -> "HIGH RISK — Immediate attention recommended."
                unsafeRate >= 5 -> "MODERATE RISK — Monitor closely."
                else -> "LOW RISK — Continue standard monitoring."
            }
            document.add(Paragraph("Risk Assessment: $riskLabel\n", boldFont).apply { spacingAfter = 8f })

            // ── Event Timeline ─────────────────────────────────────────────
            if (events.isNotEmpty()) {
                document.add(Paragraph("\nSwallow Event Timeline", boldFont).apply { spacingAfter = 6f })

                val table = PdfPTable(5).apply {
                    widthPercentage = 100f
                    setWidths(floatArrayOf(2f, 1.2f, 1f, 1f, 1f))
                    spacingAfter = 12f
                }

                listOf("Time", "Type", "Confidence", "IMU RMS", "Mic Level").forEach { h ->
                    table.addCell(PdfPCell(
                        Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9f))
                    ).apply {
                        backgroundColor = BaseColor(220, 220, 220)
                        setPadding(5f)
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
                }

                events.forEach { event ->
                    val typeColor = if (event.classification == "UNSAFE")
                        BaseColor(220, 50, 50) else BaseColor(0, 168, 120)

                    table.addCell(PdfPCell(
                        Phrase(timeSdf.format(Date(event.timestamp)), normalFont)
                    ).apply { setPadding(4f) })

                    table.addCell(PdfPCell(
                        Phrase(event.classification, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f, typeColor))
                    ).apply {
                        setPadding(4f)
                        horizontalAlignment = Element.ALIGN_CENTER
                    })

                    table.addCell(PdfPCell(
                        Phrase("${(event.confidence * 100).toInt()}%", normalFont)
                    ).apply {
                        setPadding(4f)
                        horizontalAlignment = Element.ALIGN_CENTER
                    })

                    table.addCell(PdfPCell(
                        Phrase(String.format("%.2f", event.imuRms), normalFont)
                    ).apply {
                        setPadding(4f)
                        horizontalAlignment = Element.ALIGN_CENTER
                    })

                    table.addCell(PdfPCell(
                        Phrase(String.format("%.2f", event.micEnvelope), normalFont)
                    ).apply {
                        setPadding(4f)
                        horizontalAlignment = Element.ALIGN_CENTER
                    })
                }

                document.add(table)
            }

            // ── Footer ─────────────────────────────────────────────────────
            val footer = Paragraph(
                "\n\nGenerated by DysphagiaGuard — Offline Clinical Report\n" +
                "For clinical review only. Not a substitute for professional medical advice.",
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, BaseColor.GRAY)
            )
            footer.alignment = Element.ALIGN_CENTER
            document.add(footer)

            document.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
