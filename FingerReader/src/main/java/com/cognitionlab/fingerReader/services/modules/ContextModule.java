package com.cognitionlab.fingerReader.services.modules;

import android.content.Context;

import dagger.Module;
import dagger.Provides;

@Module
public class ContextModule {

    private Context context;

    public ContextModule(Context context) {
        this.context = context;
    }

    @ApplicationContext
    @Provides
    public Context context() {
        return context.getApplicationContext();
    }
}
