package com.qsp.player.stock.dto;

import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Root(name = "game_list", strict = false)
public class GameList {
    @ElementList(name = "game", inline = true)
    public List<Game> gameList = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameList that = (GameList) o;
        return Objects.equals(gameList, that.gameList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameList);
    }
}
