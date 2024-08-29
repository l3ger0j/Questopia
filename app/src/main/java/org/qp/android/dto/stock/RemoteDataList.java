package org.qp.android.dto.stock;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JacksonXmlRootElement(localName = "game_list")
public class RemoteDataList {

    public double version;
    public String id;
    public String title;
    public String text;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<RemoteGameData> game = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (RemoteDataList) o;
        return Objects.equals(game , that.game);
    }

    @Override
    public int hashCode() {
        return Objects.hash(game);
    }

}
