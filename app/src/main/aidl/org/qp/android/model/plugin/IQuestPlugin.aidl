package org.qp.android.model.plugin;

interface IQuestPlugin {

    int id();
    int version();
    String title();
    String author();
    String pathToImage();
    String fileSize();

}