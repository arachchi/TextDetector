package com.cognitionlab.fingerReader.services.modules.camera;

import android.content.Context;
import android.hardware.Camera;

import com.cognitionlab.fingerReader.CameraPreview;
import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.impl.CameraServiceImpl;
import com.cognitionlab.fingerReader.services.impl.SearchServiceImpl;
import com.cognitionlab.fingerReader.services.modules.ApplicationContext;
import com.cognitionlab.fingerReader.services.modules.ContextModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(includes = ContextModule.class)
public class CameraServiceModule {

    @Provides
    CameraService cameraService(CameraPreview cameraPreview, Camera camera) {
        return new CameraServiceImpl(cameraPreview, camera);
    }

    @Provides
    Camera camera() {
        return Camera.open();
    }

    @Provides
    CameraPreview cameraPreview(@ApplicationContext Context context, Camera camera) {
        return new CameraPreview(context, camera);
    }
}
