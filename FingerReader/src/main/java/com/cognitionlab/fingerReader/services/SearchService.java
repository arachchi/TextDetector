package com.cognitionlab.fingerReader.services;

import android.graphics.Bitmap;

import com.cognitionlab.fingerReader.dtos.SearchDTO;

public interface SearchService {

    Bitmap searchText(SearchDTO searchDTO);

}
