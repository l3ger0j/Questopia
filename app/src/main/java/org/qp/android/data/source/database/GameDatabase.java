package org.qp.android.data.source.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import org.qp.android.data.source.database.model.Game;

@Database(entities = {Game.class}, version = 1)
public abstract class GameDatabase extends RoomDatabase {
    public abstract GameDao gameDao();
}
