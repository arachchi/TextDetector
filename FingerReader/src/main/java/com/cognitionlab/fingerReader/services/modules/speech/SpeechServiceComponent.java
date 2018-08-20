package com.cognitionlab.fingerReader.services.modules.speech;

import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SpeechServiceModule.class})
public interface SpeechServiceComponent {

    SpeechService provideSpeechService();
}
