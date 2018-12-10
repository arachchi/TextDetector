package com.cognitionlab.fingerReader.cameras;

import android.hardware.Camera;
import android.util.Log;

public class DeviceCameraAdaptor implements CameraAdaptor {

    private Camera mCamera;

    private boolean cameraFront;

    public DeviceCameraAdaptor(Camera mCamera) {
        this.mCamera = mCamera;
    }

    @Override
    public Camera getCamera() {
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                mCamera = Camera.open(findFrontFacingCamera());
            } else {
                mCamera = Camera.open(findBackFacingCamera());
            }
        }

        return mCamera;
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public Camera selectCamera() throws Exception {
        int camerasNumber = Camera.getNumberOfCameras();
        if (camerasNumber > 1) {
            releaseCamera();
            chooseCamera();
            Log.i("CAMERA", "CameraAdaptor Select Successfully.");
            return mCamera;
        } else {
            throw new Exception("Sorry, your phone has only one camera!");
        }
    }

    private void chooseCamera() {
        int cameraId;
        if (cameraFront) {
            cameraId = findBackFacingCamera();
        } else {
            cameraId = findFrontFacingCamera();
        }

        changeCamera(cameraId);
    }

    public void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    private void changeCamera(int cameraId) {
        if (cameraId >= 0) {
            mCamera = Camera.open(cameraId);

            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            params.set("orientation", "portrait");
            mCamera.setParameters(params);
        }
    }
}
