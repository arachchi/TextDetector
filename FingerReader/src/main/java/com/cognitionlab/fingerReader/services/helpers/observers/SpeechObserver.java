package com.cognitionlab.fingerReader.services.helpers.observers;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.speech.tts.TextToSpeech;
import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import com.cognitionlab.fingerReader.services.helpers.adaptors.TextToSpeechDecorator;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class SpeechObserver implements Observer {

    private TextToSpeechDecorator textToSpeech;

    private Context context;
    private AssetManager assetManager;
    private TextServicesManager textServicesManager;
    private Set<String> dictionaryWords;

    public SpeechObserver(TextToSpeechDecorator textToSpeech, Context context, Set<String> dictionaryWords) {
        this.textToSpeech = textToSpeech;
        this.context = context;
        this.assetManager = context.getAssets();
        this.textServicesManager = (TextServicesManager) context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        this.dictionaryWords = dictionaryWords;
    }

    @Override
    public void update(Observable o, Object arg) {
        ContentNotifier contentNotifier = (ContentNotifier) o;
        String textToIdentify = contentNotifier.getDataExtractionDTO().getContent();

        if (textToIdentify.isEmpty()) {
            textToIdentify = "No suggestions";
        }

        String text = textToIdentify;

        SpellCheckerSession mScs;
        mScs = textServicesManager.newSpellCheckerSession(null, Locale.ENGLISH, new SpellCheckerSession.SpellCheckerSessionListener() {
            @Override
            public void onGetSuggestions(SuggestionsInfo[] results) {
                for (SuggestionsInfo suggestionsInfo : results) {
                    final int len = suggestionsInfo.getSuggestionsCount();
                    String speechText;
                    if (len == -1) {
                        speechText = text;
                        textToSpeech.speakTextAndMeaning(speechText);
                    } else {
                        speechText = suggestionsInfo.getSuggestionAt(0);
                        textToSpeech.speakText("Suggested Meaning is " + speechText);
                    }

                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < len; j++) {
                        sb.append("," + suggestionsInfo.getSuggestionAt(j));
                    }
                    sb.append(" (" + len + ")");

                    System.out.println(suggestionsInfo);
                    System.out.println("Suggestions");
                    System.out.println(sb.toString());
                }

            }

            @Override
            public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
                System.out.println("Sentence Suggestions");
            }
        }, true);

        if (dictionaryWords.contains(text)) {
            System.out.println("-----Detected word is in the dictionary.");
            textToSpeech.speakTextAndMeaning(text);
        } else {
            System.out.println("xxxxxDetected word is not in the dictionary. Looking for suggestions.");
            mScs.getSuggestions(new TextInfo(text), 3);
        }

    }

}
