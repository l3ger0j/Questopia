package org.qp.android.dto;

import java.util.Objects;

public class FileInfo {
    public String icon;
    public String name;
    public int countObject;
    public int dateCreate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (FileInfo) o;
        return Objects.equals(icon, that.icon)
                && Objects.equals(name, that.name)
                && Objects.equals(countObject, that.countObject)
                && Objects.equals(dateCreate, that.dateCreate);
    }
}
