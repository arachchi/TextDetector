package com.cognitionlab.fingerReader.services.helpers.adaptors;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;
import java.util.Map;

public interface ProcessingAdaptor {

    String getRecognizedText(Bitmap bitmap);

    Map<String, List<Rect>> getRecognizeTextLocationsMap(String recognizedText);
}
