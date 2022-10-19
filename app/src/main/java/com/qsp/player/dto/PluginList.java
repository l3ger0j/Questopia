package com.qsp.player.dto;

import java.util.Objects;

public class PluginList {
    public int id;
    public int version;
    public String title;
    public String author;
    public String image;
    public String fileSize;

    public PluginList() {

    }

    public int getFileSize() {
        return (fileSize != null) ? Integer.parseInt(fileSize) : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginList that = (PluginList) o;
        return Objects.equals(id, that.id)
                && Objects.equals(version, that.version)
                && Objects.equals(title, that.title)
                && Objects.equals(author, that.author)
                && Objects.equals(image, that.image);
    }
}
