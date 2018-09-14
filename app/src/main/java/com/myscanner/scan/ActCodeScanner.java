package com.myscanner.scan;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.myscanner.R;
import com.myscanner.utils.CameraUtils;
import com.myscanner.view.MashCodeView;

import net.sourceforge.zbar.Image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 扫码界面
 * 使用 startActivityForResult 跳转，然后回传扫描到的信息
 */
public class ActCodeScanner extends Activity implements SurfaceHolder.Callback {

    private static final Logger logger = LoggerFactory.getLogger(ActCodeScanner.class);
    private static final int REQUEST_CAMERA = 1000;

    private SurfaceView surface_view;
    private MashCodeView finder_view;
    private TextView tx_count, tx_code;

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ActivityHandler activityHandler;
    private DecodeThread decodeThread;
    private Camera.Size optimalSize;
    private Rect scanImageRect;

    private int scanSuccessCount = 0;
    private boolean isStopCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_code_collector);

        surface_view = findViewById(R.id.surface_view);
        finder_view = findViewById(R.id.finder_view);
        tx_count = findViewById(R.id.tx_count);
        tx_code = findViewById(R.id.tx_code);
        tx_count.setVisibility(View.INVISIBLE);
        tx_code.setVisibility(View.INVISIBLE);

        mHolder = surface_view.getHolder();
        mHolder.addCallback(this);
        activityHandler = new ActivityHandler(this);
        decodeThread = new DecodeThread(this);
        decodeThread.start();

        getPermission();
    }

    private void getPermission() {
        if (Build.VERSION.SDK_INT > 22) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //先判断有没有权限 ，没有就在这里进行权限的申请
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, REQUEST_CAMERA);
                //关闭 surface view
                surface_view.setVisibility(View.GONE);
            } /*else {说明已经获取到摄像头权限了 想干嘛干嘛}*/
        } /*else {这个说明系统版本在6.0之下，不需要动态获取权限。}*/
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            int CameraIndex = CameraUtils.findCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (CameraIndex == -1) {
                mCamera = Camera.open();
            } else {
                mCamera = Camera.open(CameraIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        if (mHolder.getSurface() == null) return;
        stopCamera();
        try {
            setCameraParameter();
            startCamera();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 设置照相机参数
     */
    private void setCameraParameter() throws Exception {
        Camera.Parameters parameters = mCamera.getParameters(); // 获取各项参数
        parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
        parameters.setJpegQuality(100); // 设置照片质量
        optimalSize = CameraUtils.calBestPreviewSize(parameters, 0.6f);
        parameters.setPictureSize(optimalSize.width, optimalSize.height);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        scanImageRect = finder_view.getScanImageRect(optimalSize.height, optimalSize.width);

        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);//竖屏显示
        mCamera.setPreviewDisplay(mHolder);
        mCamera.setPreviewCallback(previewCallback);
    }

    private void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.autoFocus(null);
            isStopCamera = true;
            logger.error("stop preview");
        }
    }

    private void startCamera() {
        if (mCamera != null) {
            mCamera.startPreview();
            mCamera.setPreviewCallback(previewCallback);
            mCamera.autoFocus(autoFocusCallback);
            isStopCamera = false;
            logger.error("start preview");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    surface_view.setVisibility(View.VISIBLE);
                    Toast.makeText(ActCodeScanner.this, "开始扫描", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ActCodeScanner.this, "相机功能被禁止", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        quitDecodeThread();
    }

    private void quitDecodeThread() {
        Message quit = Message.obtain(decodeThread.getHandler(), R.id.quit_decode);
        quit.sendToTarget();
        try {
            decodeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 清除Message
        activityHandler.removeMessages(R.id.decode_succeeded);
        activityHandler.removeMessages(R.id.decode_finish);
    }

    //**************************************************************************
    // * 回调
    //**************************************************************************
    /**
     * 预览数据
     */
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (previewCallback != null) {
                logger.error("start decode");

                //创建解码图像，并转换为原始灰度数据，注意图片是被旋转了90度的，将扫描框的TOP作为left裁剪
                Image source = new Image(optimalSize.width, optimalSize.height, "Y800");
                source.setCrop(scanImageRect.top, 0, scanImageRect.height(), optimalSize.width);
                source.setData(data);

                Message message = Message.obtain(decodeThread.getHandler(), R.id.decode, source);
                if (!isFinishing()) {
                    message.sendToTarget();
                }
            }
        }
    };

    /**
     * 自动对焦
     */
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            Message message = activityHandler.obtainMessage(R.id.auto_focus, success);
            activityHandler.sendMessageDelayed(message, 1000);
        }
    };

    //**************************************************************************
    // * get & set
    //**************************************************************************
    public void setCode(String code) {
        if (tx_count.getVisibility() == View.INVISIBLE) {
            tx_count.setVisibility(View.VISIBLE);
        }
        if (tx_code.getVisibility() == View.INVISIBLE) {
            tx_code.setVisibility(View.VISIBLE);
        }
        tx_count.setText(++scanSuccessCount + "");
        tx_code.setText(code);
    }

    public Camera getCamera() {
        return mCamera;
    }

    public boolean isStopCamera() {
        return isStopCamera;
    }

    public Camera.AutoFocusCallback getAutoFocusCallback() {
        return autoFocusCallback;
    }

    public ActivityHandler getActivityHandler() {
        return activityHandler;
    }

    public void setPreviewCallbackToCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCallback);
        }
    }
}
