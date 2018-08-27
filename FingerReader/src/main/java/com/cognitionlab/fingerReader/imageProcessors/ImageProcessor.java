package com.cognitionlab.fingerReader.imageProcessors;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;

import java.util.List;
import java.util.Map;

public interface ImageProcessor {

    String getResults(Bitmap bitmap);

    Map<String, List<Rect>> getKeywordMap(String result);

    default void getExtractedData(Bitmap bitmap) {
        System.out.println("Not Available");
    }
}
