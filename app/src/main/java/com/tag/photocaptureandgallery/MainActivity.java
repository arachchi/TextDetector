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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.takeimage.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

    private int REQUEST_CAMERA = 0, SELECT_FILE = 1;
    private Button btnSearch;

    private ImageView ivImage;
    private TextView textView;

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

        AssetManager assetManager = getAssets();
        mTessOCR = new TessOCR(MainActivity.this, assetManager);
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
                    if (userChoosenTask.equals("Take Photo"))
                        cameraIntent();
                    else if (userChoosenTask.equals("Choose from Library"))
                        galleryIntent();
                } else {
                    //code for deny
                }
                break;
        }
    }

    private void selectImage() {
        final CharSequence[] items = {"Take Photo", "Choose from Library",
                "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                boolean result = Utility.checkPermission(MainActivity.this);

                if (items[item].equals("Take Photo")) {
                    userChoosenTask = "Take Photo";
                    if (result) {
                        cameraIntent();
                    }
                } else if (items[item].equals("Choose from Library")) {
                    userChoosenTask = "Choose from Library";
                    if (result) {
                        galleryIntent();
                    }

                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();


    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), SELECT_FILE);
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final Bitmap image;

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SELECT_FILE) {
                image = onSelectFromGalleryResult(data);
            } else if (requestCode == REQUEST_CAMERA) {
                image = onCaptureImageResult(data);
            } else {
                image = null;
            }
            bitmap = image;
        } else {
            image = null;
        }

        if (image != null) {
            doOCR(image);
        }
    }

    private Bitmap onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);

        StringBuilder fileName = new StringBuilder("img");
        fileName.append(System.currentTimeMillis()).append(".jpg");

        File destination = new File(Environment.getExternalStorageDirectory(), fileName.toString());

        FileOutputStream fo;
        try {
            destination.createNewFile();
            fo = new FileOutputStream(destination);
            fo.write(bytes.toByteArray());
            fo.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ivImage.setImageBitmap(thumbnail);

        return thumbnail;
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

    private void doOCR(final Bitmap bitmap) {
        final String srcText = mTessOCR.getResults(bitmap);
        keywordsMap = mTessOCR.getKeywordMap(bitmap);
        textView.setText(srcText);
    }

    private void searchText(String input) {
        List<android.graphics.Rect> searchValue = new ArrayList<>();
        String text = input.replaceAll("[^a-zA-Z ]", " ").trim();

        for (String key : keywordsMap.keySet()) {
            String formattedKey = key.replaceAll("[^a-zA-Z ]", " ").trim();
            if (text.equals(formattedKey)) {
                searchValue.addAll(keywordsMap.get(key));
            }
        }

        this.displayWord(searchValue);
    }

    private void displayWord(List<android.graphics.Rect> resultList) {
        Utils.bitmapToMat(bitmap, mIntermediateMat);

        for (android.graphics.Rect sample : resultList) {
            Log.d("RECT", sample.left + " , " + sample.top + " , " + " , " + sample.right + " , " + sample.bottom);
            Imgproc.rectangle(
                    mIntermediateMat,                             //Matrix obj of the image
                    new Point(sample.left, sample.top),       //p1
                    new Point(sample.right, sample.bottom),        //p2
                    CONTOUR_COLOR_HIGHLIGHT,                    //Scalar object for color
                    1                                   //Thickness of the line
            );
        }

        Bitmap processedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Utils.matToBitmap(mIntermediateMat, processedBitmap);

        this.showImage(processedBitmap);

    }

    private void showImage(Bitmap bitmap) {
        Context mContext = MainActivity.this;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        final PopupWindow mPopupWindow;
        LinearLayout linearLayout = findViewById(R.id.LinearLayout1);

        // Inflate the custom layout/view
        assert inflater != null;
        View customView = inflater.inflate(R.layout.popup_image, null);

        // Initialize a new instance of popup window
        mPopupWindow = new PopupWindow(
                customView,
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );

        // Set an elevation value for popup window
        // Call requires API level 21
        if (Build.VERSION.SDK_INT >= 21) {
            mPopupWindow.setElevation(5.0f);
        }

        // Get a reference for the custom view close button
        ImageButton closeButton = customView.findViewById(R.id.ib_close);
        ImageView imageView = customView.findViewById(R.id.popupView);
        imageView.setImageBitmap(bitmap);

        // Set a click listener for the popup window close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                mPopupWindow.dismiss();
            }
        });

        // Finally, show the popup window at the center location of root linear layout
        mPopupWindow.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);

    }
}

