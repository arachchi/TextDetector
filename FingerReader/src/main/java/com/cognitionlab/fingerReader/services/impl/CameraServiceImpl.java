package com.cognitionlab.fingerReader.services.impl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.util.Log;
import android.widget.Toast;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.services.CameraService;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CameraServiceImpl implements CameraService {

    private Camera mCamera;

    private Mat mIntermediateMat;

    private boolean cameraFront;

    public CameraPreview getCameraPreview(Context context) {
        return new CameraPreview(context, mCamera);
    }

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
            Log.i("CAMERA", "Camera Select Successfully.");
            return mCamera;
        } else {
            throw new Exception("Sorry, your phone has only one camera!");
        }
    }

    public void chooseCamera() {
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

    public Bitmap getDisplayImage(byte[] data) {

        Bitmap image, bm;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        image = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        Matrix matrix = new Matrix();
        matrix.postRotate(90);

        bm = Bitmap.createBitmap(image, 0, 0, image.getWidth(), image.getHeight(), matrix, true);

        bm = toGrayScale(bm);
        bm = toReducedSize(bm);

        return bm;
    }

    private Bitmap toGrayScale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private Bitmap toReducedSize(Bitmap bitmap) {
        mIntermediateMat = new Mat();
        Utils.bitmapToMat(bitmap, mIntermediateMat);
        Rect rect = new Rect(bitmap.getWidth() / 6, bitmap.getHeight() / 3,
                (bitmap.getWidth() * 2) / 3, ((bitmap.getHeight() * 1) / 3));
        mIntermediateMat = new Mat(mIntermediateMat, rect);
        Bitmap processedBitmap = Bitmap.createBitmap(rect.width, rect.height, bitmap.getConfig());
        Utils.matToBitmap(mIntermediateMat, processedBitmap);

        mIntermediateMat = null;
        return processedBitmap;
    }

    @Override
    public void initiateMatrices() {
        mIntermediateMat = new Mat();
    }
}
