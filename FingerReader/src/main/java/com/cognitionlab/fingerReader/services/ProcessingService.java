package com.cognitionlab.fingerReader.services;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Camera;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.dtos.SearchDTO;

import org.opencv.android.BaseLoaderCallback;

import java.util.Observer;

public interface ProcessingService {

    CameraPreview getCameraPreview();

    Camera selectCamera() throws Exception;

    void releaseCamera();

    Camera getCamera();

    Bitmap getDisplayImage(byte[] data);

    void fullTextRecognition(Bitmap bitmap);

    void setTessOCR(Context context, AssetManager assetManager);

    BaseLoaderCallback getLoaderCallbackForOpenCV();

    Bitmap searchText(SearchDTO searchDTO);

    void addProcessingContentObserver(Observer observer);
}
