package com.cognitionlab.fingerReader.services.helpers.adaptors;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;

import java.util.List;
import java.util.Map;

public interface ProcessingAdaptor {

    String getRecognizedText(Bitmap bitmap);

    Map<String, List<Rect>> getRecognizeTextLocationsMap(String recognizedText);

    default void getExtractedData(Bitmap bitmap) {
        System.out.println("Not Available");
    }
}
