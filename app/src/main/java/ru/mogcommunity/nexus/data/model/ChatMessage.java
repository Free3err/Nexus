package ru.mogcommunity.rbrproject.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "chat_messages",
    foreignKeys = @ForeignKey(
        entity = Project.class,
        parentColumns = "id",
        childColumns = "projectId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("projectId")}
)
public class ChatMessage {
    @PrimaryKey
    @NonNull
    private String id;
    
    @NonNull
    private String projectId;
    
    private String sender;
    private String text;
    private long timestamp;

    public ChatMessage() {
    }

    @Ignore
    public ChatMessage(@NonNull String id, @NonNull String projectId, String sender, String text, long timestamp) {
        this.id = id;
        this.projectId = projectId;
        this.sender = sender;
        this.text = text;
        this.timestamp = timestamp;
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

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
