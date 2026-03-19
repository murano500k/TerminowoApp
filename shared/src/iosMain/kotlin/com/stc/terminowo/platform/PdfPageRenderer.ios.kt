@file:OptIn(ExperimentalForeignApi::class)

package com.stc.terminowo.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGPDFDocumentCreateWithProvider
import platform.CoreGraphics.CGPDFDocumentGetNumberOfPages
import platform.CoreGraphics.CGPDFDocumentGetPage
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImageJPEGRepresentation
import platform.CoreGraphics.CGContextDrawPDFPage
import platform.CoreGraphics.CGContextScaleCTM
import platform.CoreGraphics.CGContextTranslateCTM
import platform.CoreGraphics.CGPDFPageGetBoxRect
import platform.CoreGraphics.kCGPDFMediaBox
import platform.CoreFoundation.CFDataCreate
import platform.CoreGraphics.CGDataProviderCreateWithCFData
import platform.UIKit.UIGraphicsGetCurrentContext
import kotlinx.cinterop.useContents
import platform.posix.memcpy

actual suspend fun renderPdfPage(pdfBytes: ByteArray, pageIndex: Int): ByteArray? =
    withContext(Dispatchers.Default) {
        try {
            val cfData = pdfBytes.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0).reinterpret(), pdfBytes.size.toLong())
            }
            val dataProvider = CGDataProviderCreateWithCFData(cfData)

            if (dataProvider == null) return@withContext null

            val document = CGPDFDocumentCreateWithProvider(dataProvider)
                ?: return@withContext null

            val pageCount = CGPDFDocumentGetNumberOfPages(document).toInt()
            if (pageIndex < 0 || pageIndex >= pageCount) return@withContext null

            // CGPDFDocument pages are 1-indexed
            val page = CGPDFDocumentGetPage(document, (pageIndex + 1).toULong())
                ?: return@withContext null

            val pageRect = CGPDFPageGetBoxRect(page, kCGPDFMediaBox)
            val pageWidth = pageRect.useContents { size.width }
            val pageHeight = pageRect.useContents { size.height }

            val scale = 2.0
            val scaledWidth = pageWidth * scale
            val scaledHeight = pageHeight * scale

            UIGraphicsBeginImageContextWithOptions(
                CGSizeMake(scaledWidth, scaledHeight), true, 1.0
            )
            val context = UIGraphicsGetCurrentContext()
            if (context == null) {
                UIGraphicsEndImageContext()
                return@withContext null
            }

            // Fill white background
            platform.CoreGraphics.CGContextSetRGBFillColor(context, 1.0, 1.0, 1.0, 1.0)
            platform.CoreGraphics.CGContextFillRect(context, CGRectMake(0.0, 0.0, scaledWidth, scaledHeight))

            // PDF coordinate system is bottom-up, UIKit is top-down — flip
            CGContextTranslateCTM(context, 0.0, scaledHeight)
            CGContextScaleCTM(context, scale, -scale)

            CGContextDrawPDFPage(context, page)

            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()

            if (image == null) return@withContext null

            val jpegData = UIImageJPEGRepresentation(image, 0.85)
                ?: return@withContext null

            val size = jpegData.length.toInt()
            val bytes = ByteArray(size)
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), jpegData.bytes, jpegData.length)
            }
            bytes
        } catch (e: Exception) {
            null
        }
    }

actual suspend fun getPdfPageCount(pdfBytes: ByteArray): Int =
    withContext(Dispatchers.Default) {
        try {
            val cfData = pdfBytes.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0).reinterpret(), pdfBytes.size.toLong())
            }
            val dataProvider = CGDataProviderCreateWithCFData(cfData)

            if (dataProvider == null) return@withContext 0

            val document = CGPDFDocumentCreateWithProvider(dataProvider)
                ?: return@withContext 0

            CGPDFDocumentGetNumberOfPages(document).toInt()
        } catch (e: Exception) {
            0
        }
    }
