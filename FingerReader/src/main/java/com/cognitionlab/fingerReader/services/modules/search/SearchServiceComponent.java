package com.cognitionlab.fingerReader.services.modules.search;

import com.cognitionlab.fingerReader.services.SearchService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {SearchServiceModule.class})
public interface SearchServiceComponent {

    SearchService provideSearchService();
}
