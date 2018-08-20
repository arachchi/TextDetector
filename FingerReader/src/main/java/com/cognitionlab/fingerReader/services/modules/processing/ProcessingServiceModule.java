package com.cognitionlab.fingerReader.services.modules.processing;

import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.impl.ProcessingServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ProcessingServiceModule {

    @Provides
    @Singleton
    ProcessingService provideProcessingService() {
        return new ProcessingServiceImpl();
    }
}
