package com.cognitionlab.fingerReader.dtos;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;
import java.util.Map;

public class DataExtractionDTO {

    private String content;

    private Bitmap image;

    private Map<String, List<Rect>> keywordsMap;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public Map<String, List<Rect>> getKeywordsMap() {
        return keywordsMap;
    }

    public void setKeywordsMap(Map<String, List<Rect>> keywordsMap) {
        this.keywordsMap = keywordsMap;
    }

    @Override
    public String toString() {
        return "DataExtractionDTO{" +
                "content='" + content + '\'' +
                ", image=" + image +
                ", keywordsMap=" + keywordsMap +
                '}';
    }
}
