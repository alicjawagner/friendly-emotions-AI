package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

private const val JPEG_QUALITY = 90

/**
 * Crops a picked/captured image to a centered square and persists it as JPEG, so the underlying
 * file itself — not just its on-screen `ContentScale.Crop` rendering — is square (Phase 11: "when
 * saving and displaying the image clip it to be square"). Every later consumer of the same file
 * (the folder browse grid, a future wizard tab) then sees consistent square framing without
 * needing to crop on display.
 */
object SquareImageCropper {
    /** Reads [sourceUri], corrects orientation via EXIF, center-crops to a square, writes JPEG to [outputFile]. */
    fun cropToSquareJpeg(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
    ): File {
        val upright = decodeUprightBitmap(context, sourceUri)
        val square = centerCropToSquare(upright)
        FileOutputStream(outputFile).use { output ->
            square.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        }
        if (square !== upright) upright.recycle()
        square.recycle()
        return outputFile
    }

    private fun decodeUprightBitmap(
        context: Context,
        uri: Uri,
    ): Bitmap {
        val original =
            context.contentResolver.openInputStream(uri)?.use { input -> BitmapFactory.decodeStream(input) }
                ?: error("Unable to decode image at $uri")
        val rotationDegrees = readExifRotationDegrees(context, uri)
        if (rotationDegrees == 0) return original

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
        if (rotated !== original) original.recycle()
        return rotated
    }

    private fun readExifRotationDegrees(
        context: Context,
        uri: Uri,
    ): Int {
        val orientation =
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun centerCropToSquare(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val left = (bitmap.width - size) / 2
        val top = (bitmap.height - size) / 2
        return Bitmap.createBitmap(bitmap, left, top, size, size)
    }
}
