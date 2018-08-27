package com.cognitionlab.fingerReader.services.modules.processing;

import android.content.Context;

import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.helpers.adaptors.FirebaseAdaptor;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.observers.KeywordMapObserver;
import com.cognitionlab.fingerReader.services.helpers.callbacks.OpenCVLoaderCallback;
import com.cognitionlab.fingerReader.services.helpers.adaptors.ProcessingAdaptor;
import com.cognitionlab.fingerReader.services.helpers.adaptors.TessaractAdaptor;
import com.cognitionlab.fingerReader.services.impl.ProcessingServiceImpl;
import com.cognitionlab.fingerReader.services.modules.ApplicationContext;
import com.cognitionlab.fingerReader.services.modules.ApplicationScope;
import com.cognitionlab.fingerReader.services.modules.camera.CameraServiceModule;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;
import com.cognitionlab.fingerReader.services.modules.speech.SpeechServiceModule;

import dagger.Module;
import dagger.Provides;

@Module(includes = {CameraServiceModule.class, SpeechServiceModule.class, SearchServiceModule.class})
public class ProcessingServiceModule {

    @Provides
    ProcessingService processingService(ContentNotifier contentNotifier,
                                        ProcessingAdaptor processingAdaptor,
                                        OpenCVLoaderCallback openCVLoaderCallback,
                                        KeywordMapObserver keywordMapObserver,
                                        CameraService cameraService,
                                        SearchService searchService,
                                        SpeechService speechService) {

        return new ProcessingServiceImpl(contentNotifier,
                processingAdaptor,
                openCVLoaderCallback,
                keywordMapObserver,
                cameraService,
                searchService,
                speechService);
    }

    @Provides
    @ApplicationScope
    public ContentNotifier contentNotifier() {
        return new ContentNotifier();
    }

    @Provides
    public ProcessingAdaptor processingAdaptor(@ApplicationContext Context context, ContentNotifier contentNotifier) {
        return new FirebaseAdaptor(context, contentNotifier);
    }

    @Provides
    public OpenCVLoaderCallback openCVLoaderCallback(@ApplicationContext Context context) {
        return new OpenCVLoaderCallback(context);
    }

    @Provides
    public KeywordMapObserver keywordMapObserver() {
        return new KeywordMapObserver();
    }
}
