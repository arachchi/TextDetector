package com.cognitionlab.fingerReader.services.helpers;

import android.content.Context;
import android.util.Log;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

public class OpenCVLoaderCallback extends BaseLoaderCallback {

    public OpenCVLoaderCallback(Context AppContext) {
        super(AppContext);
    }

    @Override
    public void onManagerConnected(int status) {
        switch (status) {
            case LoaderCallbackInterface.SUCCESS: {
                Log.i("OpenCV", "OpenCV loaded successfully");
            }
            break;
            default: {
                super.onManagerConnected(status);
            }
            break;
        }
    }
}
