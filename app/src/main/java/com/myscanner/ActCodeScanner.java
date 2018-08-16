package com.myscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.myscanner.utils.CameraUtils;
import com.myscanner.utils.SoundUtils;
import com.myscanner.view.MashCodeView;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;


/**
 * 扫码界面
 * 使用 startActivityForResult 跳转，然后回传扫描到的信息
 */
public class ActCodeScanner extends Activity implements SurfaceHolder.Callback {

    private static final Logger logger = LoggerFactory.getLogger(ActCodeScanner.class);
    private static final int REQUEST_CAMERA = 1000;

    private SurfaceView surface_view;
    private MashCodeView finder_view;
    private TextView tx_code;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Handler autoFocusHandler;
    private ImageScanner scanner;
    private SoundUtils soundUtils;

    private boolean isStopDecode = true;
    private boolean isStopCamera = false;
    private boolean vibrate = false;

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            mCamera.setPreviewCallback(previewCallback);
            mCamera.autoFocus(autoFocusCallback);
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_code_collector);

        surface_view = findViewById(R.id.surface_view);
        finder_view = findViewById(R.id.finder_view);
        tx_code = findViewById(R.id.tx_code);
        tx_code.setVisibility(View.INVISIBLE);

        mHolder = surface_view.getHolder();
        mHolder.addCallback(this);
        autoFocusHandler = new Handler();

        scanner = new ImageScanner();//创建扫描器
        scanner.setConfig(0, Config.X_DENSITY, 2);//行扫描间隔
        scanner.setConfig(0, Config.Y_DENSITY, 2);//列扫描间隔
        scanner.setConfig(Symbol.PDF417, Config.ENABLE, 0);//是否禁止PDF417码，默认开启

//        scanner.setConfig(0, Config.ENABLE, 0);//Disable all the Symbols
//        int[] symbolTypeArray = new int[]{
//                Symbol.CODE39, Symbol.PDF417, Symbol.QRCODE, Symbol.CODE93, Symbol.CODE128
//        };
//        for (int symbolType : symbolTypeArray) {
//            scanner.setConfig(symbolType, Config.ENABLE, 1);//Only symbolType is enable
//        }

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
        Camera.Size optimalSize = CameraUtils.calBestPreviewSize(parameters, 0.6f);
        parameters.setPictureSize(optimalSize.width, optimalSize.height);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);

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

    /**
     * 预览数据
     */
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (isStopDecode) {
                isStopDecode = false;
                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();//获取预览分辨率
                Rect scanImageRect = finder_view.getScanImageRect(size.height, size.width);

                //创建解码图像，并转换为原始灰度数据，注意图片是被旋转了90度的
                Image source = new Image(size.width, size.height, "Y800");//(Zbar format)
                //图片旋转了90度，将扫描框的TOP作为left裁剪
                source.setCrop(scanImageRect.top, scanImageRect.left, scanImageRect.height(), scanImageRect.width());
                source.setData(data);//填充数据
                AsyncDecode asyncDecode = new AsyncDecode(ActCodeScanner.this);
                asyncDecode.execute(source);//调用异步执行解码
            }
        }
    };

    private static class AsyncDecode extends AsyncTask<Image, Void, String> {

        private SoftReference<ActCodeScanner> actCodeCollectorSr;

        AsyncDecode(ActCodeScanner actCodeCollector) {
            actCodeCollectorSr = new SoftReference<>(actCodeCollector);
        }

        @Override
        protected String doInBackground(Image... params) {
            String str = "";

            if (actCodeCollectorSr != null && actCodeCollectorSr.get() != null) {
                ActCodeScanner act = actCodeCollectorSr.get();

                Image src_data = params[0];//获取灰度数据
                int scanResultCode = act.scanner.scanImage(src_data);//解码，返回值为0代表失败，>0表示成功

                if (scanResultCode != 0) {
                    act.playBeepSoundAndVibrate();
                    SymbolSet symbols = act.scanner.getResults();
                    for (Symbol sym : symbols) {
                        str = sym.getData() + " " + act.getCodeName(sym.getType());//一般只有一个
                    }
                    logger.error("BarCode -----> " + str);
                }
            }
            return str;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (actCodeCollectorSr != null && actCodeCollectorSr.get() != null) {
                final ActCodeScanner act = actCodeCollectorSr.get();

                if (null != result && result.length() > 0) {
                    act.mCamera.setPreviewCallback(null);
                    act.mCamera.autoFocus(null);

                    if (act.tx_code.getVisibility() == View.INVISIBLE) {
                        act.tx_code.setVisibility(View.VISIBLE);
                    }
                    act.tx_code.setText(result);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            act.handler.sendEmptyMessage(0);
                        }
                    }).start();
                }
                act.isStopDecode = true;
            }
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

    //**************************************************************************
    // * 声音
    //**************************************************************************
    @Override
    protected void onResume() {
        super.onResume();
        initBeepSound();
        vibrate = false;
    }

    private void initBeepSound() {
        if (soundUtils == null) {
            soundUtils = new SoundUtils(this, SoundUtils.RING_SOUND);
            soundUtils.putSound(0, R.raw.beep);
        }
    }

    private static final long VIBRATE_DURATION = 200L;

    @SuppressLint("MissingPermission")
    private void playBeepSoundAndVibrate() {
        if (soundUtils != null) {
            soundUtils.playSound(0, SoundUtils.SINGLE_PLAY);
        }
        if (vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(VIBRATE_DURATION);
            }
        }
    }

    //**************************************************************************
    // * 自动对焦
    //**************************************************************************
    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.postDelayed(doAutoFocus, 500);
        }
    };

    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            if (null != mCamera && null != autoFocusCallback && !isStopCamera) {
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };

    private String getCodeName(int type) {
        switch (type) {
            case Symbol.NONE:
                return "No symbol decoded";
            case Symbol.PARTIAL:
                return "Symbol detected but not decoded";
            case Symbol.EAN8:
                return "EAN-8";
            case Symbol.UPCE:
                return "UPC-E";
            case Symbol.ISBN10:
                return "ISBN-10 (from EAN-13)";
            case Symbol.UPCA:
                return "UPC-A";
            case Symbol.EAN13:
                return "EAN-13";
            case Symbol.ISBN13:
                return "ISBN-13 (from EAN-13)";
            case Symbol.I25:
                return "Interleaved 2 of 5";
            case Symbol.DATABAR:
                return "DataBar (RSS-14)";
            case Symbol.DATABAR_EXP:
                return "DataBar Expanded";
            case Symbol.CODABAR:
                return "Codabar";
            case Symbol.CODE39:
                return "Code 39";
            case Symbol.PDF417:
                return "PDF417";
            case Symbol.QRCODE:
                return "QR Code";
            case Symbol.CODE93:
                return "Code 93";
            case Symbol.CODE128:
                return "Code 128";
            default:
                return "";
        }
    }
}
