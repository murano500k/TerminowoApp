package com.stc.terminowo.platform

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

actual suspend fun renderPdfPage(pdfBytes: ByteArray, pageIndex: Int): ByteArray? =
    withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("pdf_render", ".pdf")
            tempFile.writeBytes(pdfBytes)

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)

            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                renderer.close()
                fd.close()
                return@withContext null
            }

            val page = renderer.openPage(pageIndex)
            val scale = 2 // render at 2x for decent quality
            val bitmap = Bitmap.createBitmap(
                page.width * scale,
                page.height * scale,
                Bitmap.Config.ARGB_8888
            )
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            renderer.close()
            fd.close()

            val output = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
            bitmap.recycle()
            output.toByteArray()
        } catch (e: Exception) {
            null
        } finally {
            tempFile?.delete()
        }
    }

actual suspend fun getPdfPageCount(pdfBytes: ByteArray): Int =
    withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            tempFile = File.createTempFile("pdf_count", ".pdf")
            tempFile.writeBytes(pdfBytes)

            val fd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            val count = renderer.pageCount
            renderer.close()
            fd.close()
            count
        } catch (e: Exception) {
            0
        } finally {
            tempFile?.delete()
        }
    }
