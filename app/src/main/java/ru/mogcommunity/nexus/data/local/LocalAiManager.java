package ru.mogcommunity.rbrproject.data.local;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.mediapipe.tasks.genai.llminference.LlmInference;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalAiManager {
    private static final String TAG = "LocalAiManager";
    private static LocalAiManager instance;
    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private LlmInference llmInference;
    private String loadedModelType;

    public interface InferenceCallback {
        void onSuccess(String output);
        void onError(String error);
    }

    private LocalAiManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized LocalAiManager getInstance(Context context) {
        if (instance == null) {
            instance = new LocalAiManager(context);
        }
        return instance;
    }

    public File getModelFile(String modelType) {
        File modelsDir = new File(context.getExternalFilesDir(null), "models");
        if (!modelsDir.exists()) {
            return null;
        }

        if ("qwen_1.5b".equals(modelType)) {
            String[] names = {"qwen15.task", "qwen15.bin", "qwen2.5-1.5b-instruct-android-gpu-int4.bin", "qwen2.5-1.5b-instruct-android-cpu-int4.bin"};
            for (String name : names) {
                File file = new File(modelsDir, name);
                if (file.exists() && file.isFile() && file.length() > 0) {
                    return file;
                }
            }
        }
        return null;
    }

    public boolean isModelAvailable(String modelType) {
        return getModelFile(modelType) != null;
    }

    public synchronized void close() {
        if (llmInference != null) {
            Log.d(TAG, "Closing LlmInference for model: " + loadedModelType);
            try {
                llmInference.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing LlmInference", e);
            }
            llmInference = null;
            loadedModelType = null;
        }
    }

    public void runInference(final String modelType, final String prompt, final InferenceCallback callback) {
        executorService.execute(() -> {
            try {
                File modelFile = getModelFile(modelType);
                if (modelFile == null) {
                    postError("Файл локальной модели не найден. Пожалуйста, скачайте модель в настройках.", callback);
                    return;
                }

                Log.d(TAG, "Attempting to run local inference. Model file: " + modelFile.getAbsolutePath() + " (size: " + modelFile.length() + " bytes)");
                if (modelFile.length() < 50 * 1024 * 1024) {
                    postError("Файл модели поврежден или недогружен (размер: " + String.format(java.util.Locale.US, "%.1f", modelFile.length() / (1024.0 * 1024.0)) + " MB). Пожалуйста, удалите модель в настройках и скачайте её заново.", callback);
                    return;
                }

                if (llmInference != null && !modelType.equals(loadedModelType)) {
                    close();
                }

                if (llmInference == null) {
                    Log.d(TAG, "Initializing LlmInference with file: " + modelFile.getAbsolutePath() + " (" + (modelFile.length() / (1024 * 1024)) + " MB)");
                    LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                            .setModelPath(modelFile.getAbsolutePath())
                            .setMaxTokens(8192)
                            .build();
                    llmInference = LlmInference.createFromOptions(context, options);
                    loadedModelType = modelType;
                    Log.d(TAG, "LlmInference initialized successfully");
                }

                Log.d(TAG, "Running local inference...");
                String cleanPrompt = prompt;
                if (!prompt.contains("Markdown") && !prompt.contains("<|im_start|>")) {
                    cleanPrompt = prompt + "\n\nОтвет напиши обычным текстом, без использования Markdown (без звездочек *, решеток #, списков, жирного или курсивного текста).";
                }

                long startTime = System.currentTimeMillis();
                String response = llmInference.generateResponse(cleanPrompt);
                long duration = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Local inference finished in " + duration + " ms");

                postSuccess(response, callback);

            } catch (final Throwable t) {
                Log.e(TAG, "Error during local inference", t);
                close();
                postError("Ошибка выполнения локального ИИ: " + t.getMessage(), callback);
            }
        });
    }

    private void postSuccess(final String result, final InferenceCallback callback) {
        mainHandler.post(() -> callback.onSuccess(result));
    }

    private void postError(final String error, final InferenceCallback callback) {
        mainHandler.post(() -> callback.onError(error));
    }
}
