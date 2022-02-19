package com.qsp.player.stock;

public class GameStockItemBuilder {
    private final GameStockItem item = new GameStockItem();

    public void setListId(String value) {
        item.listId = value;
    }

    public void setFromXML(String tag, String value) {
        switch (tag) {
            case "id":
                item.id = "id:".concat(value);
                break;
            case "list_id":
                item.listId = value;
                break;
            case "author":
                item.author = value;
                break;
            case "ported_by":
                item.portedBy = value;
                break;
            case "version":
                item.version = value;
                break;
            case "title":
                item.title = value;
                break;
            case "lang":
                item.lang = value;
                break;
            case "player":
                item.player = value;
                break;
            case "file_url":
                item.fileUrl = value;
                break;
            case "file_size":
                item.fileSize = Integer.parseInt(value);
                break;
            case "file_ext":
                item.fileExt = value;
                break;
            case "desc_url":
                item.descUrl = value;
                break;
            case "pub_date":
                item.pubDate = value;
                break;
            case "mod_date":
                item.modDate = value;
                break;
        }
    }

    public GameStockItem build() {
        return item;
    }
}
