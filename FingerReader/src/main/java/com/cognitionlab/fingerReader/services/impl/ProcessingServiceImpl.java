package com.cognitionlab.fingerReader.services.impl;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.util.Log;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.TessOCR;
import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.ExtractContentService;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.helpers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.ImageProcessingTask;
import com.cognitionlab.fingerReader.services.helpers.KeywordMapObserver;
import com.cognitionlab.fingerReader.services.modules.camera.DaggerCameraServiceComponent;
import com.cognitionlab.fingerReader.services.modules.extractContent.DaggerExtractContentServiceComponent;
import com.cognitionlab.fingerReader.services.modules.search.DaggerSearchServiceComponent;
import com.cognitionlab.fingerReader.services.modules.speech.DaggerSpeechServiceComponent;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.core.Mat;

import java.util.Observer;

import javax.inject.Inject;

public class ProcessingServiceImpl implements ProcessingService {

    @Inject
    CameraService cameraService;

    @Inject
    ExtractContentService extractContentService;

    @Inject
    SearchService searchService;

    @Inject
    SpeechService speechService;

    private Mat mIntermediateMat;

    private TessOCR mTessOCR;

    private BaseLoaderCallback mLoaderCallback;

    private ContentNotifier contentNotifier;

    private KeywordMapObserver keywordMapObserver;

    public ProcessingServiceImpl() {
        cameraService = DaggerCameraServiceComponent.builder().build().provideCameraServiceModule();
        extractContentService = DaggerExtractContentServiceComponent.builder().build().provideExtractContentService();
        searchService = DaggerSearchServiceComponent.builder().build().provideSearchService();
        speechService = DaggerSpeechServiceComponent.builder().build().provideSpeechService();

        contentNotifier = new ContentNotifier();
    }

    public void setTessOCR(Context context, AssetManager assetManager) {
        mTessOCR = new TessOCR(context, assetManager);
        mLoaderCallback = new BaseLoaderCallback(context) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        Log.i("OpenCV", "OpenCV loaded successfully");

                        mIntermediateMat = new Mat();
                        cameraService.initiateMatrices();
                    }
                    break;
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }
        };

        if (keywordMapObserver == null) {
            keywordMapObserver = new KeywordMapObserver(this.mTessOCR);
            this.addProcessingContentObserver(keywordMapObserver);
        }
    }

    @Override
    public BaseLoaderCallback getLoaderCallbackForOpenCV() {
        return mLoaderCallback;
    }

    @Override
    public CameraPreview getCameraPreview(Context context) {
        return cameraService.getCameraPreview(context);
    }

    @Override
    public Camera selectCamera() throws Exception{
        return cameraService.selectCamera();
    }

    @Override
    public void releaseCamera() {
        cameraService.releaseCamera();
    }

    @Override
    public Camera getCamera() {
        return cameraService.getCamera();
    }

    @Override
    public Bitmap getDisplayImage(byte[] data) {
        return cameraService.getDisplayImage(data);
    }

    @Override
    public void fullTextRecognition(Bitmap bitmap) {
        ImageProcessingTask t = new ImageProcessingTask(mTessOCR, contentNotifier);
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    @Override
    public void addProcessingContentObserver(Observer observer) {
        contentNotifier.addObserver(observer);
    }

    @Override
    public Bitmap searchText(SearchDTO searchDTO) {
        searchDTO.setMIntermediateMat(this.mIntermediateMat);
        searchDTO.setKeywordsMap(this.keywordMapObserver.getKeywordsMap());
        return searchService.searchText(searchDTO);
    }
}
