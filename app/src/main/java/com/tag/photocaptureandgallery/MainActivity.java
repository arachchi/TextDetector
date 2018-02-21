package com.tag.photocaptureandgallery;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.takeimage.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private int SELECT_FILE = 1;
    private Button btnSearch;

    private ImageView ivImage;
    private TextView textView;
    private CheckBox caseSensitive;
    private CheckBox onlyAlphaNumeric;

    private PopupWindow mPopupWindow;

    private String userChoosenTask;
    private TessOCR mTessOCR;

    private Mat mIntermediateMat;
    private Scalar CONTOUR_COLOR_HIGHLIGHT = new Scalar(255, 0, 0, 255);
    private Bitmap bitmap;
    private Map<String, List<android.graphics.Rect>> keywordsMap;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");

                    mIntermediateMat = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnSelect = findViewById(R.id.btnSelectPhoto);
        btnSelect.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        btnSearch = findViewById(R.id.btnSeachText);

        btnSearch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                EditText editText = findViewById(R.id.searchText);
                String searchText = editText.getText().toString();

                searchText(searchText);
            }
        });

        ivImage = findViewById(R.id.ivImage);
        textView = findViewById(R.id.txtView);
        caseSensitive = findViewById(R.id.caseSensitive);
        onlyAlphaNumeric = findViewById(R.id.onlyAlphaNumeric);

        AssetManager assetManager = getAssets();
        mTessOCR = new TessOCR(MainActivity.this, assetManager);

        keywordsMap = new HashMap<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Utility.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (userChoosenTask.equals(PhotoSelection.LIB.description))
                        galleryIntent();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), R.string.provide_photo_permission, Toast.LENGTH_LONG);
                    toast.show();
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = PhotoSelection.getValues();

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.add_photo_title);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(MainActivity.this);

                if (items[item].equals(PhotoSelection.LIB.description)) {
                    userChoosenTask = PhotoSelection.LIB.description;
                    if (result) {
                        galleryIntent();
                    }

                } else if (items[item].equals(PhotoSelection.CNX.description)) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();


    }

    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpg", "image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        String title = getResources().getString(R.string.select_file_title);

        startActivityForResult(Intent.createChooser(intent, title), SELECT_FILE);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final Bitmap image;

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                image = onSelectFromGalleryResult(data);
            } else {
                textView.setText(R.string.image_no_text);
                image = null;
            }
            bitmap = image;
        } else {
            image = null;
        }

        if (image != null) {
            performFullTextRecognition(image);
        }
    }

    @Override
    public void onBackPressed() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    private Bitmap onSelectFromGalleryResult(Intent data) {

        Bitmap bm = null;
        if (data != null) {
            try {
                bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ivImage.setImageBitmap(bm);

        return bm;
    }

    private void performFullTextRecognition(final Bitmap bitmap) {
        String srcText = mTessOCR.getResults(bitmap);

        if (srcText.isEmpty()) {
            srcText = getResources().getString(R.string.edit_text_hint);
        } else {
            keywordsMap = mTessOCR.getKeywordMap(bitmap);
        }

        textView.setText(srcText);
    }

    private void searchText(String input) {
        List<android.graphics.Rect> searchValue = new ArrayList<>();
        String text = this.criteriaBoundText(input);

        for (String key : keywordsMap.keySet()) {
            String formattedKey = this.criteriaBoundText(key);
            if (text.equals(formattedKey)) {//checking all the values to meet the criteria
                searchValue.addAll(keywordsMap.get(key));
            }
        }

        if (searchValue.isEmpty()) {
            String notFoundText = getResources().getString(R.string.search_no_result);
            if (bitmap == null) {
                notFoundText = getResources().getString(R.string.edit_text_hint);
            }

            Toast toast = Toast.makeText(getApplicationContext(), notFoundText, Toast.LENGTH_LONG);
            toast.show();
        } else {
            this.localizeSearchResult(searchValue);
        }
    }

    private String criteriaBoundText(String input) {
        String result = input.trim();
        if (!caseSensitive.isChecked()) {
            result = result.toLowerCase();
        }

        if (onlyAlphaNumeric.isChecked()) {
            result = result.replaceAll("[^a-zA-Z ]", " ").trim();
        }

        return result;
    }

    private void localizeSearchResult(List<android.graphics.Rect> resultList) {
        Utils.bitmapToMat(bitmap, mIntermediateMat);
        final int lineThickness = 2;

        for (android.graphics.Rect sample : resultList) {
            Point pointA = new Point(sample.left, sample.top), pointB = new Point(sample.right, sample.bottom);
            Imgproc.rectangle(mIntermediateMat, pointA, pointB, CONTOUR_COLOR_HIGHLIGHT, lineThickness);
        }

        Bitmap processedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Utils.matToBitmap(mIntermediateMat, processedBitmap);

        this.displaySearchResult(processedBitmap);

    }

    private void displaySearchResult(Bitmap bitmap) {
        Context mContext = MainActivity.this;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);

        LinearLayout linearLayout = findViewById(R.id.LinearLayout1);

        assert inflater != null;
        View customView = inflater.inflate(R.layout.popup_image, null);

        mPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setFocusable(true);

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21) {
            mPopupWindow.setElevation(5.0f);
        }

        ImageButton closeButton = customView.findViewById(R.id.ib_close);
        ImageView imageView = customView.findViewById(R.id.popupView);
        imageView.setImageBitmap(bitmap);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPopupWindow.dismiss();
            }
        });

        mPopupWindow.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);

    }
}

