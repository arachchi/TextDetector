package com.cognitionlab.fingerReader.services.modules.camera;

import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.impl.CameraServiceImpl;
import com.cognitionlab.fingerReader.services.impl.SearchServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class CameraServiceModule {

    @Provides
    @Singleton
    CameraService provideSearchService() {
        return new CameraServiceImpl();
    }
}
