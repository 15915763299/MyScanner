package com.myscanner.scan;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.myscanner.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.SoftReference;

public class ActivityHandler extends Handler {

    private static final Logger logger = LoggerFactory.getLogger(ActivityHandler.class);
    private SoftReference<ActCodeScanner> sr;

    ActivityHandler(ActCodeScanner act) {
        this.sr = new SoftReference<>(act);
    }

    @Override
    public void handleMessage(Message msg) {
        if (sr != null && sr.get() != null) {
            ActCodeScanner act = sr.get();

            switch (msg.what) {
                case R.id.auto_focus:
                    logger.error("auto_focus");
                    Camera camera = act.getCamera();
                    Camera.AutoFocusCallback autoFocusCallback = act.getAutoFocusCallback();
                    boolean isAutoFocusSuccess = (boolean) msg.obj;

                    if (isAutoFocusSuccess && !act.isStopCamera() &&
                            camera != null && autoFocusCallback != null) {
                        camera.autoFocus(autoFocusCallback);
                    }
                    break;
//                case R.id.add_callbacks:
//                    logger.error("add_callbacks");
//                    act.addCallBacks();
//                    break;
                case R.id.decode_succeeded:
                    logger.error("decode_succeeded");
                    String result = (String) msg.obj;
                    if (!TextUtils.isEmpty(result)) {
                        act.setCode(result);
                        logger.error("result: " + result);
                    }

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Message message = Message.obtain(ActivityHandler.this, R.id.decode_finish);
                            message.sendToTarget();
                        }
                    }).start();
                    break;
                case R.id.decode_finish:
                    logger.error("decode_finish");
                    act.setPreviewCallbackToCamera();
                    break;
            }
        }
    }

}
