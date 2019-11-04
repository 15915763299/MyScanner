package com.myscanner.scan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.myscanner.App;
import com.myscanner.R;
import com.myscanner.utils.CameraUtils;
import com.myscanner.utils.SoundUtils;
import com.myscanner.view.MashCodeView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;

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

    private Camera mCamera;
    private ActivityHandler activityHandler;
    private DecodeThread decodeThread;
    private SoundUtils soundUtils;
    private ImageScanner scanner;

    private Camera.Size optimalSize;
    private Rect scanImageRect;
    private int scanSuccessCount = 0;
    private boolean isStopCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_code_collector);

        finder_view = findViewById(R.id.finder_view);
        tx_count = findViewById(R.id.tx_count);
        tx_code = findViewById(R.id.tx_code);
        tx_count.setVisibility(View.INVISIBLE);
        tx_code.setVisibility(View.INVISIBLE);

        surface_view = findViewById(R.id.surface_view);
        SurfaceHolder surfaceHolder = surface_view.getHolder();
        surfaceHolder.addCallback(this);

        soundUtils = new SoundUtils(App.getApp(), SoundUtils.RING_SOUND);
        soundUtils.putSound(0, R.raw.beep);

        //创建扫描器
        scanner = new ImageScanner();
        //行扫描间隔
        scanner.setConfig(0, Config.X_DENSITY, 2);
        //列扫描间隔
        scanner.setConfig(0, Config.Y_DENSITY, 2);
        //Disable all the Symbols
        scanner.setConfig(0, Config.ENABLE, 0);
        //Only symbolType is enable
        int[] symbolTypeArray = new int[]{Symbol.CODE39, Symbol.QRCODE, Symbol.EAN13, Symbol.CODE128};
        for (int symbolType : symbolTypeArray) {
            scanner.setConfig(symbolType, Config.ENABLE, 1);
        }

        findViewById(R.id.btn_show_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ActCodeScanner.this);
                builder.setTitle("Test").setMessage("test").create().show();
            }
        });
        getPermission();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        logger.error("surfaceCreated");
        initCamera();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        logger.error("surfaceChanged");
        if (surfaceHolder.getSurface() != null) {
            if (mCamera != null) {
                mCamera.stopPreview();
                isStopCamera = true;
            }

            try {
                setCameraParameter(surfaceHolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
            startCamera();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        logger.error("surfaceDestroyed");
        releaseCamera();
    }

    private void initCamera() {
        try {
            int cameraIndex = CameraUtils.findCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (cameraIndex == -1) {
                mCamera = Camera.open();
            } else {
                mCamera = Camera.open(cameraIndex);
            }

            if (activityHandler == null) {
                activityHandler = new ActivityHandler(this);
                decodeThread = new DecodeThread(this);
                decodeThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mCamera = null;
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 设置照相机参数
     */
    private void setCameraParameter(SurfaceHolder surfaceHolder) throws Exception {
        // 获取各项参数
        Camera.Parameters parameters = mCamera.getParameters();
        // 设置图片格式，Android下摄像头预览数据为 ImageFormat.NV21 格式
        // parameters.setPictureFormat(ImageFormat.NV21);
        parameters.setPictureFormat(ImageFormat.JPEG);
        // 设置照片质量
        parameters.setJpegQuality(100);

        if (optimalSize == null) {
            optimalSize = CameraUtils.calBestPreviewSize(parameters, 0.6f);
        }
        if (scanImageRect == null) {
            scanImageRect = finder_view.getScanImageRect(optimalSize.height, optimalSize.width);
        }
        parameters.setPictureSize(optimalSize.width, optimalSize.height);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

        CameraUtils.setFlash(parameters);
        CameraUtils.setZoom(parameters);
        if ((!Camera.Parameters.ANTIBANDING_50HZ.equals(parameters.getAntibanding()))
                && CameraUtils.isSupportedAntibanding(parameters, Camera.Parameters.ANTIBANDING_50HZ)) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
        }

        mCamera.setParameters(parameters);
        //竖屏显示
        if (!Build.MODEL.equals("K1")) {
            mCamera.setDisplayOrientation(90);
        }
        mCamera.setPreviewDisplay(surfaceHolder);
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
    protected void onResume() {
        super.onResume();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            surface_view.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            surface_view.setVisibility(View.GONE);
        } else {
            releaseCamera();
        }

        if (activityHandler != null) {
            quitDecodeThread();
            activityHandler = null;
        }
    }

    private void quitDecodeThread() {
        decodeThread.getHandler().getLooper().quit();
        try {
            decodeThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 清除Message
        activityHandler.removeMessages(R.id.decode_succeeded);
        activityHandler.removeMessages(R.id.decode_finish);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    //**************************************************************************
    // * 6.0+权限
    //**************************************************************************

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                surface_view.setVisibility(View.VISIBLE);
                Toast.makeText(ActCodeScanner.this, "开始扫描", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ActCodeScanner.this, "相机功能被禁止", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    //**************************************************************************
    // * 回调
    //**************************************************************************
    /**
     * 预览数据
     */
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            logger.error("start decode");
            if (mCamera != null) {
                mCamera.setPreviewCallback(null);
            }

            //创建解码图像，并转换为原始灰度数据，注意图片是被旋转了90度的，将扫描框的TOP作为left裁剪
            Image source = new Image(optimalSize.width, optimalSize.height, "Y800");
            source.setCrop(scanImageRect.top, 0, scanImageRect.height(), optimalSize.width);
            source.setData(data);

            if (!isFinishing()) {
                Message message = Message.obtain(decodeThread.getHandler(), R.id.decode, source);
                message.sendToTarget();
            }
        }
    };

    /**
     * 自动对焦
     */
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            if (activityHandler != null) {
                Message message = activityHandler.obtainMessage(R.id.auto_focus, success);
                activityHandler.sendMessageDelayed(message, 1000);
            }
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
        tx_count.setText(String.valueOf(++scanSuccessCount));
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

    public SoundUtils getSoundUtils() {
        return soundUtils;
    }

    public ImageScanner getScanner() {
        return scanner;
    }
}
