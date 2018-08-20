package com.cognitionlab.fingerReader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
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

import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.helpers.ContentObserver;
import com.cognitionlab.fingerReader.services.modules.processing.DaggerProcessingServiceComponent;
import com.example.detectText.R;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import butterknife.OnClick;

public class MainActivity extends Activity {

    private int SELECT_FILE = 1;
    private Button btnSearch;

    private ImageView ivImage;

    private TextView textView;
    private CheckBox caseSensitive;
    private CheckBox onlyAlphaNumeric;

    private PopupWindow mPopupWindow;

    private String userChoosenTask;

    private Bitmap bitmap;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;

    private Button buttonCapture, buttonChangeCamera;
    private Context myContext;

    private LinearLayout cameraPreview;
    private ContentObserver contentObserver;

    @Inject
    ProcessingService processingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processingService = DaggerProcessingServiceComponent.builder().build().provideProcessingService();
        this.requestPermissions();

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();

        btnSearch = findViewById(R.id.btnSeachText);

        btnSearch.setOnClickListener((View v) -> {
                    Utility.hideSoftKeyboard(MainActivity.this);
                    EditText editText = findViewById(R.id.searchText);
                    String searchText = editText.getText().toString();
                    String notFoundText = getResources().getString(R.string.search_no_result);
                    if (bitmap == null) {
                        notFoundText = getResources().getString(R.string.image_not_found);

                        Toast toast = Toast.makeText(getApplicationContext(), notFoundText, Toast.LENGTH_LONG);
                        toast.show();
                    } else if (searchText.isEmpty()) {
                        notFoundText = getResources().getString(R.string.search_text_not_found);
                        Toast toast = Toast.makeText(getApplicationContext(), notFoundText, Toast.LENGTH_LONG);
                        toast.show();
                    } else {
                        SearchDTO searchDTO = new SearchDTO();
                        searchDTO.setBitmap(bitmap);
                        searchDTO.setInput(searchText);
                        searchDTO.setCaseSensitive(caseSensitive.isChecked());
                        searchDTO.setOnlyAlphaNumeric(onlyAlphaNumeric.isChecked());

                        Bitmap processedBitmap = processingService.searchText(searchDTO);
                        if (processedBitmap != null) {
                            displaySearchResult(processedBitmap);
                        }
                    }
                }
        );

        ivImage = findViewById(R.id.ivImage);
        textView = findViewById(R.id.txtView);
        caseSensitive = findViewById(R.id.caseSensitive);
        onlyAlphaNumeric = findViewById(R.id.onlyAlphaNumeric);
        processingService.setTessOCR(MainActivity.this, getAssets());
        this.contentObserver = new ContentObserver(bitmap, textView);
        processingService.addProcessingContentObserver(this.contentObserver);

    }

    @OnClick(R.id.btnSeachText)
    void searchText() {

    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.cameraPreview);
        mPreview = processingService.getCameraPreview(myContext);
        cameraPreview.addView(mPreview);

        buttonCapture = (Button) findViewById(R.id.buttonCapture);
        buttonCapture.setOnClickListener(captureListener);

        buttonChangeCamera = (Button) findViewById(R.id.buttonChangeCamera);
        buttonChangeCamera.setOnClickListener(switchCameraListener);
    }

    OnClickListener switchCameraListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            String message = processingService.selectCamera();
            Toast toast = Toast.makeText(myContext, message, Toast.LENGTH_LONG);
            toast.show();
        }
    };

    private Camera.PictureCallback getPictureCallback() {
        Camera.PictureCallback picture = new Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                //make a new picture file
                File pictureFile = getOutputMediaFile();

                if (pictureFile == null) {
                    return;
                }
                try {
                    setImageFromCamera(data);
                    //write the file
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                    Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile.getName(), Toast.LENGTH_LONG);
                    toast.show();


                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }

                //refresh camera to continue preview
                mPreview.refreshCamera(processingService.getCamera());
            }
        };
        return picture;
    }

    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            processingService.getCamera().takePicture(null, null, mPicture);
        }
    };

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File("/sdcard/", "JCG Camera");

        //if this "JCGCamera folder does not exist
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        //take the current timeStamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        //and make a media file:
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }

    @Override
    protected void onResume() {
        super.onResume();

        processingService.setTessOCR(MainActivity.this, getAssets());

        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, processingService.getLoaderCallbackForOpenCV());
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            processingService.getLoaderCallbackForOpenCV().onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        Camera camera = processingService.getCamera();
        mPicture = getPictureCallback();
        mPreview.refreshCamera(camera);

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

    }

    @Override
    public void onBackPressed() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        } else {
            super.onBackPressed();
        }

        processingService.releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        processingService.releaseCamera();
    }

    private Bitmap setImageFromCamera(byte[] data) {

        Bitmap bm = processingService.getDisplayImage(data);
        bitmap = bm;
        ivImage.setImageBitmap(bm);

        if (bm != null) {
            processingService.fullTextRecognition(bm);
        }

        return bm;
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

