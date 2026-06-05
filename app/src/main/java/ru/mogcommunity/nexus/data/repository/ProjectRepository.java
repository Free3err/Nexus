package ru.mogcommunity.rbr_project.data.repository;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.data.local.AppDatabase;
import ru.mogcommunity.rbr_project.data.local.ProjectDao;
import ru.mogcommunity.rbr_project.data.local.SnapshotDao;
import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.data.remote.FirebaseManager;

import java.util.List;

public class ProjectRepository {
    private final ProjectDao projectDao;
    private final SnapshotDao snapshotDao;
    private final ru.mogcommunity.rbr_project.data.local.ChatMessageDao chatMessageDao;
    private final FirebaseManager firebaseManager;
    private final PreferenceManager preferenceManager;

    public ProjectRepository(Context context) {
        AppDatabase db = AppDatabase.getDatabase(context);
        projectDao = db.projectDao();
        snapshotDao = db.snapshotDao();
        chatMessageDao = db.chatMessageDao();
        firebaseManager = FirebaseManager.getInstance();
        preferenceManager = PreferenceManager.getInstance(context);
    }

    public LiveData<List<Project>> getAllProjects() {
        return projectDao.getAllProjects();
    }

    public Project getProjectByIdSync(String id) {
        return projectDao.getProjectById(id);
    }

    public LiveData<List<Snapshot>> getSnapshotsForProject(String projectId) {
        return snapshotDao.getSnapshotsForProject(projectId);
    }

    public LiveData<List<Snapshot>> getAllSnapshotsWithImages() {
        return snapshotDao.getAllSnapshotsWithImages();
    }

    public Snapshot getLastSuccessfulSnapshot(String projectId) {
        return snapshotDao.getLastSuccessfulSnapshot(projectId);
    }

    public LiveData<List<ru.mogcommunity.rbr_project.data.model.ChatMessage>> getChatMessagesForProject(String projectId) {
        return chatMessageDao.getMessagesForProject(projectId);
    }

    public List<ru.mogcommunity.rbr_project.data.model.ChatMessage> getChatMessagesForProjectSync(String projectId) {
        return chatMessageDao.getMessagesForProjectSync(projectId);
    }

    public void insertChatMessage(ru.mogcommunity.rbr_project.data.model.ChatMessage message) {
        AppDatabase.databaseWriteExecutor.execute(() -> chatMessageDao.insert(message));
    }

    public List<Snapshot> getSnapshotsForProjectSync(String projectId) {
        return snapshotDao.getSnapshotsForProjectSync(projectId);
    }

    public void insertProject(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Log.d("RBR_ProjectRepository", "Inserting project: " + project.getId());
                projectDao.insert(project);
                Log.d("RBR_ProjectRepository", "Project inserted successfully");
                
                if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                    Log.d("RBR_ProjectRepository", "Syncing project to cloud");
                    firebaseManager.saveProjectToFirestore(project, task -> {
                        if (task.isSuccessful()) {
                            Log.d("RBR_ProjectRepository", "Project synced to cloud successfully");
                        } else {
                            Log.e("RBR_ProjectRepository", "Failed to sync project to cloud", task.getException());
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("RBR_ProjectRepository", "Error inserting project", e);
            }
        });
    }

