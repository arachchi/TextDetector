package com.cognitionlab.fingerReader.services.helpers;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;
import com.cognitionlab.fingerReader.services.helpers.adaptors.ProcessingAdaptor;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class ImageProcessingTask extends AsyncTask<Bitmap, String, String> {

    private ContentNotifier contentNotifier;

    private ProcessingAdaptor processingAdaptor;

    public ImageProcessingTask(ProcessingAdaptor processingAdaptor, ContentNotifier contentNotifier) {
        this.contentNotifier = contentNotifier;
        this.processingAdaptor = processingAdaptor;
    }

    @Override
    protected String doInBackground(final Bitmap... bitmap) {

        Bitmap processingBitmap = bitmap[0];
        this.processingAdaptor.getExtractedData(processingBitmap);
        Log.i("TIME", "Text Setting End Time +" + new Date());

        return "Processing";
    }


    @Override
    protected void onPostExecute(String result) {
    }


    @Override
    protected void onPreExecute() {
    }


    @Override
    protected void onProgressUpdate(String... text) {

    }
}