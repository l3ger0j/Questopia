package org.qp.android.presentation.di;

import android.content.Context;

import org.qp.android.helpers.service.AudioPlayer;
import org.qp.android.helpers.service.HtmlProcessor;

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

    @Provides
    @Singleton
    public AudioPlayer provideAudioPlayer() {
        return new AudioPlayer();
    }

    @Provides
    @Singleton
    public HtmlProcessor provideHtmlProcessor() {
        return new HtmlProcessor();
    }

}
