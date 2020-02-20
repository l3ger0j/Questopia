package com.qsp.player.stock;

import androidx.documentfile.provider.DocumentFile;

import java.util.List;

class GameStockItem {
    String gameId = "";
    String listId = "";
    String author = "";
    String portedBy = "";
    String version = "";
    String title = "";
    String lang = "";
    String player = "";
    String fileUrl = "";
    int fileSize;
    String descUrl = "";
    String pubDate = "";
    String modDate = "";
    boolean downloaded;
    DocumentFile gameDir;
    List<DocumentFile> gameFiles;
}
