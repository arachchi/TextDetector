package com.cognitionlab.fingerReader.services.modules.processing;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.cognitionlab.fingerReader.database.FeedReaderDbHelper;
import com.cognitionlab.fingerReader.services.CameraService;
import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.SpeechService;
import com.cognitionlab.fingerReader.services.helpers.adaptors.FirebaseAdaptor;
import com.cognitionlab.fingerReader.services.helpers.adaptors.TextToSpeechDecorator;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;
import com.cognitionlab.fingerReader.services.helpers.observers.KeywordMapObserver;
import com.cognitionlab.fingerReader.services.helpers.callbacks.OpenCVLoaderCallback;
import com.cognitionlab.fingerReader.services.helpers.adaptors.ProcessingAdaptor;
import com.cognitionlab.fingerReader.services.helpers.adaptors.TessaractAdaptor;
import com.cognitionlab.fingerReader.services.helpers.observers.SpeechObserver;
import com.cognitionlab.fingerReader.services.impl.ProcessingServiceImpl;
import com.cognitionlab.fingerReader.services.modules.ApplicationContext;
import com.cognitionlab.fingerReader.services.modules.ApplicationScope;
import com.cognitionlab.fingerReader.services.modules.camera.CameraServiceModule;
import com.cognitionlab.fingerReader.services.modules.search.SearchServiceModule;
import com.cognitionlab.fingerReader.services.modules.speech.SpeechServiceModule;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import dagger.Module;
import dagger.Provides;

@Module(includes = {CameraServiceModule.class, SearchServiceModule.class})
public class ProcessingServiceModule {

    @Provides
    ProcessingService processingService(ContentNotifier contentNotifier,
                                        ProcessingAdaptor processingAdaptor,
                                        OpenCVLoaderCallback openCVLoaderCallback,
                                        KeywordMapObserver keywordMapObserver,
                                        CameraService cameraService,
                                        SearchService searchService,
                                        SpeechObserver speechObserver) {

        return new ProcessingServiceImpl(contentNotifier,
                processingAdaptor,
                openCVLoaderCallback,
                keywordMapObserver,
                cameraService,
                searchService,
                speechObserver);
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

    @Provides
    public SpeechObserver speechObserver(TextToSpeechDecorator textToSpeech, @ApplicationContext Context context, Set<String> dictionaryWords) {
        return new SpeechObserver(textToSpeech, context, dictionaryWords);
    }

    @Provides
    public TextToSpeechDecorator textToSpeech(@ApplicationContext Context context, SQLiteDatabase sqLiteDatabase) {
        TextToSpeech tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {

            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                } else {
                    Log.e("error", "Initilization Failed!");
                }
            }
        });

        int result = tts.setLanguage(Locale.US);

        if (result == TextToSpeech.LANG_MISSING_DATA
                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.d("TTS ERROR", "Issue with Text To Speech, either lang missing or lang not supported.");
        }

        return new TextToSpeechDecorator(tts, sqLiteDatabase);
    }

    @Provides
    public Set<String> dictionaryWords(@ApplicationContext Context context) {
        Set<String> wordsSet = new HashSet<>();

        final String DATA_PATH = context.getFilesDir() + File.separator + "FingerReader" + File.separator;
        final String FOLDER = "dictionary";
        final String FILE_PATH = FOLDER + File.separator + "dictionary.txt";
        final String FULL_PATH = DATA_PATH + FILE_PATH;
        String TAG = "Dictionary";

        String[] paths = new String[]{DATA_PATH, DATA_PATH + FOLDER + File.separator};

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }
        }
        File file = new File(FULL_PATH);
        if (!(file).exists()) {
            try {
                InputStream in = context.getAssets().open(FILE_PATH);
                OutputStream out = new FileOutputStream(new File(FULL_PATH));

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                file = new File(FULL_PATH);

                Log.v(TAG, "Copied " + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + " traineddata " + e.toString());
            }
        }
        try {
            Path path = Paths.get(file.toURI());
            byte[] readBytes = Files.readAllBytes(path);
            String wordListContents = new String(readBytes, "UTF-8");
            String[] words = wordListContents.split("\n"); //the text file should contain one word in one line

            Collections.addAll(wordsSet, words);
        } catch (Exception e) {

        }

        return wordsSet;
    }

    @Provides
    public SQLiteDatabase sqLiteDatabase(@ApplicationContext Context context) {
        FeedReaderDbHelper dbHelper = new FeedReaderDbHelper(context);

        try {
            dbHelper.createDataBase();
            System.out.println("DB CREated. NR");
        } catch (Exception e) {
            System.out.println("DB not CREated. NR");
        }

        try {
            dbHelper.openDataBase();
            System.out.println("DB Opened. NR");
        } catch (Exception e) {
            System.out.println("DB not Opened. NR");
        }

        return dbHelper.getDatabase();
    }
}
