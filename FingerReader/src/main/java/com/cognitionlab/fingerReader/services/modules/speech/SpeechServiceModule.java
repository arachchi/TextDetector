package com.cognitionlab.fingerReader.services.modules.speech;

import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.impl.SearchServiceImpl;
import com.cognitionlab.fingerReader.services.impl.SpeechServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SpeechServiceModule {

    @Provides
    SpeechService speechService() {
        return new SpeechServiceImpl();
    }
}
