package ru.mogcommunity.rbr_project.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.data.remote.GeminiClient;
import ru.mogcommunity.rbr_project.data.repository.ProjectRepository;

import java.util.List;

public class ProjectViewModel extends AndroidViewModel {
    private final ProjectRepository repository;
    private final PreferenceManager preferenceManager;
    private final GeminiClient geminiClient;

    private final LiveData<List<Project>> allProjects;
    private final LiveData<List<Snapshot>> allGallerySnapshots;

    private final MutableLiveData<Boolean> isAiLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> aiError = new MutableLiveData<>(null);

    private final MutableLiveData<Boolean> isChatLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> chatError = new MutableLiveData<>(null);
    private String activeProjectId;

    public ProjectViewModel(@NonNull Application application) {
        super(application);
        repository = new ProjectRepository(application);
        preferenceManager = PreferenceManager.getInstance(application);
        geminiClient = new GeminiClient();

        allProjects = repository.getAllProjects();
        allGallerySnapshots = repository.getAllSnapshotsWithImages();
    }

    public LiveData<List<Project>> getAllProjects() {
        return allProjects;
    }

    public LiveData<List<Snapshot>> getSnapshotsForProject(String projectId) {
        return repository.getSnapshotsForProject(projectId);
    }

    public LiveData<List<Snapshot>> getAllGallerySnapshots() {
        return allGallerySnapshots;
    }

    public LiveData<Boolean> getIsAiLoading() {
        return isAiLoading;
    }

    public LiveData<String> getAiError() {
        return aiError;
    }

    public void clearAiError() {
        aiError.setValue(null);
    }

    public LiveData<Boolean> getIsChatLoading() {
        return isChatLoading;
    }

    public LiveData<String> getChatError() {
        return chatError;
    }

    public void clearChatError() {
        chatError.setValue(null);
    }

    public LiveData<List<ru.mogcommunity.rbr_project.data.model.ChatMessage>> getChatMessagesForProject(String projectId) {
        return repository.getChatMessagesForProject(projectId);
    }

    public void addProject(String id, String name, String description) {
        Project project = new Project(id, name, description, System.currentTimeMillis());
        Log.d("RBR_ProjectViewModel", "Adding project: id=" + id + ", name=" + name);
        repository.insertProject(project);
    }

    public void deleteProject(Project project) {
        repository.deleteProject(project);
    }

    public void addSnapshot(String id, String projectId, String title, String description,
                            boolean hasError, String errorLog, String imageUrl, Uri localImageUri) {
        Snapshot snapshot = new Snapshot(id, projectId, title, System.currentTimeMillis(),
                description, hasError, errorLog, imageUrl, "");
        repository.insertSnapshot(snapshot, localImageUri);
    }

    public void deleteSnapshot(Snapshot snapshot) {
        repository.deleteSnapshot(snapshot);
    }

