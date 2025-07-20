package org.qp.android.data.di;

import android.content.Context;

import org.qp.android.data.repository.LocalRepositoryImpl;
import org.qp.android.data.repository.PluginRepositoryImpl;
import org.qp.android.data.repository.RemoteRepositoryImpl;
import org.qp.android.data.source.database.GameDao;
import org.qp.android.domain.repository.LocalRepository;
import org.qp.android.domain.repository.PluginRepository;
import org.qp.android.domain.repository.RemoteRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@InstallIn(SingletonComponent.class)
@Module
public final class RepositoryModule {

    @Provides
    @Singleton
    public LocalRepository provideLocalRepository(@ApplicationContext Context context, GameDao dao) {
        return new LocalRepositoryImpl(context, dao);
    }

    @Provides
    @Singleton
    public PluginRepository providePluginRepository() {
        return new PluginRepositoryImpl();
    }

    @Provides
    @Singleton
    public RemoteRepository provideRemoteRepository() {
        return new RemoteRepositoryImpl();
    }

}
