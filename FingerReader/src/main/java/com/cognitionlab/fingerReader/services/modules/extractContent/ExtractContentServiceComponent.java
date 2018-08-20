package com.cognitionlab.fingerReader.services.modules.extractContent;

import com.cognitionlab.fingerReader.services.ExtractContentService;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {ExtractContentServiceModule.class})
public interface ExtractContentServiceComponent {

    ExtractContentService provideExtractContentService();
}
