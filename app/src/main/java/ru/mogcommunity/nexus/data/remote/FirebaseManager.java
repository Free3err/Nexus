package ru.mogcommunity.rbr_project.data.remote;

import android.net.Uri;

import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final FirebaseStorage storage;

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signInAnonymously(OnCompleteListener<AuthResult> listener) {
        auth.signInAnonymously().addOnCompleteListener(listener);
    }

    public void signInWithEmailAndPassword(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signUpWithEmailAndPassword(String email, String password, OnCompleteListener<AuthResult> listener) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(listener);
    }

    public void signOut() {
        auth.signOut();
    }

    public void saveProjectToFirestore(Project project, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .collection("projects")
                .document(project.getId())
                .set(project)
                .addOnCompleteListener(listener);
    }

    public void deleteProjectFromFirestore(String projectId, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .collection("projects")
                .document(projectId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void saveSnapshotToFirestore(String projectId, Snapshot snapshot, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .collection("projects")
                .document(projectId)
                .collection("snapshots")
                .document(snapshot.getId())
                .set(snapshot)
                .addOnCompleteListener(listener);
    }

    public void deleteSnapshotFromFirestore(String projectId, String snapshotId, OnCompleteListener<Void> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;

        db.collection("users")
                .document(userId)
                .collection("projects")
                .document(projectId)
                .collection("snapshots")
                .document(snapshotId)
                .delete()
                .addOnCompleteListener(listener);
    }

    public void uploadSnapshotImage(String projectId, String snapshotId, Uri localImageUri, OnCompleteListener<Uri> listener) {
        String userId = getCurrentUserId();
        if (userId == null || localImageUri == null) return;

        StorageReference fileRef = storage.getReference()
                .child("users/" + userId + "/projects/" + projectId + "/snapshots/" + snapshotId + ".jpg");

        java.lang.ref.WeakReference<OnCompleteListener<Uri>> weakListener = new java.lang.ref.WeakReference<>(listener);

        UploadTask uploadTask = fileRef.putFile(localImageUri);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                if (task.getException() != null) {
                    throw task.getException();
                }
            }
            return fileRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            OnCompleteListener<Uri> originalListener = weakListener.get();
            if (originalListener != null) {
                originalListener.onComplete(task);
            }
        });
     }

    public void fetchProjectsFromFirestore(OnCompleteListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        db.collection("users")
                .document(userId)
                .collection("projects")
                .get()
                .addOnCompleteListener(listener);
    }

    public void fetchSnapshotsFromFirestore(String projectId, OnCompleteListener<QuerySnapshot> listener) {
        String userId = getCurrentUserId();
        if (userId == null) return;
        db.collection("users")
                .document(userId)
                .collection("projects")
                .document(projectId)
                .collection("snapshots")
                .get()
                .addOnCompleteListener(listener);
    }
}

