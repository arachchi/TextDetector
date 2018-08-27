package com.cognitionlab.fingerReader.services.helpers.adaptors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.cognitionlab.fingerReader.imageProcessors.TessOCR;

import java.util.List;
import java.util.Map;

public class TessaractAdaptor implements ProcessingAdaptor {

    private TessOCR mTessOCR;

    public TessaractAdaptor(Context context) {
        this.mTessOCR = new TessOCR(context, context.getAssets());
    }

    @Override
    public String getRecognizedText(Bitmap bitmap) {
        return mTessOCR.getResults(bitmap);
    }

    @Override
    public Map<String, List<Rect>> getRecognizeTextLocationsMap(String recognizedText) {
        return mTessOCR.getKeywordMap(recognizedText);
    }
}
