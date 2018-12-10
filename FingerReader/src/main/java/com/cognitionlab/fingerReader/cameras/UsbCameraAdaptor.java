package com.cognitionlab.fingerReader.cameras;

import android.hardware.Camera;

public class UsbCameraAdaptor implements CameraAdaptor {

    @Override
    public android.hardware.Camera getCamera() {
        return null;
    }

    @Override
    public Camera selectCamera() throws Exception {
        return null;
    }

    @Override
    public void releaseCamera() {

    }
}
