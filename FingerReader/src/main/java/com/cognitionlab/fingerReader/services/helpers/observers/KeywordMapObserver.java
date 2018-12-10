package com.cognitionlab.fingerReader.services.helpers.observers;

import android.graphics.Rect;
import android.util.Log;

import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class KeywordMapObserver implements Observer {

    private Map<String, List<Rect>> keywordsMap;

    @Override
    public void update(Observable o, Object arg) {
        ContentNotifier contentNotifier = (ContentNotifier) o;

        Log.i("TIME", "Keyword Map Creation Started Time +" + new Date());
        this.keywordsMap = contentNotifier.getDataExtractionDTO().getKeywordsMap();
        Log.i("TIME", "Keyword Map Creation End Time +" + new Date());
    }

    public Map<String, List<Rect>> getKeywordsMap() {
        return keywordsMap;
    }

    @Override
    public String toString() {
        return "KeywordMapObserver{" +
                "keywordsMap=" + keywordsMap +
                '}';
    }
}
