package com.cognitionlab.fingerReader.dtos;

import android.graphics.Bitmap;
import android.graphics.Rect;

import org.opencv.core.Mat;

import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter(AccessLevel.PUBLIC)
@Setter(AccessLevel.PUBLIC)
public class SearchDTO {

    private Bitmap bitmap;

    private String input;

    private boolean caseSensitive;

    private boolean onlyAlphaNumeric;

    private Map<String, List<Rect>> keywordsMap;

}
