package com.cognitionlab.fingerReader;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
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

import com.cognitionlab.fingerReader.cameras.camera.CameraSource;
import com.cognitionlab.fingerReader.cameras.camera.CameraSourcePreview;
import com.cognitionlab.fingerReader.cameras.camera.GraphicOverlay;
import com.cognitionlab.fingerReader.cameras.graphics.OcrDetectorProcessor;
import com.cognitionlab.fingerReader.cameras.graphics.OcrGraphic;
import com.cognitionlab.fingerReader.dtos.SearchDTO;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentObserver;
import com.cognitionlab.fingerReader.services.modules.ApplicationComponent;
import com.cognitionlab.fingerReader.services.modules.ContextModule;
import com.cognitionlab.fingerReader.services.modules.DaggerApplicationComponent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.text.TextRecognizer;
import com.jiangdg.usbcamera.UVCCameraHelper;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;
import com.serenegiant.usb.widget.UVCCameraTextureView;

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

public class MainActivity extends Activity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback {

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
    @BindView(R.id.buttonCapture)
    Button buttonCapture;
    @BindView(R.id.buttonChangeCamera)
    Button buttonChangeCamera;
    @BindView(R.id.cameraPreview)
    CameraSourcePreview mPreview;
    @BindView(R.id.searchText)
    EditText editText;
    @BindView(R.id.btnTry)
    Button btnTryAgain;
    @BindView(R.id.graphicOverlay)
    GraphicOverlay<OcrGraphic> ocrGraphicOverlay;

    @BindView(R.id.usbCameraPreview)
    public View mTextureView;

    public UVCCameraTextureView usbCameraTextureView;

    private UVCCameraHelper mCameraHelper;
    public CameraViewInterface mUVCCameraView;
    private boolean isRequest;
    private boolean isPreview;

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private PopupWindow mPopupWindow;
    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        @Override
        public void onAttachDev(UsbDevice device) {
            if (mCameraHelper == null || mCameraHelper.getUsbDeviceCount() == 0) {
                showShortMsg("check no usb camera");
                return;
            }
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }
        }

        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected) {
            if (!isConnected) {
                showShortMsg("fail to connect,please check resolution params");
                isPreview = false;
            } else {
                isPreview = true;
                showShortMsg("connecting");
                // initialize seekbar
                // need to wait UVCCamera initialize over
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if (mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        @Override
        public void onDisConnectDev(UsbDevice device) {
            showShortMsg("disconnecting");
        }
    };

    private Bitmap bitmap;
    private Camera.PictureCallback pictureCallback;
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
        this.usbCameraTextureView = findViewById(R.id.usbCameraPreview);

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

        //mPreview = processingService.getCameraPreview();
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource(autoFocus, useFlash);
        } else {
            requestCameraPermission();
        }


        //USB Camera View Setup
        mUVCCameraView = (CameraViewInterface) mTextureView;
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();

        if (mCameraHelper.getUSBMonitor() == null) {
            mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_YUYV);
        }
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);


        mCameraHelper.setOnPreviewFrameListener(new AbstractUVCCameraHandler.OnPreViewResultListener() {
            @Override
            public void onPreviewResult(byte[] nv21Yuv) {

            }
        });


        //Complete

        processingService.setTessOCR(MainActivity.this, getAssets());
        this.contentObserver = new ContentObserver(bitmap, textView);
        processingService.addProcessingContentObserver(this.contentObserver);

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
                    // processingService.getCamera().takePicture(null, null, pictureCallback);

                    if (usbCameraTextureView != null) {
                        usbCameraImageCapture();
                    }
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
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
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

    @OnClick(R.id.btnTry)
    void retryImage() {
        if (this.bitmap != null) {
            processingService.fullTextRecognition(this.bitmap);
        }
    }

    //This is for the in-built Camera
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

                startCameraSource();
            }
        };
        return picture;
    }

    private void usbCameraImageCapture() {
        Bitmap bitmap = usbCameraTextureView.getBitmap();

        Matrix matrix = new Matrix();
        matrix.postRotate(90); // anti-clockwise by 90 degrees

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

        ivImage.setImageBitmap(rotatedBitmap);

        if (bitmap != null) {
            this.bitmap = rotatedBitmap;
            processingService.fullTextRecognition(rotatedBitmap);
        }
    }

    @OnClick(R.id.buttonCapture)
    void imageCapture() {
        mCameraSource.getCamera().takePicture(null, null, pictureCallback);
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

        pictureCallback = getPictureCallback();
        startCameraSource();

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

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("Mini Dialog");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) {
        if (!isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.startPreview(mUVCCameraView);
            isPreview = true;
        }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
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

