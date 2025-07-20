package org.qp.android.data.di;

import android.content.Context;

import androidx.room.Room;

import org.qp.android.data.source.database.GameDao;
import org.qp.android.data.source.database.GameDatabase;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public final class DatabaseModule {

    @Provides
    @Singleton
    public GameDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, GameDatabase.class, "games").build();
    }

    @Provides
    @Singleton
    public GameDao provideGameDAO(GameDatabase database) {
        return database.gameDao();
    }

}
