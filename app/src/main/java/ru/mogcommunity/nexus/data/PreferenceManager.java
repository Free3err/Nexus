package ru.mogcommunity.rbrproject.data;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferenceManager {
    private static final String PREF_NAME = "rollandback_prefs";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    private static final String KEY_CLOUD_SYNC = "cloud_sync";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DYNAMIC_COLORS = "dynamic_colors";
    private static final String KEY_SELECTED_MODEL = "selected_model";
    private static final String KEY_QWEN_15B_DOWNLOADED = "qwen_15b_downloaded";
    private static final String KEY_QWEN_4B_DOWNLOADED = "qwen_4b_downloaded";
    private static final String KEY_LAST_PROJECT_ID = "last_project_id";

    private final SharedPreferences sharedPreferences;
    private static PreferenceManager instance;

    private PreferenceManager(Context context) {
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferenceManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
        return instance;
    }

    public String getGeminiApiKey() {
        return sharedPreferences.getString(KEY_GEMINI_API_KEY, "");
    }

    public void setGeminiApiKey(String apiKey) {
        sharedPreferences.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply();
    }

    public boolean isCloudSyncEnabled() {
        return sharedPreferences.getBoolean(KEY_CLOUD_SYNC, false);
    }

    public void setCloudSyncEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_CLOUD_SYNC, enabled).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    public void setUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public boolean isDynamicColorsEnabled() {
        return sharedPreferences.getBoolean(KEY_DYNAMIC_COLORS, false);
    }

    public void setDynamicColorsEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_DYNAMIC_COLORS, enabled).apply();
    }

    public String getSelectedModel() {
        return sharedPreferences.getString(KEY_SELECTED_MODEL, "gemini");
    }

    public void setSelectedModel(String model) {
        sharedPreferences.edit().putString(KEY_SELECTED_MODEL, model).apply();
    }

    public boolean isModelDownloaded(String modelName) {
        if ("qwen_1.5b".equals(modelName)) {
            return sharedPreferences.getBoolean(KEY_QWEN_15B_DOWNLOADED, false);
        } else if ("qwen_4b".equals(modelName)) {
            return sharedPreferences.getBoolean(KEY_QWEN_4B_DOWNLOADED, false);
        }
        return false;
    }

    public void setModelDownloaded(String modelName, boolean downloaded) {
        if ("qwen_1.5b".equals(modelName)) {
            sharedPreferences.edit().putBoolean(KEY_QWEN_15B_DOWNLOADED, downloaded).apply();
        } else if ("qwen_4b".equals(modelName)) {
            sharedPreferences.edit().putBoolean(KEY_QWEN_4B_DOWNLOADED, downloaded).apply();
        }
    }

    public long getDownloadId(String modelName) {
        return sharedPreferences.getLong("download_id_" + modelName, -1);
    }

    public void setDownloadId(String modelName, long id) {
        sharedPreferences.edit().putLong("download_id_" + modelName, id).apply();
    }

    public float getFontScale() {
        return sharedPreferences.getFloat("font_scale", 0.9f);
    }

    public void setFontScale(float scale) {
        sharedPreferences.edit().putFloat("font_scale", scale).apply();
    }

    public String getLastProjectId() {
        return sharedPreferences.getString(KEY_LAST_PROJECT_ID, "");
    }

    public void setLastProjectId(String projectId) {
        sharedPreferences.edit().putString(KEY_LAST_PROJECT_ID, projectId).apply();
    }
}

