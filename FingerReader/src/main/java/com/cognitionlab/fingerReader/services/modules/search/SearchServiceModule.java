package com.cognitionlab.fingerReader.services.modules.search;

import com.cognitionlab.fingerReader.services.SearchService;
import com.cognitionlab.fingerReader.services.impl.SearchServiceImpl;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class SearchServiceModule {

    @Provides
    @Singleton
    SearchService provideSearchService() {
        return new SearchServiceImpl();
    }
}
