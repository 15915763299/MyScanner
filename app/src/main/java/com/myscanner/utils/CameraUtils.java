package com.myscanner.utils;

import android.content.pm.PackageManager;
import android.hardware.Camera;

import com.myscanner.App;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 照相机相关工具
 */
public class CameraUtils {

    /**
     * Check if this device has bg_talk_big_lower_half camera
     */
    public static boolean checkCameraHardware() {
        return App.getApp().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    /**
     * 查找指定位置摄像头
     */
    public static int findCamera(int position) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();

        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == position) {
                return camIdx;
            }
        }
        return -1;
    }

    public static Camera.Size calBestPreviewSize(Camera.Parameters camPara, float targetRatio) {
        List<Camera.Size> allSupportedSize = camPara.getSupportedPreviewSizes();
        //从大到小排序
        Collections.sort(allSupportedSize, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                return o2.height - o1.height;
            }
        });

        Camera.Size bestSize = null;
        float dValue = Float.MAX_VALUE;//差值

        float mDValue, mRatio;
        for (Camera.Size tmpSize : allSupportedSize) {
            mRatio = (float) tmpSize.height / (float) tmpSize.width;
            mDValue = Math.abs(mRatio - targetRatio);

            if (mDValue < dValue) {//相等不替换，选择前面的配对
                dValue = mDValue;
                bestSize = tmpSize;
            }
        }

        if (bestSize == null) {
            bestSize = allSupportedSize.get(0);
            bestSize.width = 640;
            bestSize.height = 480;
        }
        return bestSize;
    }
}
