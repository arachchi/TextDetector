package com.cognitionlab.fingerReader;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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

import com.cognitionlab.fingerReader.cameras.camera.CameraSource;
import com.cognitionlab.fingerReader.cameras.camera.CameraSourcePreview;
import com.cognitionlab.fingerReader.cameras.camera.GraphicOverlay;
import com.cognitionlab.fingerReader.cameras.graphics.OcrDetectorProcessor;
import com.cognitionlab.fingerReader.cameras.graphics.OcrGraphic;
import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;
import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentObserver;
import com.cognitionlab.fingerReader.services.modules.ApplicationComponent;
import com.cognitionlab.fingerReader.services.modules.ContextModule;
import com.cognitionlab.fingerReader.services.modules.DaggerApplicationComponent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends Activity {

    private static final String TAG = "fingerReader";
    // Intent request code to handle updating play services if needed.
    private static final int RC_HANDLE_GMS = 9001;
    // Permission request codes need to be < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;

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
//    @BindView(R.id.buttonCapture)
//    Button buttonCapture;
    @BindView(R.id.buttonChangeCamera)
    Button buttonChangeCamera;
    @BindView(R.id.cameraPreview)
    CameraSourcePreview mPreview;
    @BindView(R.id.searchText)
    EditText editText;
//    @BindView(R.id.btnTry)
//    Button btnTryAgain;
    @BindView(R.id.graphicOverlay)
    GraphicOverlay<OcrGraphic> ocrGraphicOverlay;

    private boolean isRequest;
    private boolean isPreview;

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private PopupWindow mPopupWindow;

    private Bitmap bitmap;
    private CameraSource.PictureCallback mPicture;
    private Context myContext;
    private ContentObserver contentObserver;
    private CameraSource mCameraSource;

    @Inject
    ProcessingService processingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestPermissions();
        setContentView(R.layout.activity_main);

//        LayoutInflater controlInflater = LayoutInflater.from(getBaseContext());
//        View viewControl = controlInflater.inflate(R.layout.control, null);
//        ViewGroup.LayoutParams layoutParamsControl
//                = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
//                ViewGroup.LayoutParams.FILL_PARENT);
//        this.addContentView(viewControl, layoutParamsControl);

        ButterKnife.bind(this);
        ButterKnife.setDebug(true);
        ApplicationComponent component = DaggerApplicationComponent.builder()
                .contextModule(new ContextModule(this))
                .build();
        processingService = component.getProcessingService();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        boolean autoFocus = true;
        boolean useFlash = false;

        mPicture = getPictureCallback();

        //mPreview = processingService.getCameraPreview();
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }
        //Complete


//      Create over lay as well as floating buttons in the app
//        Create a overlay on the camera previous just capture the image within the box. <- Do this.
        // Float the buttons on the camera previous if possible <- this is last
        // A pop up should appear until the word is pronounced.
//        Send these images to an end point with image and detected text, context data.


        processingService.setTessOCR(MainActivity.this, getAssets());
        this.contentObserver = new ContentObserver(bitmap, textView);
        processingService.addProcessingContentObserver(this.contentObserver);
        processingService.getCamera().startPreview();
        this.ocrGraphicOverlay.setContentNotifier(this.processingService.getContentNotifier());
        startCameraSource();

        String[] permissions = new String[]{
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        };

        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // do something
            }
            return;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {

            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.i("VOL_UP_pressed", String.valueOf(event.getKeyCode()));
                    Toast.makeText(getApplication(), "Image Capture clicked", Toast.LENGTH_SHORT).show();
                    //processingService.getCamera().takePicture(null, null, mPicture);
                }
                return true;

            case KeyEvent.KEYCODE_ENTER:
                if (action == KeyEvent.ACTION_DOWN) {
                    Log.i("ENTER_pressed", String.valueOf(event.getKeyCode()));
                    Toast.makeText(getApplication(), "ANDROID button clicked", Toast.LENGTH_SHORT).show();
                }
            default:
                return super.dispatchKeyEvent(event);
        }
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

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @OnClick(R.id.buttonChangeCamera)
    void switchCamera() {
        String message = "Camera Selected Successfully";
        try {
            Camera camera = processingService.selectCamera();
            startCameraSource();
        } catch (Exception e) {
            message = e.getMessage();
        }

        this.showToast(message);
    }

//    @OnClick(R.id.btnTry)
    void retryImage() {
        if (this.bitmap != null) {
            processingService.fullTextRecognition(this.bitmap);
        }
    }

    //This is for the in-built Camera
    private CameraSource.PictureCallback getPictureCallback() {
        CameraSource.PictureCallback picture = (byte[] data) -> {
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
                System.out.println("Image captured.");

            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        };
        return picture;
    }

