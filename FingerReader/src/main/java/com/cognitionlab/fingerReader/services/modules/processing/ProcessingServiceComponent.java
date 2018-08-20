package com.cognitionlab.fingerReader.services.modules.processing;

import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.modules.camera.CameraServiceModule;
import com.cognitionlab.fingerReader.services.modules.extractContent.ExtractContentServiceModule;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;
import com.cognitionlab.fingerReader.services.modules.speech.SpeechServiceModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {
        CameraServiceModule.class,
        ExtractContentServiceModule.class,
        ProcessingServiceModule.class,
        SearchServiceModule.class,
        SpeechServiceModule.class,
        ProcessingServiceModule.class})
public interface ProcessingServiceComponent {

    ProcessingService provideProcessingService();
}
