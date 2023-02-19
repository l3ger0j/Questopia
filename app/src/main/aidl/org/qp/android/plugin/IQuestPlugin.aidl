package org.qp.android.plugin;

import org.qp.android.plugin.AsyncCallback;

interface IQuestPlugin {
    // API
    String versionPlugin();
    String titlePlugin();
    String authorPlugin();

    // Plugin part
    void arrayGameData(AsyncCallback callback);
}