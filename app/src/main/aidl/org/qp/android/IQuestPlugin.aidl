package org.qp.android;

interface IQuestPlugin {

    int id();
    int version();
    String title();
    String author();
    String pathToImage();
    String fileSize();

    void showQSPFile (String pathToQSPFile);
}