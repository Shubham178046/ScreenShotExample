package com.example.screenshotexample

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.annotation.NonNull
import java.io.*
import java.nio.channels.FileChannel

object FileUtils {
    private const val TAG = "FileUtils"
    var currentPhotoPath: String = ""
    fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.getAuthority()
    }

    fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.getAuthority()
    }

    fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.getAuthority()
    }

    fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.getAuthority()
    }

    fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(
            column
        )
        try {
            cursor = uri?.let {
                context.getContentResolver().query(
                    it, projection, selection, selectionArgs,
                    null
                )
            }
            if (cursor != null && cursor.moveToFirst()) {
                val column_index: Int = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(column_index)
            }
        } catch (ex: IllegalArgumentException) {
        } finally {
            if (cursor != null) {
                cursor.close()
            }
        }
        return null
    }

    /*fun getPath(context: Context?, uri: Uri): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(
                context,
                uri
            )
        ) {
            if (isExternalStorageDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else if (isDownloadsDocument(uri)) {
                try {
                    val id: String = DocumentsContract.getDocumentId(uri)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        java.lang.Long.valueOf(id)
                    )
                    return getDataColumn(context!!, contentUri, null, null)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                    val segments = uri.pathSegments
                    if (segments.size > 1) {
                        val rawPath = segments[1]
                        return if (!rawPath.startsWith("/")) {
                            rawPath.substring(rawPath.indexOf("/"))
                        } else {
                            rawPath
                        }
                    }
                }
            } else if (isMediaDocument(uri)) {
                val docId: String = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if (("image" == type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if (("video" == type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if (("audio" == type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context!!, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context!!,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }*/
    @Throws(Exception::class)
    open fun getPath(context: Context?, uri: Uri): String? {
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

// DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                return if ("primary".equals(type, ignoreCase = true)) {
                    Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                } else {
                    "/storage/" + docId.replace(":", "/")
                }

                // TODO handle non-primary volumes
            } else if (isDownloadsDocument(uri)) {
                var contentUri: Uri? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    var docId = DocumentsContract.getDocumentId(uri)
                    if (docId != null && docId.startsWith("raw:")) {
                        docId = docId.substring(4)
                        contentUri = if (getRealPathFromURI1(
                                context!!,
                                uri
                            )?.let { getFileExt(it) } == null
                        ) {
                            Uri.parse(getRealPathFromURI1(context, uri))
                        } else {
                            Uri.parse(docId)
                        }
                    } else {
                        contentUri = Uri.parse(
                            "/storage/emulated/0/Download/" + getRealPathFromURI1(
                                context!!,
                                uri
                            )
                        )
                    }
                } else {
                    val id = DocumentsContract.getDocumentId(uri)
                    if (id != null && id.startsWith("raw:")) {
                        return id.substring(4)
                    }
                    val contentUriPrefixesToTry = arrayOf(
                        "content://downloads/public_downloads",
                        "content://downloads/my_downloads",
                        "content://downloads/all_downloads"
                    )
                    for (contentUriPrefix in contentUriPrefixesToTry) {
                        contentUri = ContentUris.withAppendedId(
                            Uri.parse(contentUriPrefix),
                            java.lang.Long.valueOf(id)
                        )
                        try {
                            val path = getDataColumn(context!!, contentUri, null, null)
                            if (path != null) {
                                return path
                            }
                        } catch (e: Exception) {
                        }
                    }
                }


                /* final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/my_downloads"), Long.valueOf(id));*/
                return getDataColumn(context!!, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).toTypedArray()
                val type = split[0]
                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(
                    split[1]
                )
                return getDataColumn(context!!, contentUri, selection, selectionArgs)
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {

                // Return the remote address
                return if (isGooglePhotosUri(uri)) {
                    getDriveFilePath(uri, context!!)
                } else getDataColumn(context!!, uri, null, null)
                //  return getDriveFilePath(uri,context);
            }

// File
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            return getDataColumn(context!!, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getDriveFilePath(uri: Uri, context: Context): String? {
        val returnCursor = context.contentResolver.query(uri, null, null, null, null)
        /*
         * Get the column indexes of the data in the Cursor,
         *     * move to the first row in the Cursor, get the data,
         *     * and display it.
         * */
        val nameIndex = returnCursor!!.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        val size = java.lang.Long.toString(returnCursor.getLong(sizeIndex))
        val file = File(context.cacheDir, name)
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            var read = 0
            val maxBufferSize = 1 * 1024 * 1024
            val bytesAvailable = inputStream!!.available()

            //int bufferSize = 1024;
            val bufferSize = Math.min(bytesAvailable, maxBufferSize)
            val buffers = ByteArray(bufferSize)
            while (inputStream.read(buffers).also { read = it } != -1) {
                outputStream.write(buffers, 0, read)
            }
            inputStream.close()
            outputStream.close()
        } catch (e: java.lang.Exception) {
        }
        return file.path
    }

    @Throws(IOException::class)
    fun copyFile(@NonNull pathFrom: String, @NonNull pathTo: String?) {
        if (pathFrom.equals(pathTo, ignoreCase = true)) {
            return
        }
        var outputChannel: FileChannel? = null
        var inputChannel: FileChannel? = null
        try {
            inputChannel = FileInputStream(File(pathFrom)).getChannel()
            outputChannel = FileOutputStream(File(pathTo)).getChannel()
            inputChannel.transferTo(0, inputChannel.size(), outputChannel)
            inputChannel.close()
        } finally {
            if (inputChannel != null) inputChannel.close()
            if (outputChannel != null) outputChannel.close()
        }
    }

    fun getFileExt(fileName: String): String? {
        return if (fileName.contains(".")) {
            fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length)
        } else {
            null
        }
    }

    fun getRealPathFromURI1(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return if (cursor == null) {
            uri.path
        } else {
            cursor.moveToFirst()
            val index = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val realPath = cursor.getString(index)
            cursor.close()
            realPath
        }
    }

    fun getRealPathFromURI(context: Context, uri: Uri?): String? {
        val cursor = context.contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToFirst()
        val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        return cursor.getString(idx)
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path: String = MediaStore.Images.Media.insertImage(
            inContext.contentResolver,
            inImage, "Title", null
        )
        return Uri.parse(path)
    }

    fun getFile(context: Context?, uri: Uri?): File? {
        if (uri != null) {
            val path: String = getPath(context!!, uri)!!
            if (path != null) {
                return File(path)
            }
        }
        return null
    }

    fun getImageFile(): File {
        val imageFileName = "JPEG_" + System.currentTimeMillis() + "_"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Camera"
        )
        val file = File.createTempFile(imageFileName, ".jpg", storageDir)
        currentPhotoPath = "file:" + file.getAbsolutePath()
        return file
    }

    fun getFileName(str: String): String? {
        return str.substring(str.lastIndexOf("/") + 1)
    }

    fun getCurrentImagePath(): String {
        return currentPhotoPath
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            } finally {
                if (cursor != null) {
                    cursor.close()
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf(File.separator)
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}