package com.cognitionlab.fingerReader.services.helpers;

import android.graphics.Rect;
import android.util.Log;

import com.cognitionlab.fingerReader.TessOCR;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import lombok.AccessLevel;
import lombok.Getter;

public class KeywordMapObserver implements Observer {

    @Getter(AccessLevel.PUBLIC)
    private Map<String, List<Rect>> keywordsMap;

    private TessOCR mTessOCR;

    public KeywordMapObserver(TessOCR mTessOCR) {
        this.mTessOCR = mTessOCR;
    }

    @Override
    public void update(Observable o, Object arg) {
        ContentNotifier contentNotifier = (ContentNotifier) o;

        Log.i("TIME", "Keyword Map Creation Started Time +" + new Date());
        this.keywordsMap = contentNotifier.getDataExtractionDTO().getKeywordsMap();
        Log.i("TIME", "Keyword Map Creation End Time +" + new Date());
    }
}
