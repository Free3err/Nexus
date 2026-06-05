package ru.mogcommunity.rbr_project.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import ru.mogcommunity.rbr_project.data.model.Project;

import java.util.List;

@Dao
public interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    LiveData<List<Project>> getAllProjects();

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    Project getProjectById(String id);

    @Query("SELECT * FROM projects")
    List<Project> getAllProjectsSync();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Project project);

    @Update
    void update(Project project);

    @Delete
    void delete(Project project);

    @Query("DELETE FROM projects")
    void deleteAll();
}

