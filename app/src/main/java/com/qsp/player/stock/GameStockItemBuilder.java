package com.qsp.player.stock;

public class GameStockItemBuilder {
    private final GameStockItem item = new GameStockItem();

    public void setListId(String value) {
        item.setListId(value);
    }

    public void setFromXML(String tag, String value) {
        switch (tag) {
            case "id":
                item.setId("id:".concat(value));
                break;
            case "list_id":
                item.setListId(value);
                break;
            case "author":
                item.setAuthor(value);
                break;
            case "ported_by":
                item.setPortedBy(value);
                break;
            case "version":
                item.setVersion(value);
                break;
            case "title":
                item.setTitle(value);
                break;
            case "lang":
                item.setLang(value);
                break;
            case "player":
                item.setPlayer(value);
                break;
            case "file_url":
                item.setFileUrl(value);
                break;
            case "file_size":
                item.setFileSize(Integer.parseInt(value));
                break;
            case "file_ext":
                item.setFileExt(value);
                break;
            case "desc_url":
                item.setDescUrl(value);
                break;
            case "pub_date":
                item.setPubDate(value);
                break;
            case "mod_date":
                item.setModDate(value);
                break;
        }
    }

    public GameStockItem build() {
        return item;
    }
}
