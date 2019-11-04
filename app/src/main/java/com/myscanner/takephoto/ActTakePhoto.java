package com.myscanner.takephoto;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.myscanner.R;
import com.myscanner.utils.BitmapUtils;
import com.myscanner.utils.CameraUtils;

import java.lang.ref.SoftReference;

public class ActTakePhoto extends Activity
        implements SurfaceHolder.Callback, Camera.PictureCallback, View.OnClickListener {

    private static final String TAG = ActTakePhoto.class.getSimpleName();
    private ImageView img;
    private LinearLayout llt_choice;
    private TextView tx_photo;

    private Camera mCamera;
    private boolean isStopCamera = false;
    private boolean safeToTakePicture = true;
    private byte[] data;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_take_photo);

        SurfaceView surface_view = findViewById(R.id.surface_view);
        ImageView img_wrong = findViewById(R.id.img_wrong);
        ImageView img_correct = findViewById(R.id.img_correct);
        img = findViewById(R.id.img);
        llt_choice = findViewById(R.id.llt_choice);
        tx_photo = findViewById(R.id.tx_photo);

        tx_photo.setOnClickListener(this);
        img_wrong.setOnClickListener(this);
        img_correct.setOnClickListener(this);
        surface_view.setOnClickListener(this);

        SurfaceHolder mHolder = surface_view.getHolder();
        mHolder.addCallback(this);
    }

    private void toast(String tip) {
        Toast.makeText(this, tip, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tx_photo:
                tx_photo.setVisibility(View.GONE);
                takePicture();
                break;
            case R.id.img_correct:
                toast("正在处理图片");
                new MyTask(this).execute();
                break;
            case R.id.img_wrong:
                startCamera();
                break;
            case R.id.surface_view:
                if (mCamera != null && safeToTakePicture) {
                    mCamera.autoFocus(null);
                }
                break;
            default:
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        data = null;
        startCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCamera();
    }

    private void takePicture() {
        if (mCamera != null && safeToTakePicture) {
            mCamera.takePicture(null, null, this);
            isStopCamera = true;
            safeToTakePicture = false;
        }
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        if (data.length > 0) {
            llt_choice.setVisibility(View.VISIBLE);
            this.data = data;
        } else {
            startCamera();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }
        stopCamera();
        try {
            setCameraParameter(holder, width, height);
            startCamera();
        } catch (Exception e) {
            Log.e(TAG, "开启异常:" + e.toString());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        destroyCamera();
    }


    private void setCameraParameter(SurfaceHolder holder, int width, int height) throws Exception {
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPictureFormat(ImageFormat.JPEG);
        parameters.setJpegQuality(100);

        float targetRatio;
        switch (Build.MODEL) {
            case "APOS A8":
                targetRatio = 0.6f;
                break;
            default:
                //反过来
                targetRatio = (float) width / (float) height;
        }

        Camera.Size optimalSize = CameraUtils.calBestPreviewSize(parameters, targetRatio);
        parameters.setPictureSize(optimalSize.width, optimalSize.height);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        mCamera.setParameters(parameters);
        //竖屏显示
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewDisplay(holder);
    }


    private void initCamera() {
        try {
            int cameraIndex = CameraUtils.findCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (cameraIndex == -1) {
                mCamera = Camera.open();
            } else {
                mCamera = Camera.open(cameraIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            destroyCamera();
        }
    }

    private void destroyCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            isStopCamera = true;
        }
    }

    private void startCamera() {
        if (mCamera != null && isStopCamera) {
            llt_choice.setVisibility(View.GONE);
            tx_photo.setVisibility(View.VISIBLE);

            try {
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "预览异常:" + e.toString());
            }
            safeToTakePicture = true;
            isStopCamera = false;
            data = null;
            mCamera.autoFocus(null);
        }
    }

    /**
     * 获取拍照的照片
     */
    private static class MyTask extends AsyncTask<Void, Void, Bitmap> {

        private SoftReference<ActTakePhoto> sf;

        MyTask(ActTakePhoto act) {
            this.sf = new SoftReference<>(act);
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            if (sf != null && sf.get() != null) {
                ActTakePhoto act = sf.get();
                if (act.data != null) {
                    Bitmap bitmap = BitmapUtils.Bytes2Bitmap(act.data);
                    //照片反转90°
                    bitmap = BitmapUtils.rotateBitmap(bitmap, 90);
                    //裁剪
//                    Bitmap cut = null;
//                    if (bitmap != null) {
//                        int height = bitmap.getHeight();
//                        int width = bitmap.getWidth();
//                        cut = Bitmap.createBitmap(bitmap,
//                                width * 3 / 20,
//                                height / 10,
//                                width * 4 / 5,
//                                height * 3 / 5
//                        );
//                    }
//                    String result = BitmapUtils.bitmapToBase64(bitmap);
//                    if (cut != null) {
//                        cut.recycle();
//                    }
//                    if (bitmap != null) {
//                        bitmap.recycle();
//                    }
                    return bitmap;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap pic) {
            if (sf != null && sf.get() != null) {
                ActTakePhoto act = sf.get();
                act.img.setImageBitmap(pic);
                act.toast("处理图片成功");
                act.startCamera();
            }
        }
    }


}
