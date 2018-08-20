package com.cognitionlab.fingerReader.services.impl;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.SearchService;

import org.opencv.android.Utils;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class SearchServiceImpl implements SearchService {

    public Bitmap searchText(SearchDTO searchDTO) {
        String input = searchDTO.getInput();
        boolean caseSensitive = searchDTO.isCaseSensitive();
        boolean onlyAlphaNumeric = searchDTO.isOnlyAlphaNumeric();

        List<Rect> searchValue = new ArrayList<>();
        String text = this.criteriaBoundText(input, caseSensitive, onlyAlphaNumeric);

        for (String key : searchDTO.getKeywordsMap().keySet()) {
            String formattedKey = this.criteriaBoundText(key, caseSensitive, onlyAlphaNumeric);
            if (text.equals(formattedKey)) {//checking all the values to meet the criteria
                searchValue.addAll(searchDTO.getKeywordsMap().get(key));
            }
        }

        return localizeSearchResult(searchDTO, searchValue);
    }

    private String criteriaBoundText(String input, boolean isCaseSensitive, boolean isOnlyAlphaNumeric) {
        String result = input.trim();
        if (!isCaseSensitive) {
            result = result.toLowerCase();
        }

        if (isOnlyAlphaNumeric) {
            result = result.replaceAll("[^a-zA-Z ]", " ").trim();
        }

        return result;
    }

    private Bitmap localizeSearchResult(SearchDTO searchDTO, List<android.graphics.Rect> resultList) {
        Bitmap bitmap = searchDTO.getBitmap();
        Utils.bitmapToMat(bitmap, searchDTO.getMIntermediateMat());
        final int lineThickness = 10;
        Scalar CONTOUR_COLOR_HIGHLIGHT = new Scalar(255, 0, 0, 255);

        for (android.graphics.Rect sample : resultList) {
            Point pointA = new Point(sample.left, sample.top), pointB = new Point(sample.right, sample.bottom);
            Imgproc.rectangle(searchDTO.getMIntermediateMat(), pointA, pointB, CONTOUR_COLOR_HIGHLIGHT, lineThickness);
        }

        Bitmap processedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Utils.matToBitmap(searchDTO.getMIntermediateMat(), processedBitmap);

        return processedBitmap;
    }
}
