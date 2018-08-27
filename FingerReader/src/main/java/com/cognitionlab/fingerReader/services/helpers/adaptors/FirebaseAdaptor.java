package com.cognitionlab.fingerReader.services.helpers.adaptors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;
import com.cognitionlab.fingerReader.imageProcessors.FirebaseProcessor;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirebaseAdaptor implements ProcessingAdaptor {

    private FirebaseProcessor firebaseProcessor;

    public FirebaseAdaptor(Context context, ContentNotifier contentNotifier) {
        this.firebaseProcessor = new FirebaseProcessor(context, contentNotifier);
    }

    @Override
    public String getRecognizedText(Bitmap bitmap) {
        String results = this.firebaseProcessor.getResults(bitmap);
        return results;
    }

    @Override
    public Map<String, List<Rect>> getRecognizeTextLocationsMap(String recognizedText) {
        return new HashMap<>();
    }

    @Override
    public void getExtractedData(Bitmap bitmap) {
        this.firebaseProcessor.getExtractedData(bitmap);
    }
}
