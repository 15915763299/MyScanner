package com.myscanner.utils;

import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;

import com.myscanner.App;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

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

    private static final int TEN_DESIRED_ZOOM = 27;
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");

    public static void setFlash(Camera.Parameters parameters) {
        if (Build.MODEL.contains("Behold II")) { // 3
            // =
            // Cupcake
            parameters.set("flash-value", 1);
        } else {
            parameters.set("flash-value", 2);
        }
        parameters.set("flash-mode", "off");
    }

    public static void setZoom(Camera.Parameters parameters) {

        String zoomSupportedString = parameters.get("zoom-supported");
        if (zoomSupportedString != null
                && !Boolean.parseBoolean(zoomSupportedString)) {
            return;
        }

        int tenDesiredZoom = TEN_DESIRED_ZOOM;

        String maxZoomString = parameters.get("max-zoom");
        if (maxZoomString != null) {
            try {
                int tenMaxZoom = (int) (10.0 * Double
                        .parseDouble(maxZoomString));
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        String takingPictureZoomMaxString = parameters
                .get("taking-picture-zoom-max");
        if (takingPictureZoomMaxString != null) {
            try {
                int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom;
                }
            } catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        String motZoomValuesString = parameters.get("mot-zoom-values");
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(motZoomValuesString,
                    tenDesiredZoom);
        }

        String motZoomStepString = parameters.get("mot-zoom-step");
        if (motZoomStepString != null) {
            try {
                double motZoomStep = Double.parseDouble(motZoomStepString
                        .trim());
                int tenZoomStep = (int) (10.0 * motZoomStep);
                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
                }
            } catch (NumberFormatException nfe) {
                // continue
            }
        }

        // Set zoom. This helps encourage the user to pull back.
        // Some devices like the Behold have a zoom parameter
        if (maxZoomString != null || motZoomValuesString != null) {
            parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
        }

        // Most devices, like the Hero, appear to expose this zoom parameter.
        // It takes on values like "27" which appears to mean 2.7x zoom
        if (takingPictureZoomMaxString != null) {
            parameters.set("taking-picture-zoom", tenDesiredZoom);
        }
    }

    private static int findBestMotZoomValue(CharSequence stringValues,
                                            int tenDesiredZoom) {
        int tenBestValue = 0;
        for (String stringValue : COMMA_PATTERN.split(stringValues)) {
            stringValue = stringValue.trim();
            double value;
            try {
                value = Double.parseDouble(stringValue);
            } catch (NumberFormatException nfe) {
                return tenDesiredZoom;
            }
            int tenValue = (int) (10.0 * value);
            if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom
                    - tenBestValue)) {
                tenBestValue = tenValue;
            }
        }
        return tenBestValue;
    }

    public static boolean isSupportedAntibanding(Camera.Parameters parameters, String temp) {
        List<String> list = parameters.getSupportedAntibanding();
        if (list != null) {
            for (String str : list) {
                if (temp.equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }
}
