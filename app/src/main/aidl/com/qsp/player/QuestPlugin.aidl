package com.qsp.player;

interface QuestPlugin {

    int id();
    int version();
    String title();
    String author();
    String pathToImage();
    String fileSize();

}