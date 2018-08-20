package com.cognitionlab.fingerReader.dtos;

import android.graphics.Bitmap;
import android.graphics.Rect;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class DataExtractionDTO {

    private String content;

    private Bitmap image;

    private Map<String, List<Rect>> keywordsMap;
}
