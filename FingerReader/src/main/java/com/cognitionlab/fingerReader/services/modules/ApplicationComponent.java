package com.cognitionlab.fingerReader.services.modules;

import com.cognitionlab.fingerReader.services.ProcessingService;
import com.cognitionlab.fingerReader.services.modules.processing.ProcessingServiceModule;

import dagger.Component;

@Component(modules = {ProcessingServiceModule.class})
@ApplicationScope
public interface ApplicationComponent {

    ProcessingService getProcessingService();

}
