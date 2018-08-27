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
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
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
import com.cognitionlab.fingerReader.services.helpers.observers.ContentObserver;
import com.cognitionlab.fingerReader.services.modules.ApplicationComponent;
import com.cognitionlab.fingerReader.services.modules.ContextModule;
import com.cognitionlab.fingerReader.services.modules.DaggerApplicationComponent;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    @BindView(R.id.ivImage)
    ImageView ivImage;
    @BindView(R.id.btnSeachText)
    Button btnSearch;
    @BindView(R.id.txtView)
    TextView textView;
    @BindView(R.id.caseSensitive)
    CheckBox caseSensitive;
    @BindView(R.id.onlyAlphaNumeric)
    CheckBox onlyAlphaNumeric;
    @BindView(R.id.buttonCapture)
    Button buttonCapture;
    @BindView(R.id.buttonChangeCamera)
    Button buttonChangeCamera;
    @BindView(R.id.cameraPreview)
    LinearLayout cameraPreview;
    @BindView(R.id.searchText)
    EditText editText;

    private PopupWindow mPopupWindow;

    private String userChoosenTask;
    private Bitmap bitmap;
    private Camera.PictureCallback mPicture;
    private Context myContext;
    private ContentObserver contentObserver;
    private CameraPreview mPreview;

    @Inject
    ProcessingService processingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestPermissions();
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        ButterKnife.setDebug(true);
        ApplicationComponent component = DaggerApplicationComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
        processingService = component.getProcessingService();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        mPreview = processingService.getCameraPreview();
        cameraPreview.addView(mPreview);

        processingService.setTessOCR(MainActivity.this, getAssets());
        this.contentObserver = new ContentObserver(bitmap, textView);
        processingService.addProcessingContentObserver(this.contentObserver);

    }

    @OnClick(R.id.btnSeachText)
    void searchText() {
        Utility.hideSoftKeyboard(MainActivity.this);
        String searchText = editText.getText().toString();
        if (bitmap == null) {
            this.showToast(getResources().getString(R.string.image_not_found));
        } else if (searchText.isEmpty()) {
            this.showToast(getResources().getString(R.string.search_text_not_found));
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

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }
    }

    @OnClick(R.id.buttonChangeCamera)
    void switchCamera() {
        String message = "Camera Selected Successfully";
        try {
            Camera camera = processingService.selectCamera();
            mPreview.refreshCamera(camera);
        } catch (Exception e) {
            message = e.getMessage();
        }

        this.showToast(message);
    }

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

    @OnClick(R.id.buttonCapture)
    void imageCapture() {
        processingService.getCamera().takePicture(null, null, mPicture);
    }

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

    @Override
    protected void onDestroy() {
        super.onDestroy();

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

        closeButton.setOnClickListener((View view) -> {
            mPopupWindow.dismiss();
        });

        mPopupWindow.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);

    }

    private void showToast(String text) {
        Toast.makeText(myContext, text, Toast.LENGTH_LONG).show();
    }

}

