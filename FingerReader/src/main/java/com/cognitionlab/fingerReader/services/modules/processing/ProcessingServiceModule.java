package com.cognitionlab.fingerReader.services.modules.processing;

import android.content.Context;

import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.helpers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.KeywordMapObserver;
import com.cognitionlab.fingerReader.services.helpers.OpenCVLoaderCallback;
import com.cognitionlab.fingerReader.services.helpers.ProcessingAdaptor;
import com.cognitionlab.fingerReader.services.helpers.TessaractAdaptor;
import com.cognitionlab.fingerReader.services.impl.ProcessingServiceImpl;
import com.cognitionlab.fingerReader.services.modules.ApplicationContext;
import com.cognitionlab.fingerReader.services.modules.ContextModule;
import com.cognitionlab.fingerReader.services.modules.camera.CameraServiceModule;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;
import com.cognitionlab.fingerReader.services.modules.speech.SpeechServiceModule;

import org.opencv.android.OpenCVLoader;

import javax.inject.Singleton;

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
    public ContentNotifier contentNotifier() {
        return new ContentNotifier();
    }

    @Provides
    public ProcessingAdaptor processingAdaptor(@ApplicationContext Context context) {
        return new TessaractAdaptor(context, context.getAssets());
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
