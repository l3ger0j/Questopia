package org.qp.android.di;

import android.content.Context;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public class AppModule {

    @Provides
    @Singleton
    public Context provideContext(@ApplicationContext Context context) {
        return context;
    }

}
