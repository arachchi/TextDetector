package com.cognitionlab.fingerReader.services.impl;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.ImageProcessingTask;
import com.cognitionlab.fingerReader.services.helpers.observers.KeywordMapObserver;
import com.cognitionlab.fingerReader.services.helpers.callbacks.OpenCVLoaderCallback;
import com.cognitionlab.fingerReader.services.helpers.adaptors.ProcessingAdaptor;

import org.opencv.android.BaseLoaderCallback;

import java.util.Observer;

public class ProcessingServiceImpl implements ProcessingService {

    private CameraService cameraService;
    private SearchService searchService;
    private SpeechService speechService;
    private ProcessingAdaptor processingAdaptor;
    private BaseLoaderCallback mLoaderCallback;
    private ContentNotifier contentNotifier;
    private KeywordMapObserver keywordMapObserver;

    public ProcessingServiceImpl(ContentNotifier contentNotifier,
                                 ProcessingAdaptor processingAdaptor,
                                 OpenCVLoaderCallback openCVLoaderCallback,
                                 KeywordMapObserver keywordMapObserver,
                                 CameraService cameraService,
                                 SearchService searchService,
                                 SpeechService speechService) {

        this.cameraService = cameraService;
        this.searchService = searchService;
        this.speechService = speechService;
        this.contentNotifier = contentNotifier;
        this.processingAdaptor = processingAdaptor;
        this.mLoaderCallback = openCVLoaderCallback;
        this.keywordMapObserver = keywordMapObserver;

        this.addProcessingContentObserver(this.keywordMapObserver);
    }

    public void setTessOCR(Context context, AssetManager assetManager) {

    }

    @Override
    public BaseLoaderCallback getLoaderCallbackForOpenCV() {
        return mLoaderCallback;
    }

    @Override
    public CameraPreview getCameraPreview() {
        return cameraService.getCameraPreview();
    }

    @Override
    public Camera selectCamera() throws Exception {
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
        ImageProcessingTask t = new ImageProcessingTask(processingAdaptor, contentNotifier);
        t.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    @Override
    public void addProcessingContentObserver(Observer observer) {
        contentNotifier.addObserver(observer);
    }

    @Override
    public Bitmap searchText(SearchDTO searchDTO) {
        searchDTO.setKeywordsMap(this.keywordMapObserver.getKeywordsMap());
        return searchService.searchText(searchDTO);
    }
}
