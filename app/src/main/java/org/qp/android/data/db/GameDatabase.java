package org.qp.android.data.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Game.class}, version = 1)
public abstract class GameDatabase extends RoomDatabase {
    public abstract GameDao gameDao();
}
