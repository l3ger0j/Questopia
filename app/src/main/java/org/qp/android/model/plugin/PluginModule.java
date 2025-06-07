package org.qp.android.model.plugin;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class PluginModule {

    @Provides
    @Singleton
    public PluginClient providePluginClient() {
        return new PluginClient();
    }

}
