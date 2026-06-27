package ru.mogcommunity.rbrproject.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import ru.mogcommunity.rbrproject.data.model.ChatMessage;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesForProject(String projectId);

    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForProjectSync(String projectId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ChatMessage chatMessage);

    @Query("DELETE FROM chat_messages WHERE projectId = :projectId")
    void deleteProjectMessages(String projectId);
}
