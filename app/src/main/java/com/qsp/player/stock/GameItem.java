package com.qsp.player.stock;

public class GameItem {
    //Parsed
    String game_id = "";
    String list_id = "";
    String author = "";
    String ported_by = "";
    String version = "";
    public String title = "";
    String lang = "";
    String player = "";
    String file_url = "";
    int file_size;
    String desc_url = "";
    String pub_date = "";
    String mod_date = "";
    //Flags
    boolean downloaded;
    boolean checked;
    //Local
    String game_file = "";
}
