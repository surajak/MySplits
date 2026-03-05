package com.SohamProject.mysplits

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object PdfExporter {
    fun generateAndShare(
        context: Context,
        group: Group,
        members: List<Member>,
        expenses: List<Expense>,
        payers: List<ExpensePayer>,
        shares: List<ExpenseShare>,
        // CHANGE THIS LINE:
        settlements: List<Settlement> // removed "SplitViewModel." prefix
    ) {
        // 1. Setup PDF Document (A4 Landscape for more width)
        val pdfDocument = PdfDocument()
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // 2. Setup Paints
        val textPaint = Paint().apply { textSize = 10f; color = Color.BLACK }
        val headerPaint = Paint().apply { textSize = 12f; color = Color.BLACK; isFakeBoldText = true }
        val smallBoldPaint = Paint().apply { textSize = 9f; color = Color.BLACK; isFakeBoldText = true }
        val linePaint = Paint().apply { strokeWidth = 1f; color = Color.DKGRAY }

        // 3. Layout Dimensions
        val margin = 20f
        var currentY = 40f

        // Title
        canvas.drawText("Group Report: ${group.name}", margin, currentY, headerPaint)
        currentY += 30f

        // --- TABLE MATH ---
        val descWidth = 120f
        val totalWidth = 60f
        // Remaining width divided by members
        val memberColWidth = (pageWidth - (margin * 2) - descWidth - totalWidth) / members.size.coerceAtLeast(1)
        val halfMemberCol = memberColWidth / 2

        // --- DRAW TABLE HEADER ---
        val headerY = currentY
        val rowHeight = 35f

        // 1. Description Header
        canvas.drawText("Description", margin + 5, headerY, headerPaint)
        canvas.drawLine(margin + descWidth, headerY - 15, margin + descWidth, headerY + 20, linePaint) // Vertical line

        // 2. Total Header
        canvas.drawText("Total", margin + descWidth + 5, headerY, headerPaint)
        canvas.drawLine(margin + descWidth + totalWidth, headerY - 15, margin + descWidth + totalWidth, headerY + 20, linePaint)

        // 3. Member Headers
        var currentX = margin + descWidth + totalWidth
        members.forEach { member ->
            // Draw Name Centered
            canvas.drawText(member.name, currentX + 5, headerY - 5, smallBoldPaint)

            // Draw Sub-headers (Paid | Share)
            canvas.drawText("Paid", currentX + 5, headerY + 15, textPaint)
            canvas.drawText("Share", currentX + halfMemberCol + 5, headerY + 15, textPaint)

            // Vertical line between Paid/Share
            canvas.drawLine(currentX + halfMemberCol, headerY + 5, currentX + halfMemberCol, headerY + 20, linePaint)

            // Vertical line between Members
            currentX += memberColWidth
            canvas.drawLine(currentX, headerY - 15, currentX, headerY + 20, linePaint)
        }

        // Header Bottom Line
        currentY += 25f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
        canvas.drawLine(margin, headerY - 15, pageWidth - margin, headerY - 15, linePaint) // Header Top Line

        // --- DRAW EXPENSE ROWS ---
        expenses.forEach { expense ->
            currentY += 25f
            val y = currentY

            // 1. Desc
            canvas.drawText(expense.description, margin + 5, y, textPaint)
            // 2. Total
            canvas.drawText(String.format("%.0f", expense.totalAmount), margin + descWidth + 5, y, textPaint)

            // 3. Member Data
            var mX = margin + descWidth + totalWidth
            members.forEach { member ->
                // Find Data
                val paid = payers.find { it.expenseId == expense.id && it.memberId == member.id }?.amountPaid ?: 0.0
                val share = shares.find { it.expenseId == expense.id && it.memberId == member.id }?.amountOwed ?: 0.0

                // Draw Paid
                if (paid > 0) canvas.drawText(String.format("%.0f", paid), mX + 5, y, smallBoldPaint)
                else canvas.drawText("-", mX + 5, y, textPaint)

                // Draw Share
                if (share > 0) canvas.drawText(String.format("%.0f", share), mX + halfMemberCol + 5, y, textPaint)
                else canvas.drawText("-", mX + halfMemberCol + 5, y, textPaint)

                // Draw vertical lines for this row
                canvas.drawLine(mX, y - 20, mX, y + 10, linePaint) // Left of member
                canvas.drawLine(mX + halfMemberCol, y - 20, mX + halfMemberCol, y + 10, linePaint) // Middle of member

                mX += memberColWidth
                canvas.drawLine(mX, y - 20, mX, y + 10, linePaint) // Right of member
            }

            // Draw vertical lines for Desc and Total
            canvas.drawLine(margin + descWidth, y - 20, margin + descWidth, y + 10, linePaint)
            canvas.drawLine(margin + descWidth + totalWidth, y - 20, margin + descWidth + totalWidth, y + 10, linePaint)

            // Row Bottom Line
            canvas.drawLine(margin, y + 10, pageWidth - margin, y + 10, linePaint)
        }

        // --- TOTALS FOOTER ---
        currentY += 30f
        canvas.drawText("TOTALS", margin + 5, currentY, headerPaint)
        var tX = margin + descWidth + totalWidth

        members.forEach { member ->
            val totalPaid = payers.filter { it.memberId == member.id }.sumOf { it.amountPaid }
            val totalShare = shares.filter { it.memberId == member.id }.sumOf { it.amountOwed }

            canvas.drawText(String.format("%.0f", totalPaid), tX + 5, currentY, headerPaint)
            canvas.drawText(String.format("%.0f", totalShare), tX + halfMemberCol + 5, currentY, headerPaint)
            tX += memberColWidth
        }

        // --- SETTLEMENTS SECTION ---
        currentY += 50f
        canvas.drawText("Settlement Plan (Who pays whom):", margin, currentY, headerPaint)
        currentY += 20f

        if (settlements.isEmpty()) {
            canvas.drawText("All settled up!", margin, currentY, textPaint)
        } else {
            settlements.forEach {
                canvas.drawText("- ${it.fromName} pays ${it.toName}: $${String.format("%.2f", it.amount)}", margin, currentY, textPaint)
                currentY += 15f
            }
        }

        pdfDocument.finishPage(page)

        // 4. Save & Share
        val file = File(context.cacheDir, "${group.name}_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            shareFile(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Report"))
    }
}
