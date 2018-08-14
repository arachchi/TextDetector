package com.cognitionlab.fingerReader;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
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

import com.example.detectText.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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

    private Camera mCamera;
    private CameraPreview mPreview;
    private Camera.PictureCallback mPicture;
    private Button capture, switchCamera;
    private Context myContext;
    private LinearLayout cameraPreview;
    private boolean cameraFront = false;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            }
        }

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        initialize();

        btnSearch = findViewById(R.id.btnSeachText);

        btnSearch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Utility.hideSoftKeyboard(MainActivity.this);
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

    public void initialize() {
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);

        capture = (Button) findViewById(R.id.button_capture);
        capture.setOnClickListener(captureListener);

        switchCamera = (Button) findViewById(R.id.button_ChangeCamera);
        switchCamera.setOnClickListener(switchCameraListener);
    }

    OnClickListener switchCameraListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            //get the number of cameras
            int camerasNumber = Camera.getNumberOfCameras();
            if (camerasNumber > 1) {
                //release the old camera instance
                //switch camera, from the front and the back and vice versa

                releaseCamera();
                chooseCamera();
            } else {
                Toast toast = Toast.makeText(myContext, "Sorry, your phone has only one camera!", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    };

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                cameraFront = true;
                break;
            }
        }
        return cameraId;
    }

    private int findBackFacingCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                cameraFront = false;
                break;
            }
        }
        return cameraId;
    }

    public void chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            int cameraId = findBackFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);

                Camera.Parameters params = mCamera.getParameters();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                params.set("orientation", "portrait");
                mCamera.setParameters(params);

                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        } else {
            int cameraId = findFrontFacingCamera();
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                mCamera = Camera.open(cameraId);

                Camera.Parameters params = mCamera.getParameters();
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                params.set("orientation", "portrait");
                mCamera.setParameters(params);

                mPicture = getPictureCallback();
                mPreview.refreshCamera(mCamera);
            }
        }


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
                mPreview.refreshCamera(mCamera);
            }
        };
        return picture;
    }

    OnClickListener captureListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mCamera.takePicture(null, null, mPicture);
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

    private void releaseCamera() {
        // stop and release camera
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
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

        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext, "Sorry, your phone does not have a camera!", Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        if (mCamera == null) {
            //if the front facing camera does not exist
            if (findFrontFacingCamera() < 0) {
                Toast.makeText(this, "No front facing camera found.", Toast.LENGTH_LONG).show();
                switchCamera.setVisibility(View.GONE);
            }
            mCamera = Camera.open(findBackFacingCamera());
            mPicture = getPictureCallback();
            mPreview.refreshCamera(mCamera);
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

        releaseCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //when on Pause, release camera in order to be used from other applications
        releaseCamera();
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
        bitmap = bm;
        ivImage.setImageBitmap(bm);

        return bm;
    }

    private Bitmap setImageFromCamera(byte[] data) {

        Bitmap image = null, bm = null;

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        image = BitmapFactory.decodeByteArray(data, 0, data.length, options);

        bitmap = image;
        ivImage.setImageBitmap(image);

        if (image != null) {
            performFullTextRecognition(image);
        }

        return bm;
    }

    private void performFullTextRecognition(final Bitmap bitmap) {
        AsyncTaskRunner runner = new AsyncTaskRunner();
        runner.execute(bitmap);
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
                notFoundText = getResources().getString(R.string.image_not_found);
            }

            if (text.isEmpty()) {
                notFoundText = getResources().getString(R.string.search_text_not_found);
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
        final int lineThickness = 10;

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

    private class AsyncTaskRunner extends AsyncTask<Bitmap, String, String> {

        private String text;

        @Override
        protected String doInBackground(final Bitmap... bitmap) {

            keywordsMap = mTessOCR.getKeywordMap(bitmap[0]);

            final String srcText = mTessOCR.getResults(bitmap[0]);
            if (srcText.isEmpty()) {
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      textView.setText(getResources().getString(R.string.edit_text_hint));
                                  }
                              }
                );
            } else {
                runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      textView.setText(srcText);
                                  }
                              }
                );
            }


            return srcText;
        }


        @Override
        protected void onPostExecute(String result) {
        }


        @Override
        protected void onPreExecute() {
        }


        @Override
        protected void onProgressUpdate(String... text) {

        }
    }

    private boolean hasCamera(Context context) {
        //check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }

}

