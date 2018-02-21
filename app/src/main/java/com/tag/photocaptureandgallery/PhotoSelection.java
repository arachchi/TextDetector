package com.tag.photocaptureandgallery;

public enum PhotoSelection {
    LIB("Choose from Library"), CNX("Cancel");
    String description;

    PhotoSelection(String value) {
        description = value;
    }

    static CharSequence[] getValues() {
        CharSequence[] list = new CharSequence[PhotoSelection.values().length];

        int i = 0;
        for (PhotoSelection value : PhotoSelection.values()) {
            list[i] = value.description;
            i++;
        }
        return list;
    }
}
