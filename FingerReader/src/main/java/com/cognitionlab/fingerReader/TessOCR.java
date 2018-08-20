package com.cognitionlab.fingerReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TessOCR {
    private static final String lang = "eng";
    private static final String TAG = "TESSERACT";

    private TessBaseAPI mTess;

    public TessOCR(Context context, AssetManager assetManager) {
        final String DATA_PATH = context.getFilesDir() + File.separator + "AndroidOCR" + File.separator;
        final String FOLDER = "tessdata";
        final String FILE_PATH = FOLDER + File.separator + lang + ".traineddata";
        final String FULL_PATH = DATA_PATH + FILE_PATH;

        String[] paths = new String[]{DATA_PATH, DATA_PATH + FOLDER + File.separator};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }

        if (!(new File(FULL_PATH)).exists()) {
            try {
                InputStream in = assetManager.open(FILE_PATH);
                OutputStream out = new FileOutputStream(new File(FULL_PATH));

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

        mTess = new TessBaseAPI();
        mTess.setDebug(true);
        mTess.init(DATA_PATH, lang);

        Log.i("Completed", "Initialization");

    }

    public String getResults(Bitmap bitmap) {
        mTess.setImage(bitmap);
        return mTess.getUTF8Text();
    }

    public Map<String, List<Rect>> getKeywordMap(String result) {
        Map<String, List<Rect>> wordMap = new HashMap<>();
        String[] splitedString = result.split("\\s+");
        List<Rect> rectList = mTess.getWords().getBoxRects();

        boolean concatenated = false;
        String concatenatedString = "";//String ends with - in the previous line to continue until next
        Rect concatenatedStringRect = null;

        int i = 0;
        for (Rect sample : rectList) {
            if (i < splitedString.length) {
                String key = splitedString[i];

                if (key.endsWith("-")) {
                    concatenated = true;
                    concatenatedStringRect = sample;
                    concatenatedString = key;
                    i++;
                    continue;
                }

                List<Rect> list = wordMap.get(key);
                if (list == null) {
                    list = new ArrayList<>();
                }

                if (concatenated) {
                    key = concatenatedString.replace("-", "").concat(key);
                    list.add(concatenatedStringRect);
                    concatenated = false;
                }

                list.add(sample);
                wordMap.put(key, list);

            } else {
                Log.i("TAG", "ERROR in processing");
            }

            i++;
        }

        return wordMap;
    }
}