    public void deleteProject(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            projectDao.delete(project);
            if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                firebaseManager.deleteProjectFromFirestore(project.getId(), null);
            }
        });
    }

    public void insertSnapshot(Snapshot snapshot, Uri localImageUri) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            snapshotDao.insert(snapshot);

            Project project = projectDao.getProjectById(snapshot.getProjectId());
            if (project != null) {
                project.setSnapshotsCount(project.getSnapshotsCount() + 1);
                projectDao.update(project);

                if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                    firebaseManager.saveProjectToFirestore(project, null);
                }
            }

            if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                if (localImageUri != null) {
                    firebaseManager.uploadSnapshotImage(snapshot.getProjectId(), snapshot.getId(), localImageUri, task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String downloadUrl = task.getResult().toString();
                            snapshot.setImageUrl(downloadUrl);

                            AppDatabase.databaseWriteExecutor.execute(() -> {
                                snapshotDao.insert(snapshot);
                                firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
                            });
                        } else {
                            firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
                        }
                    });
                } else {
                    firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
                }
            }
        });
    }

    public void updateSnapshot(Snapshot snapshot) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            snapshotDao.update(snapshot);
            if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
            }
        });
    }

    public void deleteSnapshot(Snapshot snapshot) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            snapshotDao.delete(snapshot);

            Project project = projectDao.getProjectById(snapshot.getProjectId());
            if (project != null && project.getSnapshotsCount() > 0) {
                project.setSnapshotsCount(project.getSnapshotsCount() - 1);
                projectDao.update(project);
                if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                    firebaseManager.saveProjectToFirestore(project, null);
                }
            }

            if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                firebaseManager.deleteSnapshotFromFirestore(snapshot.getProjectId(), snapshot.getId(), null);
            }
        });
    }

    public interface OnSyncCompleteListener {
        void onSyncSuccess();
        void onSyncFailed(String error);
    }

    private void mainThreadNotifySuccess(OnSyncCompleteListener listener) {
        if (listener == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(listener::onSyncSuccess);
    }

    private void mainThreadNotifyFailed(OnSyncCompleteListener listener, String error) {
        if (listener == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onSyncFailed(error));
    }

    public void syncWithCloud(OnSyncCompleteListener listener) {
        if (!firebaseManager.isUserLoggedIn()) {
            mainThreadNotifyFailed(listener, "Пользователь не авторизован");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                final List<Project> localProjects = projectDao.getAllProjectsSync();
                final List<Snapshot> localSnapshots = snapshotDao.getAllSnapshotsSync();

                firebaseManager.fetchProjectsFromFirestore(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        mainThreadNotifyFailed(listener, task.getException() != null ? task.getException().getMessage() : "Ошибка загрузки проектов");
                        return;
                    }

                    List<Project> remoteProjects = task.getResult().toObjects(Project.class);
                    final int totalProjects = remoteProjects.size();
                    
                    if (totalProjects == 0) {
                        uploadLocalData(localProjects, localSnapshots, listener);
                        return;
                    }

                    final java.util.concurrent.atomic.AtomicInteger pendingQueries = new java.util.concurrent.atomic.AtomicInteger(totalProjects);

                    for (Project rp : remoteProjects) {
                        AppDatabase.databaseWriteExecutor.execute(() -> {
                            projectDao.insert(rp);
                        });

                        firebaseManager.fetchSnapshotsFromFirestore(rp.getId(), sTask -> {
                            if (sTask.isSuccessful() && sTask.getResult() != null) {
                                List<Snapshot> remoteSnapshots = sTask.getResult().toObjects(Snapshot.class);
                                AppDatabase.databaseWriteExecutor.execute(() -> {
                                    for (Snapshot rs : remoteSnapshots) {
                                        snapshotDao.insert(rs);
                                    }
                                    if (pendingQueries.decrementAndGet() == 0) {
                                        uploadLocalData(localProjects, localSnapshots, listener);
                                    }
                                });
                            } else {
                                if (pendingQueries.decrementAndGet() == 0) {
                                    uploadLocalData(localProjects, localSnapshots, listener);
                                }
                            }
                        });
                    }
                });
            } catch (Exception e) {
                mainThreadNotifyFailed(listener, e.getMessage());
            }
        });
    }

    private void uploadLocalData(List<Project> localProjects, List<Snapshot> localSnapshots, OnSyncCompleteListener listener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (Project p : localProjects) {
                firebaseManager.saveProjectToFirestore(p, null);
            }
            for (Snapshot s : localSnapshots) {
                firebaseManager.saveSnapshotToFirestore(s.getProjectId(), s, null);
            }
            mainThreadNotifySuccess(listener);
        });
    }
}

