package com.cognitionlab.fingerReader.services.helpers;

import android.content.Context;
import android.content.res.AssetManager;

import com.cognitionlab.fingerReader.TessOCR;

public class TessaractAdaptor extends ProcessingAdaptor {

    private TessOCR mTessOCR;

    public TessaractAdaptor(Context context, AssetManager assetManager) {
        this.mTessOCR = new TessOCR(context, assetManager);
    }

    @Override
    TessOCR getTessaract() {
        return this.mTessOCR;
    }
}
