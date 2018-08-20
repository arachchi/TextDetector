package com.cognitionlab.fingerReader.services.modules.extractContent;

import com.cognitionlab.fingerReader.services.ExtractContentService;
import com.cognitionlab.fingerReader.services.impl.ExtractContentServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class ExtractContentServiceModule {

    @Provides
    @Singleton
    ExtractContentService provideExtractContentService() {
        return new ExtractContentServiceImpl();
    }
}
