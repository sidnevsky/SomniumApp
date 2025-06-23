package com.example.somnium;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.Date;
import java.util.List;

@Dao
public interface SleepRecordDao {

    @Insert
    long insert(SleepRecord record);

    @Query("SELECT * FROM sleep_records WHERE userId = :userId ORDER BY createdAt DESC")
    List<SleepRecord> getAllByUser(int userId);

    @Query("DELETE FROM sleep_records WHERE id = :id")
    int delete(int id);

    @Query("SELECT * FROM sleep_records WHERE id = :id LIMIT 1")
    SleepRecord getById(int id);

    @Query("SELECT COUNT(*) FROM sleep_records WHERE userId = :userId")
    int getRecordsCount(int userId);

    @Query("SELECT * FROM sleep_records WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    List<SleepRecord> getRecentRecords(int userId, int limit);

    @Query("SELECT * FROM sleep_records " +
            "WHERE userId = :userId " +
            "AND (LOWER(title) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(notes) LIKE '%' || LOWER(:query) || '%') " +
            "ORDER BY createdAt DESC")
    List<SleepRecord> searchRecords(int userId, String query);

    @Update
    int update(SleepRecord record);

    @Query("UPDATE sleep_records " +
            "SET title = :title, notes = :notes, createdAt = :createdAt " +
            "WHERE id = :id")
    int updateRecord(int id, String title, String notes, Date createdAt);

    @Query("SELECT * FROM sleep_records " +
            "WHERE userId = :userId " +
            "AND createdAt >= :fromDate " +
            "ORDER BY createdAt DESC")
    List<SleepRecord> getRecordsAfterDate(int userId, Date fromDate);

    @Query("SELECT * FROM sleep_records " +
            "WHERE userId = :userId " +
            "AND date(createdAt/1000, 'unixepoch') = date(:date/1000, 'unixepoch') " +
            "ORDER BY createdAt DESC")
    List<SleepRecord> getRecordsByDate(int userId, Date date);

    @Query("SELECT * FROM sleep_records " +
            "WHERE userId = :userId " +
            "AND createdAt BETWEEN :from AND :to " +
            "ORDER BY createdAt DESC")
    List<SleepRecord> getRecordsBetweenDates(int userId, Date from, Date to);

    @Query("SELECT DISTINCT date(createdAt/1000, 'unixepoch') as recordDate " +
            "FROM sleep_records " +
            "WHERE userId = :userId " +
            "ORDER BY recordDate DESC")
    List<Date> getUniqueRecordDates(int userId);
}