package pg.autyzm.friendlyemotions.therapist.materials.newMaterial

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

private const val CAMERA_CACHE_DIR_NAME = "camera"
private const val PENDING_IMAGES_CACHE_DIR_NAME = "pending_images"

/**
 * Creates a fresh destination file for [androidx.activity.result.contract.ActivityResultContracts.TakePicture]
 * to write into, plus the `FileProvider` [Uri] the camera app needs — the same
 * `${applicationId}.fileprovider` authority and `cache/camera/` path already declared in
 * `app/src/main/AndroidManifest.xml` / `res/xml/file_paths.xml`.
 */
fun createCameraCaptureTarget(context: Context): Pair<File, Uri> {
    val cameraDir = File(context.cacheDir, CAMERA_CACHE_DIR_NAME).apply { mkdirs() }
    val file = File(cameraDir, "capture_${UUID.randomUUID()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return file to uri
}

/**
 * A fresh cache file for [SquareImageCropper.cropToSquareJpeg]'s output — one per picked/captured
 * image, later copied into `filesDir/images/` by `EmotionImageRepository.addImages` on save.
 */
fun createPendingImageFile(context: Context): File {
    val dir = File(context.cacheDir, PENDING_IMAGES_CACHE_DIR_NAME).apply { mkdirs() }
    return File(dir, "${UUID.randomUUID()}.jpg")
}
