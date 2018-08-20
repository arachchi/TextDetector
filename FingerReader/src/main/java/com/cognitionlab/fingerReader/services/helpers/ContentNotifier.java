package com.cognitionlab.fingerReader.services.helpers;

import com.cognitionlab.fingerReader.dtos.DataExtractionDTO;

import java.util.Observable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter(AccessLevel.PUBLIC)
public class ContentNotifier extends Observable {

    private DataExtractionDTO dataExtractionDTO;

    public ContentNotifier() {

    }

    public void setDataExtractionDTO(DataExtractionDTO dataExtractionDTO) {
        this.dataExtractionDTO = dataExtractionDTO;
        setChanged();
        notifyObservers();
    }
}
