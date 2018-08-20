package com.cognitionlab.fingerReader.services.modules.camera;

import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {CameraServiceModule.class})
public interface CameraServiceComponent {

    CameraService provideCameraServiceModule();
}