//    @OnClick(R.id.buttonCapture)
    void imageCapture() {
        try {
            //openCamera(CameraInfo.CAMERA_FACING_BACK);
            //releaseCameraSource();
            //releaseCamera();
            //openCamera(CameraInfo.CAMERA_FACING_BACK);
            //setUpCamera(camera);
            //Thread.sleep(1000);
            int rotation = 90;
            mCameraSource.takePicture(null, mPicture);

        } catch (Exception ex) {

        } finally {
            startCameraSource();
        }
    }

    //make picture and save to a folder
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "FingerReader");

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

        mPicture = getPictureCallback();

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
            displayPreview(bm);
//            processingService.fullTextRecognition(bm);
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

    private void displayPreview(Bitmap bitmap) {
//        Context mContext = MainActivity.this;
//        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
//
//        LinearLayout linearLayout = findViewById(R.id.LinearLayout1);
//
//        assert inflater != null;
//        View customView = inflater.inflate(R.layout.popup_preview, null);
//        //https://stackoverflow.com/questions/4933612/how-to-convert-coordinates-of-the-image-view-to-the-coordinates-of-the-bitmap
//        mPopupWindow = new PopupWindow(
//                customView,
//                RelativeLayout.LayoutParams.FILL_PARENT,
//                RelativeLayout.LayoutParams.FILL_PARENT
//        );
//        mPopupWindow.setOutsideTouchable(true);
//        mPopupWindow.setFocusable(true);
//
//        // Set an elevation value for popup window
//        // Call requires API level 21
//        if (Build.VERSION.SDK_INT >= 21) {
//            mPopupWindow.setElevation(5.0f);
//        }
//
//        ImageButton closeButton = customView.findViewById(R.id.ib_close);
//        ImageView imageView = customView.findViewById(R.id.popupView);
//
//
//        closeButton.setOnClickListener((View view) -> {
//            mPopupWindow.dismiss();
//        });
//
//        List<FirebaseVisionText.Element> elementList = new ArrayList<>();
//
//
//        mPopupWindow.showAtLocation(linearLayout, Gravity.CENTER, 0, 0);
//
//        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
//        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
//
//        DataExtractionDTO dataExtractionDTO = new DataExtractionDTO();
//
//        Log.d("TIME", "Processing Started " + new Date());
//        textRecognizer.processImage(image)
//                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
//                    @Override
//                    public void onSuccess(FirebaseVisionText result) {
//                        // Task completed successfully
//                        List<Point[]> textBlocksList = new ArrayList<>();
//                        FirebaseVisionText.TextBlock selectedTextBlock = null;
//                        Point[] selectedBlock;
//                        Point[] currentBlock;
//                        String resultText = result.getText();
//                        HashMap<Integer, List<FirebaseVisionText.TextBlock>> map = new HashMap<>();
//
//                        System.out.println(resultText);
//
//                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
//                            int index = block.getCornerPoints()[0].y;
//                            List<FirebaseVisionText.TextBlock> list;
//                            if (map.get(index) == null) {
//                                list = new ArrayList<>();
//                            } else {
//                                list = map.get(index);
//                            }
//
//
//                        }
//
//                        List<Integer> keySet = new ArrayList<>(map.keySet());
//                        Collections.sort(keySet);
//
//                        if (!keySet.isEmpty()) {
//                            int bottomY = keySet.get(keySet.size() - 1);
//                            List<FirebaseVisionText.TextBlock> list = map.get(bottomY);
//                            int listSize = list.size();
//                            int selectedWordIndex = 0;
//
//                            selectedTextBlock = list.get(0);
//                            resultText = selectedTextBlock.getText();
//                        }
//
//                        Mat matrix = new Mat();
//                        Utils.bitmapToMat(bitmap, matrix);
//                        final int lineThickness = 10;
//                        Scalar CONTOUR_COLOR_HIGHLIGHT = new Scalar(255, 0, 0, 255);
//
//
//                        Log.d("TIME", "Processing Finished " + new Date());
//                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
//                            String blockText = block.getText();
//                            Float blockConfidence = block.getConfidence();
//                            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//                            Point[] blockCornerPoints = block.getCornerPoints();
//                            Rect blockFrame = block.getBoundingBox();
//
//                            if (selectedTextBlock == null) {
//                                selectedTextBlock = block;
//                            } else {
//                                selectedBlock = selectedTextBlock.getCornerPoints();
//                                currentBlock = block.getCornerPoints();
//                                Point sp0 = selectedBlock[0], sp1 = selectedBlock[2];
//                                Point p0 = currentBlock[0], p1 = currentBlock[2];
//
//                            }
//
//
//                            for (FirebaseVisionText.Line line : block.getLines()) {
//                                String lineText = line.getText();
//                                Float lineConfidence = line.getConfidence();
//                                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//                                Point[] lineCornerPoints = line.getCornerPoints();
//                                Rect lineFrame = line.getBoundingBox();
//                                for (FirebaseVisionText.Element element : line.getElements()) {
//                                    String elementText = element.getText();
//                                    Float elementConfidence = element.getConfidence();
//                                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//                                    Point[] elementCornerPoints = element.getCornerPoints();
//                                    Rect elementFrame = element.getBoundingBox();
//                                    elementList.add(element);
//
//                                    double leftx = elementCornerPoints[0].x,
//                                            lefty = elementCornerPoints[0].y,
//                                            rightx = elementCornerPoints[2].x,
//                                            righty = elementCornerPoints[2].y;
//
//                                    if (leftx > elementCornerPoints[3].x) {
//                                        leftx = elementCornerPoints[3].x;
//                                    }
//                                    if (lefty > elementCornerPoints[1].y) {
//                                        lefty = elementCornerPoints[1].y;
//                                    }
//
//                                    if (rightx < elementCornerPoints[1].x) {
//                                        rightx = elementCornerPoints[1].x;
//                                    }
//                                    if (righty < elementCornerPoints[3].y) {
//                                        righty = elementCornerPoints[3].y;
//                                    }
//
//                                    org.opencv.core.Point pointA = new org.opencv.core.Point(leftx, lefty),
//                                            pointB = new org.opencv.core.Point(rightx, righty);
//                                    Imgproc.rectangle(matrix, pointA, pointB, CONTOUR_COLOR_HIGHLIGHT, lineThickness);
//                                }
//                            }
//                        }
//
//                        Bitmap processedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
//                        Utils.matToBitmap(matrix, processedBitmap);
//
//                        imageView.setImageBitmap(processedBitmap);
//                        imageView.setOnTouchListener(new View.OnTouchListener() {
//                            @Override
//                            public boolean onTouch(View v, MotionEvent event) {
//                                if (event.getAction() == MotionEvent.ACTION_DOWN) {
//                                    textView.setText("Touch coordinates : " +
//                                            String.valueOf(event.getX()) + "x" + String.valueOf(event.getY()));
//                                    System.out.println("----------------------" + event.getAction());
//                                    System.out.println(String.valueOf(event.getX()) + "x" + String.valueOf(event.getY()));
//                                    double tx = event.getX(), ty = event.getY();
//                                    for (FirebaseVisionText.Element element : elementList) {
//                                        Point[] elementCornerPoints = element.getCornerPoints();
//                                        double leftx = elementCornerPoints[0].x,
//                                                lefty = elementCornerPoints[0].y,
//                                                rightx = elementCornerPoints[2].x,
//                                                righty = elementCornerPoints[2].y;
//
//                                        if (leftx > elementCornerPoints[3].x) {
//                                            leftx = elementCornerPoints[3].x;
//                                        }
//                                        if (lefty > elementCornerPoints[1].y) {
//                                            lefty = elementCornerPoints[1].y;
//                                        }
//
//                                        if (rightx < elementCornerPoints[1].x) {
//                                            rightx = elementCornerPoints[1].x;
//                                        }
//                                        if (righty < elementCornerPoints[3].y) {
//                                            righty = elementCornerPoints[3].y;
//                                        }
//
//                                        if (leftx <= tx && rightx >= tx && lefty <= ty && righty >= ty) {
//                                            System.out.println("---------------------------------");
//                                            System.out.println(element.getText());
//                                            System.out.println("---------------------------------");
//                                        }
//
//                                    }
//                                }
//
//                                return true;
//                            }
//                        });
//                    }
//                })
//                .addOnFailureListener(
//                        new OnFailureListener() {
//                            @Override
//                            public void onFailure(Exception e) {
//                                System.out.println("Image Recognition Failed.");
//                                Log.d("ERROR", e.getMessage());
//                                // Task failed with an exception
//                                // ...
//
//                            }
//                        });


    }

    private void showToast(String text) {
        Toast.makeText(myContext, text, Toast.LENGTH_LONG).show();
    }

    private void createCameraSource(boolean autoFocus, boolean useFlash) {
        Context context = getApplicationContext();

        // A text recognizer is created to find text.  An associated processor instance
        // is set to receive the text recognition results and display graphics for each text block
        // on screen.
        TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
        textRecognizer.setProcessor(new OcrDetectorProcessor(ocrGraphicOverlay));


        if (!textRecognizer.isOperational()) {
            // Note: The first time that an app using a Vision API is installed on a
            // device, GMS will download a native libraries to the device in order to do detection.
            // Usually this completes before the app is run for the first time.  But if that
            // download has not yet completed, then the above call will not detect any text,
            // barcodes, or faces.
            //
            // isOperational() can be used to check if the required native libraries are currently
            // available.  The detectors will automatically become operational once the library
            // downloads complete on device.
            Log.w(TAG, "Detector dependencies are not yet available.");

            // Check for low storage.  If there is low storage, the native library will not be
            // downloaded, so detection will not become operational.
            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;

            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                Log.w(TAG, getString(R.string.low_storage_error));
            }
        }

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the text recognizer to detect small pieces of text.
        mCameraSource =
                new CameraSource.Builder(getApplicationContext(), textRecognizer)
                        .setFacing(CameraSource.CAMERA_FACING_BACK)
                        .setRequestedPreviewSize(1280, 1024)
                        .setRequestedFps(2.0f)
                        .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                        .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                        .build();
    }

    private void startCameraSource() throws SecurityException {
        // Check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, ocrGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };
    }
}

