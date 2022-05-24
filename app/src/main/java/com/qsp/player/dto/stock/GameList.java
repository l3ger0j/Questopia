package com.qsp.player.dto.stock;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Root(name = "game_list", strict = false)
public class GameList {
    @ElementList(name = "game", inline = true)
    public List<GameData> gameDataList = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameList that = (GameList) o;
        return Objects.equals(gameDataList , that.gameDataList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameDataList);
    }
}
