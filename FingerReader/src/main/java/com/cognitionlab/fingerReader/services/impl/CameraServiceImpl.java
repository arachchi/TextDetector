package com.cognitionlab.fingerReader.services.impl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.cameras.CameraAdaptor;
import com.cognitionlab.fingerReader.cameras.DeviceCameraAdaptor;
import com.cognitionlab.fingerReader.services.CameraService;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class CameraServiceImpl implements CameraService {

    private Mat mIntermediateMat;

    private CameraPreview cameraPreview;

    private CameraAdaptor adaptor;

    public CameraServiceImpl(CameraPreview cameraPreview, Camera camera) {
        this.cameraPreview = cameraPreview;
        this.adaptor = new DeviceCameraAdaptor(camera);
    }

    public CameraPreview getCameraPreview() {
        return this.cameraPreview;
    }

    public Camera getCamera() {
        return adaptor.getCamera();
    }

    public Camera selectCamera() throws Exception {
        return adaptor.selectCamera();
    }

    public void releaseCamera() {
        adaptor.releaseCamera();
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
