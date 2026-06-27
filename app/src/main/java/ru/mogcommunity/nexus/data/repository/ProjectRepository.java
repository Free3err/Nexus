package ru.mogcommunity.rbrproject.data.repository;

import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.LiveData;

import ru.mogcommunity.rbrproject.data.PreferenceManager;
import ru.mogcommunity.rbrproject.data.local.AppDatabase;
import ru.mogcommunity.rbrproject.data.local.ProjectDao;
import ru.mogcommunity.rbrproject.data.local.SnapshotDao;
import ru.mogcommunity.rbrproject.data.model.Project;
import ru.mogcommunity.rbrproject.data.model.Snapshot;
import ru.mogcommunity.rbrproject.data.remote.FirebaseManager;

import java.util.List;
import javax.inject.Inject;
import android.content.Context;
import dagger.hilt.android.qualifiers.ApplicationContext;

public class ProjectRepository {
    private final ProjectDao projectDao;
    private final SnapshotDao snapshotDao;
    private final ru.mogcommunity.rbrproject.data.local.ChatMessageDao chatMessageDao;
    private final FirebaseManager firebaseManager;
    private final PreferenceManager preferenceManager;
    private final Context context;

    @Inject
    public ProjectRepository(
            ProjectDao projectDao,
            SnapshotDao snapshotDao,
            ru.mogcommunity.rbrproject.data.local.ChatMessageDao chatMessageDao,
            FirebaseManager firebaseManager,
            PreferenceManager preferenceManager,
            @ApplicationContext Context context
    ) {
        this.projectDao = projectDao;
        this.snapshotDao = snapshotDao;
        this.chatMessageDao = chatMessageDao;
        this.firebaseManager = firebaseManager;
        this.preferenceManager = preferenceManager;
        this.context = context;
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

    public LiveData<List<ru.mogcommunity.rbrproject.data.model.ChatMessage>> getChatMessagesForProject(String projectId) {
        return chatMessageDao.getMessagesForProject(projectId);
    }

    public List<ru.mogcommunity.rbrproject.data.model.ChatMessage> getChatMessagesForProjectSync(String projectId) {
        return chatMessageDao.getMessagesForProjectSync(projectId);
    }

    public void insertChatMessage(ru.mogcommunity.rbrproject.data.model.ChatMessage message) {
        AppDatabase.databaseWriteExecutor.execute(() -> chatMessageDao.insert(message));
    }

    public List<Snapshot> getSnapshotsForProjectSync(String projectId) {
        return snapshotDao.getSnapshotsForProjectSync(projectId);
    }

    public void insertProject(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Log.d("rbrprojectRepository", "Inserting project: " + project.getId());
                projectDao.insert(project);
                Log.d("rbrprojectRepository", "Project inserted successfully");
                
                if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                    Log.d("rbrprojectRepository", "Syncing project to cloud");
                    firebaseManager.saveProjectToFirestore(project, task -> {
                        if (task.isSuccessful()) {
                            Log.d("rbrprojectRepository", "Project synced to cloud successfully");
                        } else {
                            Log.e("rbrprojectRepository", "Failed to sync project to cloud", task.getException());
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("rbrprojectRepository", "Error inserting project", e);
            }
        });
     }

    public void updateProjectDetails(String projectId, String name, String description, String configEnv) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Log.d("rbrprojectRepository", "Updating project details: " + projectId);
                Project project = projectDao.getProjectById(projectId);
                if (project != null) {
                    project.setName(name);
                    project.setDescription(description);
                    project.setConfigEnv(configEnv);
                    projectDao.update(project);
                    if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                        firebaseManager.saveProjectToFirestore(project, null);
                    }
                }
            } catch (Exception e) {
                Log.e("rbrprojectRepository", "Error updating project details", e);
            }
        });
    }

    public void updateProject(Project project) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                Log.d("rbrprojectRepository", "Updating project: " + project.getId());
                projectDao.update(project);
                if (preferenceManager.isCloudSyncEnabled() && firebaseManager.isUserLoggedIn()) {
                    firebaseManager.saveProjectToFirestore(project, null);
                }
            } catch (Exception e) {
                Log.e("rbrprojectRepository", "Error updating project", e);
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

    private Uri copyToInternalStorage(Context context, Uri uri, String destFileName) {
        if (uri == null || !"content".equals(uri.getScheme())) {
            return uri;
        }
        try {
            java.io.File dir = new java.io.File(context.getFilesDir(), "snapshots");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            java.io.File destFile = new java.io.File(dir, destFileName);
            try (java.io.InputStream is = context.getContentResolver().openInputStream(uri);
                 java.io.OutputStream os = new java.io.FileOutputStream(destFile)) {
                if (is != null) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        os.write(buffer, 0, read);
                    }
                    os.flush();
                    return Uri.fromFile(destFile);
                }
            }
        } catch (Exception e) {
            Log.e("rbrprojectRepository", "Error copying image to internal storage", e);
        }
        return uri;
    }

    public void insertSnapshot(Snapshot snapshot, Uri localImageUri, List<Uri> secondaryUris) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            
            Uri copiedMainUri = localImageUri;
            if (localImageUri != null) {
                copiedMainUri = copyToInternalStorage(context, localImageUri, "main_" + snapshot.getId() + ".jpg");
                snapshot.setImageUrl(copiedMainUri.toString());
            }

            List<Uri> copiedSecondaryUris = new java.util.ArrayList<>();
            if (secondaryUris != null) {
                for (int i = 0; i < secondaryUris.size(); i++) {
                    Uri copiedSec = copyToInternalStorage(context, secondaryUris.get(i), "sec_" + snapshot.getId() + "_" + i + ".jpg");
                    copiedSecondaryUris.add(copiedSec);
                }
            }

            if (!copiedSecondaryUris.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < copiedSecondaryUris.size(); i++) {
                    sb.append(copiedSecondaryUris.get(i).toString());
                    if (i < copiedSecondaryUris.size() - 1) {
                        sb.append(",");
                    }
                }
                snapshot.setSecondaryImages(sb.toString());
            }

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
                final Uri finalMainUri = copiedMainUri;
                if (finalMainUri != null) {
                    firebaseManager.uploadSnapshotImage(snapshot.getProjectId(), snapshot.getId(), finalMainUri, task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String downloadUrl = task.getResult().toString();
                            snapshot.setImageUrl(downloadUrl);
                        }
                        uploadSecondaryImagesAndSave(snapshot, copiedSecondaryUris);
                    });
                } else {
                    uploadSecondaryImagesAndSave(snapshot, copiedSecondaryUris);
                }
            }
        });
    }

    private void uploadSecondaryImagesAndSave(Snapshot snapshot, List<Uri> secondaryUris) {
        if (secondaryUris == null || secondaryUris.isEmpty()) {
            AppDatabase.databaseWriteExecutor.execute(() -> {
                snapshotDao.insert(snapshot);
                firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
            });
            return;
        }

        final int total = secondaryUris.size();
        final String[] uploadedUrls = new String[total];
        final java.util.concurrent.atomic.AtomicInteger finished = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < total; i++) {
            final int index = i;
            Uri uri = secondaryUris.get(i);
            String subId = snapshot.getId() + "_sec_" + index;
            firebaseManager.uploadSnapshotImage(snapshot.getProjectId(), subId, uri, task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    uploadedUrls[index] = task.getResult().toString();
                } else {
                    uploadedUrls[index] = uri.toString();
                }
                if (finished.incrementAndGet() == total) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < total; j++) {
                        sb.append(uploadedUrls[j]);
                        if (j < total - 1) {
                            sb.append(",");
                        }
                    }
                    snapshot.setSecondaryImages(sb.toString());
                    AppDatabase.databaseWriteExecutor.execute(() -> {
                        snapshotDao.insert(snapshot);
                        firebaseManager.saveSnapshotToFirestore(snapshot.getProjectId(), snapshot, null);
                    });
                }
            });
        }
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

    private void mainThreadNotifySuccess(java.lang.ref.WeakReference<OnSyncCompleteListener> weakListener) {
        OnSyncCompleteListener listener = weakListener.get();
        if (listener == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(listener::onSyncSuccess);
    }

    private void mainThreadNotifyFailed(java.lang.ref.WeakReference<OnSyncCompleteListener> weakListener, String error) {
        OnSyncCompleteListener listener = weakListener.get();
        if (listener == null) return;
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> listener.onSyncFailed(error));
    }

    public void syncWithCloud(OnSyncCompleteListener listener) {
        java.lang.ref.WeakReference<OnSyncCompleteListener> weakListener = new java.lang.ref.WeakReference<>(listener);
        if (!firebaseManager.isUserLoggedIn()) {
            mainThreadNotifyFailed(weakListener, "Пользователь не авторизован");
            return;
        }

        AppDatabase.databaseWriteExecutor.execute(() -> {
            try {
                final List<Project> localProjects = projectDao.getAllProjectsSync();
                final List<Snapshot> localSnapshots = snapshotDao.getAllSnapshotsSync();

                firebaseManager.fetchProjectsFromFirestore(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        mainThreadNotifyFailed(weakListener, task.getException() != null ? task.getException().getMessage() : "Ошибка загрузки проектов");
                        return;
                    }

                    List<Project> remoteProjects = task.getResult().toObjects(Project.class);
                    final int totalProjects = remoteProjects.size();
                    
                    if (totalProjects == 0) {
                        uploadLocalData(localProjects, localSnapshots, weakListener);
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
                                        uploadLocalData(localProjects, localSnapshots, weakListener);
                                    }
                                });
                            } else {
                                if (pendingQueries.decrementAndGet() == 0) {
                                    uploadLocalData(localProjects, localSnapshots, weakListener);
                                }
                            }
                        });
                    }
                });
            } catch (Exception e) {
                mainThreadNotifyFailed(weakListener, e.getMessage());
            }
        });
    }

    private void uploadLocalData(List<Project> localProjects, List<Snapshot> localSnapshots, java.lang.ref.WeakReference<OnSyncCompleteListener> weakListener) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            for (Project p : localProjects) {
                firebaseManager.saveProjectToFirestore(p, null);
            }
            for (Snapshot s : localSnapshots) {
                firebaseManager.saveSnapshotToFirestore(s.getProjectId(), s, null);
            }
            mainThreadNotifySuccess(weakListener);
        });
    }
}

