package ru.mogcommunity.rbr_project.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.data.remote.FirebaseManager;

public class AuthViewModel extends AndroidViewModel {
    private final FirebaseManager firebaseManager;
    private final PreferenceManager preferenceManager;

    private final MutableLiveData<Boolean> isLoggedIn = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);

    public AuthViewModel(@NonNull Application application) {
        super(application);
        firebaseManager = FirebaseManager.getInstance();
        preferenceManager = PreferenceManager.getInstance(application);
        checkLoginStatus();
    }

    public LiveData<Boolean> getIsLoggedIn() {
        return isLoggedIn;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void checkLoginStatus() {
        boolean firebaseLoggedIn = firebaseManager.isUserLoggedIn();
        String localUserId = preferenceManager.getUserId();
        boolean loggedIn = firebaseLoggedIn || !localUserId.isEmpty();
        Log.d("RBR_AuthViewModel", "checkLoginStatus: firebaseLoggedIn=" + firebaseLoggedIn + ", localUserId=" + localUserId + ", loggedIn=" + loggedIn);
        isLoggedIn.setValue(loggedIn);
        if (firebaseLoggedIn && localUserId.isEmpty()) {
            String newUid = firebaseManager.getCurrentUserId();
            Log.d("RBR_AuthViewModel", "Syncing firebase UID to preferenceManager: " + newUid);
            preferenceManager.setUserId(newUid);
        }
    }

    public void signInAnonymously() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        firebaseManager.signInAnonymously(task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                String userId = firebaseManager.getCurrentUserId();
                preferenceManager.setUserId(userId);
                preferenceManager.setCloudSyncEnabled(false);
                isLoggedIn.setValue(true);
            } else {
                preferenceManager.setUserId("local_guest_user");
                preferenceManager.setCloudSyncEnabled(false);
                isLoggedIn.setValue(true);
            }
        });
    }

    public void signInWithEmailAndPassword(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Email and password cannot be empty");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        firebaseManager.signInWithEmailAndPassword(email, password, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                String userId = firebaseManager.getCurrentUserId();
                preferenceManager.setUserId(userId);
                preferenceManager.setCloudSyncEnabled(true);
                isLoggedIn.setValue(true);
            } else {
                Exception ex = task.getException();
                String error = ex != null ? ex.getLocalizedMessage() : "Ошибка авторизации";
                Log.e("RBR_AuthViewModel", "Login failed: ", ex);
                
                if (ex instanceof FirebaseAuthInvalidCredentialsException || ex instanceof FirebaseAuthInvalidUserException) {
                    errorMessage.setValue("Неверный email или пароль.");
                } else {
                    errorMessage.setValue("Ошибка входа: " + error);
                }
            }
        });
    }

    public void signUpWithEmailAndPassword(String email, String password) {
        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            errorMessage.setValue("Email and password cannot be empty");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);

        firebaseManager.signUpWithEmailAndPassword(email, password, task -> {
            isLoading.setValue(false);
            if (task.isSuccessful() && task.getResult() != null) {
                String userId = firebaseManager.getCurrentUserId();
                preferenceManager.setUserId(userId);
                preferenceManager.setCloudSyncEnabled(true);
                isLoggedIn.setValue(true);
            } else {
                Exception ex = task.getException();
                String error = ex != null ? ex.getLocalizedMessage() : "Ошибка регистрации";
                Log.e("RBR_AuthViewModel", "Signup failed: ", ex);

                if (ex instanceof FirebaseAuthInvalidCredentialsException || ex instanceof FirebaseAuthWeakPasswordException) {
                    errorMessage.setValue("Недопустимый формат email или слишком слабый пароль (минимум 6 символов).");
                } else if (ex instanceof FirebaseAuthUserCollisionException) {
                    errorMessage.setValue("Пользователь с таким email уже зарегистрирован.");
                } else {
                    errorMessage.setValue("Ошибка регистрации: " + error);
                }
            }
        });
    }

    public void signOut() {
        Log.d("RBR_AuthViewModel", "signOut called");
        firebaseManager.signOut();
        preferenceManager.setUserId("");
        preferenceManager.setCloudSyncEnabled(false);
        isLoggedIn.setValue(false);
    }
}

