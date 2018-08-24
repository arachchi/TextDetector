package com.cognitionlab.fingerReader.services.helpers.observers;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter(AccessLevel.PUBLIC)
public class ContentObserver implements Observer {

    private TextView displayView;

    private Bitmap bitmap;

    private Handler handler;

    public ContentObserver(Bitmap bitmap, TextView displayView) {
        this.displayView = displayView;
        this.bitmap = bitmap;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void update(Observable o, Object arg) {
        ContentNotifier contentNotifier = (ContentNotifier) o;
        this.handler.post(() -> {
            this.displayView.setText(contentNotifier.getDataExtractionDTO().getContent());
            this.bitmap = contentNotifier.getDataExtractionDTO().getImage();
        });
    }
}
