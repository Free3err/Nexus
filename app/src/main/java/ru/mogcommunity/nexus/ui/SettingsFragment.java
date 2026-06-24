package ru.mogcommunity.rbr_project.ui;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;

import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.data.repository.ProjectRepository;
import ru.mogcommunity.rbr_project.databinding.FragmentSettingsBinding;
import ru.mogcommunity.rbr_project.viewmodel.AuthViewModel;
import ru.mogcommunity.rbr_project.viewmodel.ProjectViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private ProjectViewModel projectViewModel;
    private AuthViewModel authViewModel;
    private final Handler downloadProgressHandler = new Handler(Looper.getMainLooper());
    private Runnable downloadProgressRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        projectViewModel = new ViewModelProvider(requireActivity()).get(ProjectViewModel.class);
        authViewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        loadPreferences();
        setupListeners();
        observeAuth();
        setupAccountState();
    }

    private void loadPreferences() {
        binding.switchCloudSync.setChecked(projectViewModel.isCloudSyncEnabled());
        binding.inputApiKey.setText(projectViewModel.getGeminiApiKey());

        PreferenceManager prefs = PreferenceManager.getInstance(getContext());
        binding.switchDynamicColors.setChecked(prefs.isDynamicColorsEnabled());

        float scale = prefs.getFontScale();
        int percent = Math.round(scale * 100f);
        binding.textFontSizeTitle.setText("Размер шрифта: " + percent + "%");
        binding.sliderFontSize.setValue((float) percent);

        String activeModel = projectViewModel.getSelectedModel();
        updateModelSelectionVisuals(activeModel);

        String[] models = {"qwen_1.5b"};
        for (String m : models) {
            long downloadId = prefs.getDownloadId(m);
            if (downloadId != -1) {
                trackDownloadProgress(m, downloadId);
            }
        }

        updateModelDownloadUI();
    }

    private void setupListeners() {
        binding.switchCloudSync.setOnCheckedChangeListener((buttonView, isChecked) -> {
            projectViewModel.setCloudSyncEnabled(isChecked);
        });

        binding.inputApiKey.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                projectViewModel.setGeminiApiKey(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.switchDynamicColors.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferenceManager.getInstance(getContext()).setDynamicColorsEnabled(isChecked);
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });

        binding.sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            int val = Math.round(value);
            binding.textFontSizeTitle.setText("Размер шрифта: " + val + "%");
        });

        binding.sliderFontSize.addOnSliderTouchListener(new com.google.android.material.slider.Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {
                float newScale = slider.getValue() / 100f;
                PreferenceManager prefs = PreferenceManager.getInstance(getContext());
                if (Math.abs(prefs.getFontScale() - newScale) > 0.01f) {
                    prefs.setFontScale(newScale);
                    if (getActivity() != null) {
                        getActivity().recreate();
                    }
                }
            }
        });

        binding.cardModelGemini.setOnClickListener(v -> {
            projectViewModel.setSelectedModel("gemini");
            updateModelSelectionVisuals("gemini");
            updateModelDownloadUI();
        });

        binding.cardModelQwen.setOnClickListener(v -> {
            projectViewModel.setSelectedModel("qwen_1.5b");
            updateModelSelectionVisuals("qwen_1.5b");
            updateModelDownloadUI();
        });

        binding.btnDownloadLocalModel.setOnClickListener(v -> {
            String activeModel = projectViewModel.getSelectedModel();
            handleDownloadClick(activeModel);
        });

        binding.btnDeleteLocalModel.setOnClickListener(v -> {
            String activeModel = projectViewModel.getSelectedModel();
            deleteModelFile(activeModel);
        });

        binding.btnSyncNow.setOnClickListener(v -> {
            binding.btnSyncNow.setEnabled(false);
            binding.btnSyncNow.setText("Синхронизация...");
            projectViewModel.syncWithCloud(new ProjectRepository.OnSyncCompleteListener() {
                @Override
                public void onSyncSuccess() {
                    if (binding == null) return;
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Синхронизировать сейчас");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Данные синхронизированы с облаком", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onSyncFailed(String error) {
                    if (binding == null) return;
                    binding.btnSyncNow.setEnabled(true);
                    binding.btnSyncNow.setText("Синхронизировать сейчас");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка синхронизации: " + error, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });

        binding.btnLogout.setOnClickListener(v -> authViewModel.signOut());
        binding.btnLogin.setOnClickListener(v -> authViewModel.signOut());
    }

    private void updateModelSelectionVisuals(String activeModel) {
        int primaryColor = getResources().getColor(R.color.primary);
        int outlineColor = getResources().getColor(R.color.outline);

        if (getContext() != null) {
            android.util.TypedValue typedValuePrimary = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValuePrimary, true)) {
                primaryColor = typedValuePrimary.data;
            }
            android.util.TypedValue typedValueOutline = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValueOutline, true)) {
                outlineColor = typedValueOutline.data;
            }
        }

        int borderActive = (int) (2 * getResources().getDisplayMetrics().density);
        int borderNormal = (int) (1 * getResources().getDisplayMetrics().density);

        binding.cardModelGemini.setStrokeColor("gemini".equals(activeModel) ? primaryColor : outlineColor);
        binding.cardModelGemini.setStrokeWidth("gemini".equals(activeModel) ? borderActive : borderNormal);

        binding.cardModelQwen.setStrokeColor("qwen_1.5b".equals(activeModel) ? primaryColor : outlineColor);
        binding.cardModelQwen.setStrokeWidth("qwen_1.5b".equals(activeModel) ? borderActive : borderNormal);
    }

    private void updateModelDownloadUI() {
        if (getContext() == null || binding == null) return;

        String activeModel = projectViewModel.getSelectedModel();
        if ("gemini".equals(activeModel)) {
            binding.cardLocalModelActions.setVisibility(View.GONE);
            return;
        }

        binding.cardLocalModelActions.setVisibility(View.VISIBLE);
        binding.textLocalModelTitle.setText("Параметры модели " + getModelFriendlyName(activeModel));

        PreferenceManager prefs = PreferenceManager.getInstance(getContext());
        long downloadId = prefs.getDownloadId(activeModel);
        boolean isDownloaded = projectViewModel.isModelDownloaded(activeModel);

        if (downloadId != -1) {
            binding.textLocalModelStatus.setText("Статус: Загрузка...");
            binding.progressLocalModel.setVisibility(View.VISIBLE);
            binding.btnDownloadLocalModel.setEnabled(false);
            binding.btnDeleteLocalModel.setEnabled(false);
            trackDownloadProgress(activeModel, downloadId);
        } else if (isDownloaded) {
            binding.textLocalModelStatus.setText("Статус: Установлена");
            binding.progressLocalModel.setVisibility(View.GONE);
            binding.btnDownloadLocalModel.setEnabled(false);
            binding.btnDownloadLocalModel.setText("Загружено");
            binding.btnDeleteLocalModel.setEnabled(true);
        } else {
            binding.textLocalModelStatus.setText("Статус: Не загружена");
            binding.progressLocalModel.setVisibility(View.GONE);
            binding.btnDownloadLocalModel.setEnabled(true);
            binding.btnDownloadLocalModel.setText("Загрузить");
            binding.btnDeleteLocalModel.setEnabled(false);
        }
    }

    private void handleDownloadClick(String modelName) {
        String url;
        String fileName;
        if ("qwen_1.5b".equals(modelName)) {
            url = "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/main/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task";
            fileName = "qwen15.task";
        } else {
            return;
        }
        startRealDownload(modelName, url, fileName);
    }

    private void startRealDownload(String modelName, String url, String fileName) {
        if (getContext() == null) return;

        File modelsDir = new File(getContext().getExternalFilesDir(null), "models");
        if (!modelsDir.exists()) {
            modelsDir.mkdirs();
        }
        File targetFile = new File(modelsDir, fileName);
        if (targetFile.exists()) {
            targetFile.delete();
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Загрузка модели " + getModelFriendlyName(modelName));
        request.setDescription("Загрузка необходимых весов для локального ИИ");
        request.setDestinationUri(Uri.fromFile(targetFile));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

        DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            long downloadId = manager.enqueue(request);
            PreferenceManager.getInstance(getContext()).setDownloadId(modelName, downloadId);
            trackDownloadProgress(modelName, downloadId);
            updateModelDownloadUI();
        } else {
            Toast.makeText(getContext(), "Служба загрузки недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteModelFile(String modelName) {
        ru.mogcommunity.rbr_project.data.local.LocalAiManager localAi = ru.mogcommunity.rbr_project.data.local.LocalAiManager.getInstance(getContext());
        localAi.close();
        File file = localAi.getModelFile(modelName);
        if (file != null && file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                PreferenceManager.getInstance(getContext()).setModelDownloaded(modelName, false);
                Toast.makeText(getContext(), "Модель " + getModelFriendlyName(modelName) + " удалена", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Не удалось удалить файл модели", Toast.LENGTH_SHORT).show();
            }
        } else {
            PreferenceManager.getInstance(getContext()).setModelDownloaded(modelName, false);
            Toast.makeText(getContext(), "Файл модели отсутствует", Toast.LENGTH_SHORT).show();
        }
        updateModelDownloadUI();
    }

    private void trackDownloadProgress(final String modelName, final long downloadId) {
        if (downloadProgressRunnable != null) {
            downloadProgressHandler.removeCallbacks(downloadProgressRunnable);
        }

        downloadProgressRunnable = new Runnable() {
            @Override
            public void run() {
                if (getContext() == null || binding == null) return;

                DownloadManager manager = (DownloadManager) getContext().getSystemService(Context.DOWNLOAD_SERVICE);
                if (manager == null) return;

                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = manager.query(query);

                if (cursor != null && cursor.moveToFirst()) {
                    int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                    int bytesDownloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                    int bytesTotalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                    int status = cursor.getInt(statusIdx);
                    long bytesDownloaded = cursor.getLong(bytesDownloadedIdx);
                    long bytesTotal = cursor.getLong(bytesTotalIdx);

                    cursor.close();

                    String currentActive = projectViewModel.getSelectedModel();
                    boolean isCurrentModel = modelName.equals(currentActive);

                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        PreferenceManager.getInstance(getContext()).setModelDownloaded(modelName, true);
                        PreferenceManager.getInstance(getContext()).setDownloadId(modelName, -1);

                        if (isCurrentModel) {
                            updateModelDownloadUI();
                        }
                        Toast.makeText(getContext(), "Модель " + getModelFriendlyName(modelName) + " успешно загружена", Toast.LENGTH_SHORT).show();
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        PreferenceManager.getInstance(getContext()).setDownloadId(modelName, -1);
                        if (isCurrentModel) {
                            updateModelDownloadUI();
                        }
                        Toast.makeText(getContext(), "Ошибка загрузки модели " + getModelFriendlyName(modelName), Toast.LENGTH_LONG).show();
                    } else {
                        if (isCurrentModel) {
                            binding.progressLocalModel.setVisibility(View.VISIBLE);
                            if (bytesTotal > 0) {
                                int progressVal = (int) (bytesDownloaded * 100 / bytesTotal);
                                binding.progressLocalModel.setIndeterminate(false);
                                binding.progressLocalModel.setProgress(progressVal);
                                binding.textLocalModelStatus.setText("Статус: Загрузка... " + progressVal + "% (" +
                                        String.format(java.util.Locale.US, "%.1f", bytesDownloaded / (1024.0 * 1024.0)) + " MB / " +
                                        String.format(java.util.Locale.US, "%.1f", bytesTotal / (1024.0 * 1024.0)) + " MB)");
                            } else {
                                binding.progressLocalModel.setIndeterminate(true);
                                binding.textLocalModelStatus.setText("Статус: Подготовка к загрузке...");
                            }
                            binding.btnDownloadLocalModel.setEnabled(false);
                            binding.btnDeleteLocalModel.setEnabled(false);
                        }
                        downloadProgressHandler.postDelayed(this, 1000);
                    }
                } else {
                    if (cursor != null) {
                        cursor.close();
                    }
                    PreferenceManager.getInstance(getContext()).setDownloadId(modelName, -1);
                    if (modelName.equals(projectViewModel.getSelectedModel())) {
                        updateModelDownloadUI();
                    }
                }
            }
        };

        downloadProgressHandler.post(downloadProgressRunnable);
    }

    private String getModelFriendlyName(String modelName) {
        if ("qwen_1.5b".equals(modelName)) return "Qwen2.5 1.5B";
        return modelName;
    }

    private void observeAuth() {
        authViewModel.getIsLoggedIn().observe(getViewLifecycleOwner(), loggedIn -> {
            if (!loggedIn) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
    }

    private void setupAccountState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            binding.textUserEmail.setText(user.getEmail());
            binding.btnLogin.setVisibility(View.GONE);
            binding.btnLogout.setVisibility(View.VISIBLE);
            binding.btnSyncNow.setVisibility(View.VISIBLE);
            binding.switchCloudSync.setEnabled(true);
        } else {
            String localUserId = PreferenceManager.getInstance(getContext()).getUserId();
            if (!localUserId.isEmpty()) {
                String displayName = "local_guest_user".equals(localUserId) ? "local_guest" : localUserId.substring(0, Math.min(localUserId.length(), 8));
                binding.textUserEmail.setText("Локальный профиль (" + displayName + ")");
            } else {
                binding.textUserEmail.setText("Не авторизован");
            }
            binding.btnLogin.setVisibility(View.VISIBLE);
            binding.btnLogout.setVisibility(View.GONE);
            binding.btnSyncNow.setVisibility(View.GONE);
            binding.switchCloudSync.setEnabled(false);
            binding.switchCloudSync.setChecked(false);
            projectViewModel.setCloudSyncEnabled(false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (downloadProgressRunnable != null) {
            downloadProgressHandler.removeCallbacks(downloadProgressRunnable);
        }
        binding = null;
    }
}
