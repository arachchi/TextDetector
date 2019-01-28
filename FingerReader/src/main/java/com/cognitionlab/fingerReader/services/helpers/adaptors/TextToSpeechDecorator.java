package com.cognitionlab.fingerReader.services.helpers.adaptors;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.speech.tts.TextToSpeech;

public class TextToSpeechDecorator {

    private TextToSpeech textToSpeech;
    private SQLiteDatabase sqLiteDatabase;

    public TextToSpeechDecorator(TextToSpeech textToSpeech, SQLiteDatabase sqLiteDatabase) {
        this.textToSpeech = textToSpeech;
        this.sqLiteDatabase = sqLiteDatabase;
    }

    public void speakText(String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    public void speakTextAndMeaning(String text) {
        text = text.trim().toLowerCase();
        text = "'".concat(text.concat("'"));
        String query = "select word, gloss from wn_synset s inner join wn_gloss g on s.synset_id = g.synset_id where s.word = " + text;
        Cursor resultSet = sqLiteDatabase.rawQuery(query, null);
        Integer value = resultSet.getCount();

        if (resultSet.moveToNext()) {
            String word = resultSet.getString(resultSet.getColumnIndex("word"));
            String gloss = resultSet.getString(resultSet.getColumnIndex("gloss"));

            System.out.println(word + "  " + gloss);
            String filler = "the meaning is \n";
            String speakText = word + " " + filler + " " + gloss;

            this.speakText(speakText);
        } else {
            this.speakText(text);
        }


        System.out.println(value);
    }
}
