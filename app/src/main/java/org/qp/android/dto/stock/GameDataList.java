package org.qp.android.dto.stock;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonRootName;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@JsonRootName(value = "game_list")
public class GameDataList {

    @JsonAlias({"game"})
    public List<GameData> gameDataList = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (GameDataList) o;
        return Objects.equals(gameDataList , that.gameDataList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDataList);
    }
}
