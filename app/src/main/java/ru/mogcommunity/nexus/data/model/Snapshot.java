package ru.mogcommunity.rbr_project.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "snapshots",
    foreignKeys = @ForeignKey(
        entity = Project.class,
        parentColumns = "id",
        childColumns = "projectId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("projectId")}
)
public class Snapshot {
    @PrimaryKey
    @NonNull
    private String id;
    @NonNull
    private String projectId;
    private String title;
    private long timestamp;
    private String description;
    private boolean hasError;
    private String errorLog;
    private String imageUrl;
    private String aiAnalysisPlan;
    private String tags;
    private String secondaryImages;

    public Snapshot() {
    }

    @Ignore
    public Snapshot(@NonNull String id, @NonNull String projectId, String title, long timestamp,
                    String description, boolean hasError, String errorLog, String imageUrl, String aiAnalysisPlan) {
        this.id = id;
        this.projectId = projectId;
        this.title = title;
        this.timestamp = timestamp;
        this.description = description;
        this.hasError = hasError;
        this.errorLog = errorLog;
        this.imageUrl = imageUrl;
        this.aiAnalysisPlan = aiAnalysisPlan;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(@NonNull String projectId) {
        this.projectId = projectId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isHasError() {
        return hasError;
    }

    public void setHasError(boolean hasError) {
        this.hasError = hasError;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getAiAnalysisPlan() {
        return aiAnalysisPlan;
    }

    public void setAiAnalysisPlan(String aiAnalysisPlan) {
        this.aiAnalysisPlan = aiAnalysisPlan;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSecondaryImages() {
        return secondaryImages;
    }

    public void setSecondaryImages(String secondaryImages) {
        this.secondaryImages = secondaryImages;
    }
}

