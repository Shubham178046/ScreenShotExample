package com.example.screenshotexample

import android.graphics.*
import java.io.ByteArrayOutputStream


object Functions {

    fun BitMapToString(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val b = baos.toByteArray()
        return android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT)
    }
}