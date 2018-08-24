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
        DataExtractionDTO dataExtractionDTO = new DataExtractionDTO();
        Bitmap processingBitmap = bitmap[0];
        Log.i("TIME", "Text Extraction Started Time +" + new Date());
        final String srcText = processingAdaptor.getRecognizedText(processingBitmap);
        Log.i("TIME", "Text Extraction End Time +" + new Date());

        Log.i("TIME", "Keyword Map Creation Started Time +" + new Date());
        Map<String, List<Rect>> keywordsMap = processingAdaptor.getRecognizeTextLocationsMap(srcText);
        Log.i("TIME", "Keyword Map Creation End Time +" + new Date());

        Log.i("TIME", "Text Setting Started Time +" + new Date());
        if (srcText.isEmpty()) {
            dataExtractionDTO.setContent("No Text Found");
        } else {
            dataExtractionDTO.setContent(srcText);
        }

        dataExtractionDTO.setImage(processingBitmap);
        dataExtractionDTO.setKeywordsMap(keywordsMap);
        contentNotifier.setDataExtractionDTO(dataExtractionDTO);
        Log.i("TIME", "Text Setting End Time +" + new Date());

        return srcText;
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