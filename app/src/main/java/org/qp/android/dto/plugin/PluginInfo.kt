package org.qp.android.dto.plugin;

import java.util.Objects;

public class PluginInfo {
    public String version;
    public String title;
    public String author;

    public PluginInfo() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (PluginInfo) o;
        return Objects.equals(version, that.version)
                && Objects.equals(title, that.title)
                && Objects.equals(author, that.author);
    }
}
