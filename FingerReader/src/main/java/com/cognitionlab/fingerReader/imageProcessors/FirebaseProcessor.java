package com.cognitionlab.fingerReader.imageProcessors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;
import com.cognitionlab.fingerReader.services.helpers.observers.ContentNotifier;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FirebaseProcessor implements ImageProcessor {

    private ContentNotifier contentNotifier;
    private Set<String> dictionaryWords;

    public FirebaseProcessor(Context context, ContentNotifier contentNotifier, Set<String> dictionaryWords) {
        FirebaseApp.initializeApp(context);
        this.contentNotifier = contentNotifier;
        this.dictionaryWords = dictionaryWords;
    }

    @Override
    public String getResults(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        // ...
                        String resultText = result.getText();
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            String blockText = block.getText();
                            Float blockConfidence = block.getConfidence();
                            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
                            Point[] blockCornerPoints = block.getCornerPoints();
                            Rect blockFrame = block.getBoundingBox();
                            for (FirebaseVisionText.Line line : block.getLines()) {
                                String lineText = line.getText();
                                Float lineConfidence = line.getConfidence();
                                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                                Point[] lineCornerPoints = line.getCornerPoints();
                                Rect lineFrame = line.getBoundingBox();
                                for (FirebaseVisionText.Element element : line.getElements()) {
                                    String elementText = element.getText();
                                    Float elementConfidence = element.getConfidence();
                                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                                    Point[] elementCornerPoints = element.getCornerPoints();
                                    Rect elementFrame = element.getBoundingBox();
                                }
                            }
                        }

                        System.out.println(resultText);
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                // Task failed with an exception
                                // ...
                            }
                        });

        return "Waiting";
    }


    @Override
    public Map<String, List<Rect>> getKeywordMap(String result) {
        return null;
    }

    @Override
    public void getExtractedData(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        DataExtractionDTO dataExtractionDTO = new DataExtractionDTO();

        Log.d("TIME", "Processing Started " + new Date());
        textRecognizer.processImage(image)
                .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                    @Override
                    public void onSuccess(FirebaseVisionText result) {
                        // Task completed successfully
                        List<Point[]> textBlocksList = new ArrayList<>();
                        FirebaseVisionText.TextBlock selectedTextBlock = null;
                        Point[] selectedBlock;
                        Point[] currentBlock;
                        String resultText = result.getText();
                        String totalResultProcessedText = processSelectedText(resultText);
                        boolean garbageIgnorable = !totalResultProcessedText.isEmpty();
                        HashMap<Integer, List<FirebaseVisionText.TextBlock>> map = new HashMap<>();

                        System.out.println(resultText);

                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            int index = block.getCornerPoints()[0].y;
                            List<FirebaseVisionText.TextBlock> list;
                            if (map.get(index) == null) {
                                list = new ArrayList<>();
                            } else {
                                list = map.get(index);
                            }

                            if (!processSelectedText(block.getText()).isEmpty()) {
                                list.add(block);
                                map.put(index, list);
                            }

                        }

                        List<Integer> keySet = new ArrayList<>(map.keySet());
                        Collections.sort(keySet);

                        if (!keySet.isEmpty()) {
                            int bottomY = keySet.get(keySet.size() - 1);
                            List<FirebaseVisionText.TextBlock> list = map.get(bottomY);
                            int listSize = list.size();
                            int selectedWordIndex = 0;

                            selectedTextBlock = list.get(0);
                            resultText = selectedTextBlock.getText();
                        }

                        Log.d("TIME", "Processing Finished " + new Date());
                        for (FirebaseVisionText.TextBlock block : result.getTextBlocks()) {
                            String blockText = block.getText();
                            Float blockConfidence = block.getConfidence();
                            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
                            Point[] blockCornerPoints = block.getCornerPoints();
                            Rect blockFrame = block.getBoundingBox();

                            if (selectedTextBlock == null) {
                                selectedTextBlock = block;
                            } else {
                                selectedBlock = selectedTextBlock.getCornerPoints();
                                currentBlock = block.getCornerPoints();
                                Point sp0 = selectedBlock[0], sp1 = selectedBlock[2];
                                Point p0 = currentBlock[0], p1 = currentBlock[2];

                            }


                            for (FirebaseVisionText.Line line : block.getLines()) {
                                String lineText = line.getText();
                                Float lineConfidence = line.getConfidence();
                                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                                Point[] lineCornerPoints = line.getCornerPoints();
                                Rect lineFrame = line.getBoundingBox();
                                for (FirebaseVisionText.Element element : line.getElements()) {
                                    String elementText = element.getText();
                                    Float elementConfidence = element.getConfidence();
                                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                                    Point[] elementCornerPoints = element.getCornerPoints();
                                    Rect elementFrame = element.getBoundingBox();
                                }
                            }
                        }

                        String processedText = processSelectedText(resultText);
                        if (!processedText.isEmpty()) {
                            resultText = processedText;
                        }

                        dataExtractionDTO.setContent(resultText);
                        System.out.println(resultText);
                        contentNotifier.setDataExtractionDTO(dataExtractionDTO);
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(Exception e) {
                                System.out.println("Image Recognition Failed.");
                                Log.d("ERROR", e.getMessage());
                                // Task failed with an exception
                                // ...

                            }
                        });

    }

    private String processSelectedText(String resultText) {
        String result = "";
        String[] wordList = resultText.split("\\W+");
        List<String> meaningFullWords = new ArrayList<>();

        for (String word : wordList) {
            if (!word.isEmpty() && dictionaryWords.contains(word)) {
                meaningFullWords.add(word);
            }
        }

        if (!meaningFullWords.isEmpty()) {
            result = meaningFullWords.get(meaningFullWords.size() - 1);
        }

        return result;
    }
}