    public void runAiAnalysis(Snapshot snapshot, String projectDescription) {
        String selectedModel = preferenceManager.getSelectedModel();
        
        isAiLoading.setValue(true);
        aiError.setValue(null);

        ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Snapshot> snapshots = repository.getSnapshotsForProjectSync(snapshot.getProjectId());
            StringBuilder historyBuilder = new StringBuilder();
            if (snapshots != null) {
                int index = 1;
                for (Snapshot s : snapshots) {
                    if (s.getId().equals(snapshot.getId())) {
                        continue;
                    }
                    historyBuilder.append(index).append(". Снимок: \"").append(s.getTitle()).append("\"\n");
                    historyBuilder.append("   Описание: ").append(s.getDescription() != null ? s.getDescription() : "нет").append("\n");
                    historyBuilder.append("   Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append("\n");
                    if (s.getAiAnalysisPlan() != null && !s.getAiAnalysisPlan().trim().isEmpty()) {
                        historyBuilder.append("   План решения ИИ: ").append(s.getAiAnalysisPlan().trim()).append("\n");
                    }
                    historyBuilder.append("\n");
                    index++;
                }
            }
            String finalHistory = historyBuilder.toString().trim();
            if (finalHistory.isEmpty()) {
                finalHistory = "Отсутствует (это первый снимок проекта)";
            }

            if ("gemini".equals(selectedModel)) {
                String apiKey = preferenceManager.getGeminiApiKey();
                if (apiKey.trim().isEmpty()) {
                    aiError.postValue("Gemini API-ключ отсутствует. Задайте его в Настройках.");
                    isAiLoading.postValue(false);
                    return;
                }

                String logContent = snapshot.getErrorLog() != null ? snapshot.getErrorLog() : "Лог ошибки отсутствует";
                String prompt = "Ты — ведущий инженер по поиску неисправностей в системах. Тебе нужно проанализировать ошибку и предложить пошаговый план решения.\n" +
                        "Проект: " + projectDescription + "\n\n" +
                        "История всех предыдущих этапов проекта:\n" + finalHistory + "\n\n" +
                        "Текущий сбой/ошибка: " + logContent + "\n\n" +
                        "Задание: Напиши краткий, технически точный пошаговый план действий для устранения этой проблемы на русском языке. Ответ должен быть написан обычным текстом, без использования Markdown-разметки (не используй звездочки *, решетки #, списки, жирный или курсивный текст). Для разделения шагов используй только новые строки.";

                geminiClient.analyzeError(apiKey, prompt, new GeminiClient.GeminiCallback() {
                    @Override
                    public void onSuccess(String plan) {
                        snapshot.setAiAnalysisPlan(ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(plan));
                        repository.updateSnapshot(snapshot);
                        isAiLoading.postValue(false);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        aiError.postValue(errorMessage);
                        isAiLoading.postValue(false);
                    }
                });
            } else {
                ru.mogcommunity.rbr_project.data.local.LocalAiManager localAiManager = 
                        ru.mogcommunity.rbr_project.data.local.LocalAiManager.getInstance(getApplication());
                
                if (localAiManager.isModelAvailable(selectedModel)) {
                    String logContent = snapshot.getErrorLog() != null ? snapshot.getErrorLog() : "Лог ошибки отсутствует";
                    String prompt;
                    if ("qwen_1.5b".equals(selectedModel)) {
                        StringBuilder qwenPrompt = new StringBuilder();
                        qwenPrompt.append("<|im_start|>system\n");
                        qwenPrompt.append("Ты — ведущий инженер по поиску неисправностей. Напиши пошаговый план решения проблемы или ремонта на русском языке. Пиши простыми словами, без приветствий, вступлений и Markdown-разметки.\n");
                        qwenPrompt.append("<|im_end|>\n");
                        qwenPrompt.append("<|im_start|>user\n");
                        qwenPrompt.append("Проект: ").append(projectDescription).append("\n\n");
                        qwenPrompt.append("История всех предыдущих этапов проекта:\n").append(finalHistory).append("\n\n");
                        qwenPrompt.append("Проблема: ").append(logContent).append("\n");
                        qwenPrompt.append("<|im_end|>\n");
                        qwenPrompt.append("<|im_start|>assistant\nПлан действий:\n");
                        prompt = qwenPrompt.toString();
                    } else {
                        prompt = "Инструкция: Напиши краткий пошаговый план решения проблемы или ремонта на русском языке. Пиши простыми словами, без приветствий, вступлений и Markdown-разметки.\n" +
                                "Проект: " + projectDescription + "\n\n" +
                                "История всех предыдущих этапов проекта:\n" + finalHistory + "\n\n" +
                                "Проблема: " + logContent + "\n\n" +
                                "План действий:";
                    }
                    
                    localAiManager.runInference(selectedModel, prompt, new ru.mogcommunity.rbr_project.data.local.LocalAiManager.InferenceCallback() {
                        @Override
                        public void onSuccess(String output) {
                            snapshot.setAiAnalysisPlan(ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(output));
                            repository.updateSnapshot(snapshot);
                            isAiLoading.postValue(false);
                        }

                        @Override
                        public void onError(String error) {
                            aiError.postValue(error);
                            isAiLoading.postValue(false);
                        }
                    });
                } else {
                    String friendlyName;
                    if ("qwen_1.5b".equals(selectedModel)) {
                        friendlyName = "Qwen2.5 1.5B";
                    } else {
                        friendlyName = "локальной";
                    }
                    aiError.postValue("Файл модели " + friendlyName + " не найден. Пожалуйста, скачайте её в Настройках.");
                    isAiLoading.postValue(false);
                }
            }
        });
    }

    public boolean isCloudSyncEnabled() {
        return preferenceManager.isCloudSyncEnabled();
    }

    public void setCloudSyncEnabled(boolean enabled) {
        preferenceManager.setCloudSyncEnabled(enabled);
    }

    public String getGeminiApiKey() {
        return preferenceManager.getGeminiApiKey();
    }

    public void setGeminiApiKey(String apiKey) {
        preferenceManager.setGeminiApiKey(apiKey);
    }

    public void syncWithCloud(ProjectRepository.OnSyncCompleteListener listener) {
        repository.syncWithCloud(listener);
    }

    public String getSelectedModel() {
        return preferenceManager.getSelectedModel();
    }

    public void setSelectedModel(String model) {
        preferenceManager.setSelectedModel(model);
    }

    public boolean isModelDownloaded(String modelName) {
        return ru.mogcommunity.rbr_project.data.local.LocalAiManager.getInstance(getApplication()).isModelAvailable(modelName);
    }

    public void setModelDownloaded(String modelName, boolean downloaded) {
        preferenceManager.setModelDownloaded(modelName, downloaded);
    }

    public void sendChatMessage(String projectId, String text) {
        if (text == null || text.trim().isEmpty()) return;
        
        String cleanText = text.trim();
        String messageId = java.util.UUID.randomUUID().toString();
        ru.mogcommunity.rbr_project.data.model.ChatMessage userMsg = new ru.mogcommunity.rbr_project.data.model.ChatMessage(messageId, projectId, "user", cleanText, System.currentTimeMillis());
        repository.insertChatMessage(userMsg);
        
        isChatLoading.setValue(true);
        chatError.setValue(null);
        
        ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            Project project = repository.getProjectByIdSync(projectId);
            if (project == null) {
                chatError.postValue("Проект не найден");
                isChatLoading.postValue(false);
                return;
            }
            
            List<Snapshot> snapshots = repository.getSnapshotsForProjectSync(projectId);
            List<ru.mogcommunity.rbr_project.data.model.ChatMessage> history = repository.getChatMessagesForProjectSync(projectId);

            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("Инженерный контекст проекта:\n");
            contextBuilder.append("Название проекта: ").append(project.getName()).append("\n");
            contextBuilder.append("Описание проекта: ").append(project.getDescription()).append("\n\n");
            contextBuilder.append("История снимков (коммитов) проекта:\n");
            if (snapshots == null || snapshots.isEmpty()) {
                contextBuilder.append("История снимков пуста.\n");
            } else {
                for (int i = 0; i < snapshots.size(); i++) {
                    Snapshot s = snapshots.get(i);
                    contextBuilder.append(i + 1).append(". Снимок: \"").append(s.getTitle()).append("\"\n");
                    contextBuilder.append("   Описание действий: ").append(s.getDescription() != null ? s.getDescription() : "нет").append("\n");
                    contextBuilder.append("   Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append("\n");
                    if (s.isHasError() && s.getErrorLog() != null && !s.getErrorLog().trim().isEmpty()) {
                        contextBuilder.append("   Лог ошибки: ").append(s.getErrorLog().trim()).append("\n");
                    }
                    if (s.getAiAnalysisPlan() != null && !s.getAiAnalysisPlan().trim().isEmpty()) {
                        contextBuilder.append("   План решения ИИ: ").append(s.getAiAnalysisPlan().trim()).append("\n");
                    }
                    contextBuilder.append("\n");
                }
            }

            String selectedModel = preferenceManager.getSelectedModel();
            String prompt;
            if ("qwen_1.5b".equals(selectedModel)) {
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("<|im_start|>system\n");
                promptBuilder.append("Ты — ИИ-ассистент инженера. Твоя роль — отвечать на вопросы пользователя о проекте, анализировать ошибки, объяснять причины сбоев и давать рекомендации по ремонту/настройке.\n");
                promptBuilder.append("Отвечай технически точно, развернуто и по существу на русском языке. Ответ должен быть обычным текстом, БЕЗ использования Markdown разметки (не используй звездочки *, решетки #, списки, жирный или курсивный текст). Пиши профессиональным, грамотным и дружелюбным языком.\n");
                promptBuilder.append("<|im_end|>\n");
                
                promptBuilder.append("<|im_start|>user\n");
                promptBuilder.append(contextBuilder.toString());
                promptBuilder.append("\nПредыдущая история диалога:\n");
                if (history != null) {
                    for (ru.mogcommunity.rbr_project.data.model.ChatMessage msg : history) {
                        if (msg.getId().equals(messageId)) continue;
                        if ("user".equals(msg.getSender())) {
                            promptBuilder.append("Пользователь: ").append(msg.getText()).append("\n");
                        } else {
                            promptBuilder.append("ИИ-ассистент: ").append(msg.getText()).append("\n");
                        }
                    }
                }
                promptBuilder.append("\nНовый вопрос: ").append(cleanText).append("\n");
                promptBuilder.append("<|im_end|>\n");
                promptBuilder.append("<|im_start|>assistant\n");
                prompt = promptBuilder.toString();
            } else {
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Ты — ИИ-ассистент инженера. Твоя роль — отвечать на вопросы пользователя о проекте, анализировать ошибки, объяснять причины сбоев и давать рекомендации по ремонту/настройке.\n\n");
                promptBuilder.append(contextBuilder.toString());
                promptBuilder.append("Предыдущая история диалога:\n");
                if (history != null) {
                    for (ru.mogcommunity.rbr_project.data.model.ChatMessage msg : history) {
                        if (msg.getId().equals(messageId)) continue;
                        if ("user".equals(msg.getSender())) {
                            promptBuilder.append("Пользователь: ").append(msg.getText()).append("\n");
                        } else {
                            promptBuilder.append("ИИ-ассистент: ").append(msg.getText()).append("\n");
                        }
                    }
                }
                promptBuilder.append("\nНовый вопрос пользователя: ").append(cleanText).append("\n\n");
                promptBuilder.append("Инструкция для ответа: Отвечай технически точно, кратко и по существу на русском языке. Ответ должен быть обычным текстом, БЕЗ использования Markdown разметки (не используй звездочки *, решетки #, списки, жирный или курсивный текст).");
                prompt = promptBuilder.toString();
            }
            
            if ("gemini".equals(selectedModel)) {
                String apiKey = preferenceManager.getGeminiApiKey();
                if (apiKey.trim().isEmpty()) {
                    chatError.postValue("Gemini API-ключ отсутствует. Задайте его в Настройках.");
                    isChatLoading.postValue(false);
                    return;
                }
                
                geminiClient.analyzeError(apiKey, prompt, new GeminiClient.GeminiCallback() {
                    @Override
                    public void onSuccess(String plan) {
                        ru.mogcommunity.rbr_project.data.model.ChatMessage aiMsg = new ru.mogcommunity.rbr_project.data.model.ChatMessage(
                            java.util.UUID.randomUUID().toString(),
                            projectId,
                            "ai",
                            ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(plan),
                            System.currentTimeMillis()
                        );
                        repository.insertChatMessage(aiMsg);
                        isChatLoading.postValue(false);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        chatError.postValue(errorMessage);
                        isChatLoading.postValue(false);
                    }
                });
            } else {
                ru.mogcommunity.rbr_project.data.local.LocalAiManager localAiManager = 
                        ru.mogcommunity.rbr_project.data.local.LocalAiManager.getInstance(getApplication());
                
                if (localAiManager.isModelAvailable(selectedModel)) {
                    localAiManager.runInference(selectedModel, prompt, new ru.mogcommunity.rbr_project.data.local.LocalAiManager.InferenceCallback() {
                        @Override
                        public void onSuccess(String output) {
                            ru.mogcommunity.rbr_project.data.model.ChatMessage aiMsg = new ru.mogcommunity.rbr_project.data.model.ChatMessage(
                                java.util.UUID.randomUUID().toString(),
                                projectId,
                                "ai",
                                ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(output),
                                System.currentTimeMillis()
                            );
                            repository.insertChatMessage(aiMsg);
                            isChatLoading.postValue(false);
                        }

                        @Override
                        public void onError(String error) {
                            chatError.postValue(error);
                            isChatLoading.postValue(false);
                        }
                    });
                } else {
                    String friendlyName;
                    if ("qwen_1.5b".equals(selectedModel)) {
                        friendlyName = "Qwen2.5 1.5B";
                    } else {
                        friendlyName = "локальной";
                    }
                    chatError.postValue("Файл модели " + friendlyName + " не найден. Пожалуйста, скачайте её в Настройках.");
                    isChatLoading.postValue(false);
                }
            }
        });
    }

    public String getActiveProjectId() {
        return activeProjectId;
    }

    public void setActiveProjectId(String activeProjectId) {
        this.activeProjectId = activeProjectId;
    }
}

