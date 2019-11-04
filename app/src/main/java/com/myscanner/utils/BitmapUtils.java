package com.myscanner.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Base64;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapUtils {

    private static final String TAG = BitmapUtils.class.getSimpleName();

//    /**
//     * 转换条形码
//     *
//     * @param str 条形码的字符串
//     */
//    public static Bitmap BarcodeFormatCode(String str) {
//        int width = 800;
//        int height = 160;
//        BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;
//        BitMatrix matrix = null;
//        try {
//            matrix = new MultiFormatWriter().encode(str, barcodeFormat, width, height, null);
//            return bitMatrix2Bitmap(matrix);
//        } catch (WriterException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

//    public static Bitmap bitMatrix2Bitmap(BitMatrix matrix) {
//        int w = matrix.getWidth();
//        int h = matrix.getHeight();
//        int[] rawData = new int[w * h];
//        for (int i = 0; i < w; i++) {
//            for (int j = 0; j < h; j++) {
//                int color = Color.WHITE;
//                if (matrix.get(i, j)) {
//                    // 有内容的部分，颜色设置为黑色，可以自己修改成其他颜色
//                    color = Color.BLACK;
//                }
//                rawData[i + (j * w)] = color;
//            }
//        }
//        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//        bitmap.setPixels(rawData, 0, w, 0, 0, w, h);
//        return bitmap;
//    }

    /**
     * 旋转照片
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(degree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(),
                    bitmap.getHeight(), matrix, true);
            return bitmap;
        }
        return null;
    }

    public static Bitmap Bytes2Bitmap(byte[] b) {
        if (b.length != 0) {
            return BitmapFactory.decodeByteArray(b, 0, b.length);
        } else {
            return null;
        }
    }

    /**
     * bitmap转为base64
     */
    public static String bitmapToBase64(Bitmap bitmap) {

        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            if (bitmap != null) {
                baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

                baos.flush();
                baos.close();

                byte[] bitmapBytes = baos.toByteArray();
                result = Base64.encodeToString(bitmapBytes, Base64.DEFAULT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.flush();
                    baos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * base64转为bitmap
     */
    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    //**************************************************************************
    // * 存储操作
    //**************************************************************************

    /**
     * 保存图片到 App 私有空间
     */
    public static boolean saveBitmap(Context context, Bitmap mBitmap, String fileName) {
        boolean isSuccess = false;
        if (mBitmap != null) {
            try {
                //该方法将在/data/data/<Package Name>/files/目录下打开一个指定名字的文件，如果不存在，会新创建一个
                //getFilesDir()方法用于获取/data/data/<package name>/files目录
                FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                isSuccess = mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Log.w(TAG, "save bitmap: " + isSuccess);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "bitmap is null");
        }
        return isSuccess;
    }

    /**
     * 从 APP 私有空间取出图片
     */
    public static Bitmap readBitmap(Context context, String fileName) {
        Bitmap bitmap = null;
        String path = context.getFilesDir() + "/" + fileName;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        if (options.outWidth < 4800 && options.outHeight < 4800) {
            if (options.outWidth < 2400 && options.outHeight < 2400) {
                options.inSampleSize = 1;
            } else {
                options.inSampleSize = 2;
            }
        } else {
            options.inSampleSize = 4;
        }
        options.inJustDecodeBounds = false;
        try {
            bitmap = BitmapFactory.decodeFile(path, options);
        } catch (OutOfMemoryError oom) {
            oom.printStackTrace();
        }
        return bitmap;
    }


//    //把白色转换成透明
//    public static Bitmap getImageToChange(Bitmap mBitmap) {
//        Bitmap createBitmap = Bitmap.createBitmap(mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888);
//        if (mBitmap != null) {
//            int mWidth = mBitmap.getWidth();
//            int mHeight = mBitmap.getHeight();
//            for (int i = 0; i < mHeight; i++) {
//                for (int j = 0; j < mWidth; j++) {
//                    int color = mBitmap.getPixel(j, i);
//                    int g = Color.green(color);
//                    int r = Color.red(color);
//                    int b = Color.blue(color);
//                    int a = Color.alpha(color);
//                    if(g>=250&&r>=250&&b>=250){
//                        a = 0;
//                    }
//                    color = Color.argb(a, r, g, b);
//                    createBitmap.setPixel(j, i, color);
//                }
//            }
//        }
//        return createBitmap;
//
//    }


//    /**
//     * 旋转图片
//     * @param bitmap
//     * @param rotation
//     * @Return
//     */
//    public static Bitmap getRotatedBitmap(Bitmap bitmap, int rotation) {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(rotation);
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
//                bitmap.getHeight(), matrix, false);
//    }
//
//    /**
//     * 镜像翻转图片
//     * @param bitmap
//     * @Return
//     */
//    public static Bitmap getFlipBitmap(Bitmap bitmap) {
//        Matrix matrix = new Matrix();
//        matrix.setScale(-1, 1);
//        matrix.postTranslate(bitmap.getWidth(), 0);
//        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
//                bitmap.getHeight(), matrix, false);
//    }

//    public static Bitmap handleImageFromlbum(Intent data, Activity activity){
//        String imagePath = null;
//        Uri uri = data.getData();
//        if (DocumentsContract.isDocumentUri(activity,uri)){
//            //如果是document类型的Uri,则通过document id处理
//            String docId = DocumentsContract.getDocumentId(uri);
//            if ("com.android.providers.media.documents".equals(uri.getAuthority())){
//                String id = docId.split(":")[1];//解析出数字格式的id
//                String selection = MediaStore.Images.Media._ID + "=" + id;
//                imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,selection,activity);
//            }else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())){
//                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),Long.valueOf(docId));
//                imagePath = getImagePath(contentUri,null,activity);
//            }
//        }else if ("content".equalsIgnoreCase(uri.getScheme())){
//            //如果是content类型的Uri，则使用普通方式处理
//            imagePath = getImagePath(uri,null,activity);
//        }else if ("file".equalsIgnoreCase(uri.getScheme())){
//            //如果是file类型的Uri，直接获取图片路径即可
//            imagePath = uri.getPath();
//        }
//
//        return  path2Bitmap(imagePath);
//    }

//    private static String getImagePath(Uri uri,String selection,Activity activity){
//        String path = null;
//        //通过Uri和selection来获取真实的图片路径
//        Cursor cursor = activity.getContentResolver().query(uri,null,selection,null,null);
//        if (cursor != null){
//            if (cursor.moveToFirst()){
//                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
//            }
//            cursor.close();
//        }
//        return path;
//    }
//
//
//    private static  Bitmap path2Bitmap(String imagePath){
//        Bitmap bitmap = null;
//        if (imagePath != null){
//            bitmap = BitmapFactory.decodeFile(imagePath);
//        }
//        return bitmap;
//    }
//
//    public static final int CHOOSE_PHOTO = 1;
//
//    /**
//     * 打开系统相册 选择图片
//     * @param activity
//     */
//    public static void openAlbum(Activity activity,int requestCode) {
//        Intent intent = new Intent("android.intent.action.GET_CONTENT");
//        intent.setType("image/*");
//        activity.startActivityForResult(intent, requestCode);//打开相册
//    }
//
//
//    public static String getRealFilePath(Context context, final Uri uri) {
//        if (null == uri)
//            return null;
//        final String scheme = uri.getScheme();
//        String data = null;
//        if (scheme == null)
//            data = uri.getPath();
//        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
//            data = uri.getPath();
//        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
//            Cursor cursor = context.getContentResolver().query(uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null);
//            if (null != cursor) {
//                if (cursor.moveToFirst()) {
//                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//                    if (index > -1) {
//                        data = cursor.getString(index);
//                    }
//
//                }
//                cursor.close();
//            }
//            if (data == null) {
//                data = getImageAbsolutePath(context, uri);
//            }
//
//        }
//        return data;
//    }
    /*-------------根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换------------------------------------------------*/
//    /**
//     * 根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换
//     *
//     * @param context
//     * @param imageUri
//     * @author yaoxing
//     * @date 2014-10-12
//     */
//    @TargetApi(19)
//    public static String getImageAbsolutePath(Context context, Uri imageUri) {
//        if (context == null || imageUri == null)
//            return null;
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
//            if (isExternalStorageDocument(imageUri)) {
//                String docId = DocumentsContract.getDocumentId(imageUri);
//                String[] split = docId.split(":");
//                String type = split[0];
//                if ("primary".equalsIgnoreCase(type)) {
//                    return Environment.getExternalStorageDirectory() + "/" + split[1];
//                }
//            } else if (isDownloadsDocument(imageUri)) {
//                String id = DocumentsContract.getDocumentId(imageUri);
//                Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
//                return getDataColumn(context, contentUri, null, null);
//            } else if (isMediaDocument(imageUri)) {
//                String docId = DocumentsContract.getDocumentId(imageUri);
//                String[] split = docId.split(":");
//                String type = split[0];
//                Uri contentUri = null;
//                if ("image".equals(type)) {
//                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
//                } else if ("video".equals(type)) {
//                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
//                } else if ("audio".equals(type)) {
//                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
//                }
//                String selection = MediaStore.Images.Media._ID + "=?";
//                String[] selectionArgs = new String[] { split[1] };
//                return getDataColumn(context, contentUri, selection, selectionArgs);
//            }
//        } // MediaStore (and general)
//        else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
//            // Return the remote address
//            if (isGooglePhotosUri(imageUri))
//                return imageUri.getLastPathSegment();
//            return getDataColumn(context, imageUri, null, null);
//        }
//        // File
//        else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
//            return imageUri.getPath();
//        }
//        return null;
//    }

//    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
//        Cursor cursor = null;
//        String column = MediaStore.Images.Media.DATA;
//        String[] projection = { column };
//        try {
//            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
//            if (cursor != null && cursor.moveToFirst()) {
//                int index = cursor.getColumnIndexOrThrow(column);
//                return cursor.getString(index);
//            }
//        } finally {
//            if (cursor != null)
//                cursor.close();
//        }
//        return null;
//    }
//    /**
//     * @param uri
//     *                The Uri to check.
//     * @return Whether the Uri authority is ExternalStorageProvider.
//     */
//    public static boolean isExternalStorageDocument(Uri uri) {
//        return "com.android.externalstorage.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * @param uri
//     *                The Uri to check.
//     * @return Whether the Uri authority is DownloadsProvider.
//     */
//    public static boolean isDownloadsDocument(Uri uri) {
//        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * @param uri
//     *                The Uri to check.
//     * @return Whether the Uri authority is MediaProvider.
//     */
//    public static boolean isMediaDocument(Uri uri) {
//        return "com.android.providers.media.documents".equals(uri.getAuthority());
//    }
//
//    /**
//     * @param uri
//     *                The Uri to check.
//     * @return Whether the Uri authority is Google Photos.
//     */
//    public static boolean isGooglePhotosUri(Uri uri) {
//        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
//    }
    /*-------------根据Uri获取图片绝对路径，解决Android4.4以上版本Uri转换------------------------------------------------*/


}




