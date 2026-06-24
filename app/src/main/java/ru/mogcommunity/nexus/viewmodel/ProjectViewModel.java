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
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
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
    private static final java.util.Set<String> activeSummarizations = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    @Inject
    public ProjectViewModel(@NonNull Application application, ProjectRepository repository, PreferenceManager preferenceManager, GeminiClient geminiClient) {
        super(application);
        this.repository = repository;
        this.preferenceManager = preferenceManager;
        this.geminiClient = geminiClient;

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

    public void updateProjectDetails(String projectId, String name, String description, String configEnv) {
        repository.updateProjectDetails(projectId, name, description, configEnv);
    }

    public void addSnapshot(String id, String projectId, String title, String description,
                            boolean hasError, String errorLog, String imageUrl, Uri localImageUri,
                            String tags, String secondaryImages, List<Uri> secondaryUris) {
        Snapshot snapshot = new Snapshot(id, projectId, title, System.currentTimeMillis(),
                description, hasError, errorLog, imageUrl, "");
        snapshot.setTags(tags);
        snapshot.setSecondaryImages(secondaryImages);
        repository.insertSnapshot(snapshot, localImageUri, secondaryUris);
    }

    public void deleteSnapshot(Snapshot snapshot) {
        repository.deleteSnapshot(snapshot);
    }

    public void runAiAnalysis(Snapshot snapshot, String projectDescription) {
        String selectedModel = preferenceManager.getSelectedModel();
        
        isAiLoading.setValue(true);
        aiError.setValue(null);

        ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            Project project = repository.getProjectByIdSync(snapshot.getProjectId());
            String finalProjectDesc = projectDescription;
            if (project != null && project.getConfigEnv() != null && !project.getConfigEnv().trim().isEmpty()) {
                finalProjectDesc += "\nКонфигурация проекта (config.env):\n" + project.getConfigEnv().trim();
            }
            List<Snapshot> snapshots = repository.getSnapshotsForProjectSync(snapshot.getProjectId());
            List<Snapshot> historySnapshots = new java.util.ArrayList<>();
            if (snapshots != null) {
                for (Snapshot s : snapshots) {
                    if (!s.getId().equals(snapshot.getId())) {
                        historySnapshots.add(s);
                    }
                }
            }

            StringBuilder historyBuilder = new StringBuilder();
            int totalHist = historySnapshots.size();
            int cutoff = totalHist - 3;
            for (int i = 0; i < totalHist; i++) {
                Snapshot s = historySnapshots.get(i);
                int index = i + 1;
                if (totalHist > 4 && i < cutoff) {
                    historyBuilder.append(index).append(". Снимок: \"").append(s.getTitle()).append("\" ");
                    historyBuilder.append("(Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append(")");
                    if (s.getTags() != null && !s.getTags().trim().isEmpty()) {
                        historyBuilder.append(" [Теги: ").append(s.getTags().trim()).append("]");
                    }
                    historyBuilder.append("\n");
                } else {
                    historyBuilder.append(index).append(". Снимок: \"").append(s.getTitle()).append("\"\n");
                    historyBuilder.append("   Описание: ").append(s.getDescription() != null ? s.getDescription() : "нет").append("\n");
                    historyBuilder.append("   Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append("\n");
                    if (s.getTags() != null && !s.getTags().trim().isEmpty()) {
                        historyBuilder.append("   Теги: ").append(s.getTags().trim()).append("\n");
                    }
                    if (s.getAiAnalysisPlan() != null && !s.getAiAnalysisPlan().trim().isEmpty()) {
                        historyBuilder.append("   План решения ИИ: ").append(s.getAiAnalysisPlan().trim()).append("\n");
                    }
                    historyBuilder.append("\n");
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
                        "Проект: " + finalProjectDesc + "\n\n" +
                        "История всех предыдущих этапов проекта:\n" + finalHistory + "\n\n" +
                        "Текущий сбой/ошибка: " + logContent + "\n\n" +
                        "Задание: Напиши краткий, технически точный пошаговый план действий для устранения этой проблемы на русском языке. Ответ должен быть написан обычным текстом, без использования Markdown-разметки (не используй звездочки *, решетки #, списки, жирный или курсивный текст). Для разделения шагов используй только новые строки.";

                geminiClient.analyzeError(apiKey, prompt, snapshot.getImageUrl(), getApplication(), new GeminiClient.GeminiCallback() {
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
                        qwenPrompt.append("Проект: ").append(finalProjectDesc).append("\n\n");
                        qwenPrompt.append("История всех предыдущих этапов проекта:\n").append(finalHistory).append("\n\n");
                        qwenPrompt.append("Проблема: ").append(logContent).append("\n");
                        qwenPrompt.append("<|im_end|>\n");
                        qwenPrompt.append("<|im_start|>assistant\nПлан действий:\n");
                        prompt = qwenPrompt.toString();
                    } else {
                        prompt = "Инструкция: Напиши краткий пошаговый план решения проблемы или ремонта на русском языке. Пиши простыми словами, без приветствий, вступлений и Markdown-разметки.\n" +
                                "Проект: " + finalProjectDesc + "\n\n" +
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
            contextBuilder.append("Описание проекта: ").append(project.getDescription()).append("\n");
            if (project.getConfigEnv() != null && !project.getConfigEnv().trim().isEmpty()) {
                contextBuilder.append("Конфигурация проекта (config.env):\n").append(project.getConfigEnv().trim()).append("\n");
            }
            contextBuilder.append("\n");
            contextBuilder.append("История снимков (коммитов) проекта:\n");
            if (snapshots == null || snapshots.isEmpty()) {
                contextBuilder.append("История снимков пуста.\n");
            } else {
                int totalSnaps = snapshots.size();
                int cutoff = totalSnaps - 3;
                for (int i = 0; i < totalSnaps; i++) {
                    Snapshot s = snapshots.get(i);
                    int index = i + 1;
                    if (totalSnaps > 4 && i < cutoff) {
                        contextBuilder.append(index).append(". Снимок: \"").append(s.getTitle()).append("\" ");
                        contextBuilder.append("(Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append(")");
                        if (s.getTags() != null && !s.getTags().trim().isEmpty()) {
                            contextBuilder.append(" [Теги: ").append(s.getTags().trim()).append("]");
                        }
                        contextBuilder.append("\n");
                    } else {
                        contextBuilder.append(index).append(". Снимок: \"").append(s.getTitle()).append("\"\n");
                        contextBuilder.append("   Описание действий: ").append(s.getDescription() != null ? s.getDescription() : "нет").append("\n");
                        contextBuilder.append("   Статус: ").append(s.isHasError() ? "Ошибка/Сбой" : "Успешно").append("\n");
                        if (s.getTags() != null && !s.getTags().trim().isEmpty()) {
                            contextBuilder.append("   Теги: ").append(s.getTags().trim()).append("\n");
                        }
                        if (s.isHasError() && s.getErrorLog() != null && !s.getErrorLog().trim().isEmpty()) {
                            contextBuilder.append("   Лог ошибки: ").append(s.getErrorLog().trim()).append("\n");
                        }
                        if (s.getAiAnalysisPlan() != null && !s.getAiAnalysisPlan().trim().isEmpty()) {
                            contextBuilder.append("   План решения ИИ: ").append(s.getAiAnalysisPlan().trim()).append("\n");
                        }
                        contextBuilder.append("\n");
                    }
                }
            }

            int lastSummarizedIndex = -1;
            String lastSumId = project.getLastSummarizedMessageId();
            if (lastSumId != null && !lastSumId.isEmpty() && history != null) {
                for (int i = 0; i < history.size(); i++) {
                    if (history.get(i).getId().equals(lastSumId)) {
                        lastSummarizedIndex = i;
                        break;
                    }
                }
            }

            List<ru.mogcommunity.rbr_project.data.model.ChatMessage> activeHistory = new java.util.ArrayList<>();
            if (history != null) {
                for (int i = lastSummarizedIndex + 1; i < history.size(); i++) {
                    activeHistory.add(history.get(i));
                }
            }

            StringBuilder historyPrompt = new StringBuilder();
            if (project.getChatSummary() != null && !project.getChatSummary().isEmpty()) {
                historyPrompt.append("Ранее в диалоге было обсуждено следующее:\n")
                             .append(project.getChatSummary().trim()).append("\n\n");
            }
            if (!activeHistory.isEmpty()) {
                historyPrompt.append("Активная часть диалога:\n");
                for (ru.mogcommunity.rbr_project.data.model.ChatMessage msg : activeHistory) {
                    if (msg.getId().equals(messageId)) continue;
                    if ("user".equals(msg.getSender())) {
                        historyPrompt.append("Пользователь: ").append(msg.getText()).append("\n");
                    } else {
                        historyPrompt.append("ИИ-ассистент: ").append(msg.getText()).append("\n");
                    }
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
                promptBuilder.append(historyPrompt.toString());
                promptBuilder.append("\nНовый вопрос: ").append(cleanText).append("\n");
                promptBuilder.append("<|im_end|>\n");
                promptBuilder.append("<|im_start|>assistant\n");
                prompt = promptBuilder.toString();
            } else {
                StringBuilder promptBuilder = new StringBuilder();
                promptBuilder.append("Ты — ИИ-ассистент инженера. Твоя роль — отвечать на вопросы пользователя о проекте, анализировать ошибки, объяснять причины сбоев и давать рекомендации по ремонту/настройке.\n\n");
                promptBuilder.append(contextBuilder.toString());
                promptBuilder.append("Предыдущая история диалога:\n");
                promptBuilder.append(historyPrompt.toString());
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
                
                geminiClient.analyzeError(apiKey, prompt, null, null, new GeminiClient.GeminiCallback() {
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
            
            if (history != null && history.size() >= 10 && activeHistory.size() >= 6) {
                String pId = projectId;
                if (!activeSummarizations.contains(pId)) {
                    activeSummarizations.add(pId);
                    triggerBackgroundChatSummarization(project, lastSummarizedIndex, history);
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

    private void triggerBackgroundChatSummarization(Project project, int lastSummarizedIndex, List<ru.mogcommunity.rbr_project.data.model.ChatMessage> history) {
        ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            int newCutoff = history.size() - 3;
            if (newCutoff <= lastSummarizedIndex + 1) {
                activeSummarizations.remove(project.getId());
                return;
            }

            List<ru.mogcommunity.rbr_project.data.model.ChatMessage> newBatchToSummarize = new java.util.ArrayList<>();
            for (int i = lastSummarizedIndex + 1; i < newCutoff; i++) {
                newBatchToSummarize.add(history.get(i));
            }

            if (newBatchToSummarize.isEmpty()) {
                activeSummarizations.remove(project.getId());
                return;
            }

            ru.mogcommunity.rbr_project.data.model.ChatMessage newLastSumMsg = history.get(newCutoff - 1);

            StringBuilder sumPrompt = new StringBuilder();
            sumPrompt.append("Ты — ИИ-ассистент, помогающий сжать историю диалога. Твоя задача — составить краткую выжимку (2-3 предложения) на русском языке.\n");
            if (project.getChatSummary() != null && !project.getChatSummary().isEmpty()) {
                sumPrompt.append("Предыдущая сводка диалога:\n").append(project.getChatSummary()).append("\n\n");
            }
            sumPrompt.append("Новые сообщения для добавления в сводку:\n");
            for (ru.mogcommunity.rbr_project.data.model.ChatMessage m : newBatchToSummarize) {
                sumPrompt.append(m.getSender().equals("user") ? "Пользователь: " : "ИИ-ассистент: ").append(m.getText()).append("\n");
            }
            sumPrompt.append("\nНапиши обновленную единую краткую сводку диалога на русском языке (2-3 предложения), объединяющую предыдущую сводку и новые сообщения. Ответ должен быть простым текстом без Markdown.");

            String selectedModel = preferenceManager.getSelectedModel();
            if ("gemini".equals(selectedModel)) {
                String apiKey = preferenceManager.getGeminiApiKey();
                if (apiKey.trim().isEmpty()) {
                    activeSummarizations.remove(project.getId());
                    return;
                }

                geminiClient.analyzeError(apiKey, sumPrompt.toString(), null, null, new GeminiClient.GeminiCallback() {
                    @Override
                    public void onSuccess(String plan) {
                        ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                            project.setChatSummary(ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(plan));
                            project.setLastSummarizedMessageId(newLastSumMsg.getId());
                            repository.updateProject(project);
                        });
                        activeSummarizations.remove(project.getId());
                    }

                    @Override
                    public void onError(String errorMessage) {
                        Log.e("RBR_Summarization", "Error summarising: " + errorMessage);
                        activeSummarizations.remove(project.getId());
                    }
                });
            } else {
                ru.mogcommunity.rbr_project.data.local.LocalAiManager localAiManager = 
                        ru.mogcommunity.rbr_project.data.local.LocalAiManager.getInstance(getApplication());
                
                if (localAiManager.isModelAvailable(selectedModel)) {
                    localAiManager.runInference(selectedModel, sumPrompt.toString(), new ru.mogcommunity.rbr_project.data.local.LocalAiManager.InferenceCallback() {
                        @Override
                        public void onSuccess(String output) {
                            ru.mogcommunity.rbr_project.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
                                project.setChatSummary(ru.mogcommunity.rbr_project.ui.helper.MarkdownStripper.strip(output));
                                project.setLastSummarizedMessageId(newLastSumMsg.getId());
                                repository.updateProject(project);
                            });
                            activeSummarizations.remove(project.getId());
                        }

                        @Override
                        public void onError(String error) {
                            Log.e("RBR_Summarization", "Error summarising locally: " + error);
                            activeSummarizations.remove(project.getId());
                        }
                    });
                } else {
                    activeSummarizations.remove(project.getId());
                }
            }
        });
    }
}

