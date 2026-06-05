package ru.mogcommunity.rbr_project.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import ru.mogcommunity.rbr_project.data.model.Snapshot;

import java.util.List;

@Dao
public interface SnapshotDao {
    @Query("SELECT * FROM snapshots WHERE projectId = :projectId ORDER BY timestamp DESC")
    LiveData<List<Snapshot>> getSnapshotsForProject(String projectId);

    @Query("SELECT * FROM snapshots WHERE imageUrl IS NOT NULL AND imageUrl != '' ORDER BY timestamp DESC")
    LiveData<List<Snapshot>> getAllSnapshotsWithImages();

    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    Snapshot getSnapshotById(String id);

    @Query("SELECT * FROM snapshots WHERE projectId = :projectId AND hasError = 0 ORDER BY timestamp DESC LIMIT 1")
    Snapshot getLastSuccessfulSnapshot(String projectId);

    @Query("SELECT * FROM snapshots WHERE projectId = :projectId ORDER BY timestamp ASC")
    List<Snapshot> getSnapshotsForProjectSync(String projectId);

    @Query("SELECT * FROM snapshots")
    List<Snapshot> getAllSnapshotsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Snapshot snapshot);

    @Update
    void update(Snapshot snapshot);

    @Delete
    void delete(Snapshot snapshot);

    @Query("DELETE FROM snapshots")
    void deleteAll();
}

