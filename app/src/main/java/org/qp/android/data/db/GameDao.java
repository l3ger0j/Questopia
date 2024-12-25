package org.qp.android.data.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GameDao {

    @Query("SELECT * FROM game")
    List<Game> getAll();

    @Query("SELECT * FROM game ORDER BY " +
            "CASE WHEN :isAsc = 0 THEN title END ASC, " +
            "CASE WHEN :isAsc = 1 THEN title END DESC ")
    List<Game> getAllSortedByName(int isAsc);

    @Query("SELECT * FROM game WHERE listId LIKE 0 ORDER BY " +
            "CASE WHEN :isAsc = 1 THEN title END ASC, " +
            "CASE WHEN :isAsc = 2 THEN title END DESC ")
    List<Game> getIntSortedByName(int isAsc);

    @Query("SELECT * FROM game WHERE id LIKE :id LIMIT 1")
    Game getById(long id);

    @Query("SELECT * FROM game WHERE title LIKE :name LIMIT 1")
    Game getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Game game);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Game> games);

    @Update
    void update(Game game);

    @Delete
    void delete(Game game);

    @Delete
    int deleteList(List<Game> entriesDelete);

}
