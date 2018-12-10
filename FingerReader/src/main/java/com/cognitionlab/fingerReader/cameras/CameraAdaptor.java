package com.cognitionlab.fingerReader.cameras;

import android.hardware.Camera;

public interface CameraAdaptor {

    android.hardware.Camera getCamera();

    Camera selectCamera() throws Exception;

    void releaseCamera();
}
