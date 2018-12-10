package com.cognitionlab.fingerReader.services.helpers.observers;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;

import java.util.Observable;

public class ContentNotifier extends Observable {

    private DataExtractionDTO dataExtractionDTO;

    public DataExtractionDTO getDataExtractionDTO() {
        return dataExtractionDTO;
    }

    public ContentNotifier() {

    }

    public void setDataExtractionDTO(DataExtractionDTO dataExtractionDTO) {
        this.dataExtractionDTO = dataExtractionDTO;
        setChanged();
        notifyObservers();
    }

    @Override
    public String toString() {
        return "ContentNotifier{" +
                "dataExtractionDTO=" + dataExtractionDTO +
                '}';
    }
}
