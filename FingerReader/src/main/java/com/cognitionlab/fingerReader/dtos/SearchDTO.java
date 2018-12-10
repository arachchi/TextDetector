package com.cognitionlab.fingerReader.dtos;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;
import java.util.Map;

public class SearchDTO {

    private Bitmap bitmap;

    private String input;

    private boolean caseSensitive;

    private boolean onlyAlphaNumeric;

    private Map<String, List<Rect>> keywordsMap;

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean isOnlyAlphaNumeric() {
        return onlyAlphaNumeric;
    }

    public void setOnlyAlphaNumeric(boolean onlyAlphaNumeric) {
        this.onlyAlphaNumeric = onlyAlphaNumeric;
    }

    public Map<String, List<Rect>> getKeywordsMap() {
        return keywordsMap;
    }

    public void setKeywordsMap(Map<String, List<Rect>> keywordsMap) {
        this.keywordsMap = keywordsMap;
    }

    @Override
    public String toString() {
        return "SearchDTO{" +
                "bitmap=" + bitmap +
                ", input='" + input + '\'' +
                ", caseSensitive=" + caseSensitive +
                ", onlyAlphaNumeric=" + onlyAlphaNumeric +
                ", keywordsMap=" + keywordsMap +
                '}';
    }
}
