package com.cognitionlab.fingerReader.services.modules.camera;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.impl.CameraServiceImpl;
import com.cognitionlab.fingerReader.services.impl.SearchServiceImpl;
import com.cognitionlab.fingerReader.services.modules.ActivityModule;
import com.cognitionlab.fingerReader.services.modules.ApplicationContext;
import com.cognitionlab.fingerReader.services.modules.ContextModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ContextModule.class, ActivityModule.class})
public class CameraServiceModule {

    @Provides
    CameraService cameraService(CameraPreview cameraPreview, Camera camera) {
        return new CameraServiceImpl(cameraPreview, camera);
    }

    @Provides
    Camera camera() {

        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }

        return Camera.open(cameraId);
    }

    @Provides
    CameraPreview cameraPreview(@ApplicationContext Context context, Camera camera) {
        return new CameraPreview(context, camera);
    }
}
