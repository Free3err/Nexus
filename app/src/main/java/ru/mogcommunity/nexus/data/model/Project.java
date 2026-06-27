package ru.mogcommunity.rbrproject.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "projects")
public class Project {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String description;
    private long createdAt;
    private int snapshotsCount;

    private String configEnv;
    private String chatSummary;
    private String lastSummarizedMessageId;

    public Project() {
    }

    @Ignore
    public Project(@NonNull String id, String name, String description, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.snapshotsCount = 0;
        this.configEnv = "";
        this.chatSummary = "";
        this.lastSummarizedMessageId = "";
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public int getSnapshotsCount() {
        return snapshotsCount;
    }

    public void setSnapshotsCount(int snapshotsCount) {
        this.snapshotsCount = snapshotsCount;
    }

    public String getConfigEnv() {
        return configEnv;
    }

    public void setConfigEnv(String configEnv) {
        this.configEnv = configEnv;
    }

    public String getChatSummary() {
        return chatSummary;
    }

    public void setChatSummary(String chatSummary) {
        this.chatSummary = chatSummary;
    }

    public String getLastSummarizedMessageId() {
        return lastSummarizedMessageId;
    }

    public void setLastSummarizedMessageId(String lastSummarizedMessageId) {
        this.lastSummarizedMessageId = lastSummarizedMessageId;
    }
}

