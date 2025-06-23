package com.example.somnium;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.util.Date;

@Entity(tableName = "sleep_records")
@TypeConverters({DateConverter.class})
public class SleepRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String notes;
    public Date createdAt;
    public int userId;

    public SleepRecord() {}

    public SleepRecord(String title, String notes, Date createdAt, int userId) {
        this.title = title;
        this.notes = notes;
        this.createdAt = createdAt;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}