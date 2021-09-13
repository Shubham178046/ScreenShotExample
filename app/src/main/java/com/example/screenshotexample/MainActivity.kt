package com.example.screenshotexample

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.constraintlayout.widget.ConstraintLayout
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*



class MainActivity : AppCompatActivity() {
    var baseImgStr: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<AppCompatButton>(R.id.btnCheck).setOnClickListener {
            TedPermission.with(this)
                .setPermissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ).setPermissionListener(object : PermissionListener {
                    override fun onPermissionGranted() {
                        var pic_name: String =
                            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                        var StoredPath = "SAVED_$pic_name.png"
                        getScreenShot(findViewById<ConstraintLayout>(R.id.root_layout),StoredPath)


                    }

                    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {

                    }

                }).check()

        }
    }

    private fun getScreenShot(view: View,StoredPath: String?): String? {
        var result = ""
        var mFileOutStream: OutputStream? = null
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) bgDrawable.draw(canvas)
        else canvas.drawColor(Color.WHITE)
        view.draw(canvas)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver?.also { resolver ->
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, StoredPath)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                        put(
                            MediaStore.MediaColumns.RELATIVE_PATH,
                            Environment.DIRECTORY_PICTURES + "/ScreenShots"
                        )
                    }
                    val imageUri: Uri? =
                        resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                    mFileOutStream = imageUri?.let {
                        resolver.openOutputStream(it)
                    }
                    result = FileUtils.getPath(this, imageUri!!)!!
                }
            } else {
                val imagesDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/DigitalSign")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                val image = File(imagesDir, StoredPath!!)
                mFileOutStream = FileOutputStream(image)
                result = image.path
            }
            baseImgStr = Functions.BitMapToString(returnedBitmap!!)
            returnedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, mFileOutStream)
            findViewById<ImageView>(R.id.imgScreenShot).setImageBitmap(returnedBitmap)
            mFileOutStream!!.flush()
            mFileOutStream!!.close()
        } catch (e: Exception) {

        }
        return result
    }
}