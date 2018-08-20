package com.cognitionlab.fingerReader.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Camera;

import com.cognitionlab.fingerReader.CameraPreview;

public interface CameraService {

    CameraPreview getCameraPreview(Context context);

    Camera selectCamera() throws Exception;

    void releaseCamera();

    Camera getCamera();

    Bitmap getDisplayImage(byte[] data);

    void initiateMatrices();
}
