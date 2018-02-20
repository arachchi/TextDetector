package com.tag.photocaptureandgallery;

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

class TessOCR {
    private static final String lang = "eng";

    private static final String TAG = "TESSERACT";

    private TessBaseAPI mTess;

    TessOCR(Context context, AssetManager assetManager) {
        String DATA_PATH = context.getFilesDir() + "/AndroidOCR/";
        Log.i(TAG, DATA_PATH);

        String[] paths = new String[]{DATA_PATH, DATA_PATH + "tessdata/"};

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

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(new File(DATA_PATH + "tessdata/", lang + ".traineddata"));

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

    String getResults(Bitmap bitmap) {
        mTess.setImage(bitmap);

        return mTess.getUTF8Text();
    }

    Map<String, List<Rect>> getKeywordMap(Bitmap bitmap) {
        Map<String, List<Rect>> wordMap = new HashMap<>();
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        String[] splited = result.split("\\s+");
        List<Rect> rectList = mTess.getWords().getBoxRects();

        int i = 0;
        for (Rect sample : rectList) {
            if (i < splited.length) {
                String key = splited[i];
                List<Rect> list = wordMap.get(key);
                if (list == null) {
                    list = new ArrayList<>();
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