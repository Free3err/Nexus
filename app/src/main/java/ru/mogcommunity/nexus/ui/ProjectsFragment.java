package ru.mogcommunity.rbr_project.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.databinding.FragmentProjectsBinding;
import ru.mogcommunity.rbr_project.ui.adapter.GalleryAdapter;
import ru.mogcommunity.rbr_project.ui.adapter.ProjectAdapter;
import ru.mogcommunity.rbr_project.ui.adapter.SnapshotAdapter;
import ru.mogcommunity.rbr_project.ui.adapter.ChatMessageAdapter;
import ru.mogcommunity.rbr_project.ui.helper.SpaceItemDecoration;
import ru.mogcommunity.rbr_project.viewmodel.ProjectViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectsFragment extends Fragment {
    private FragmentProjectsBinding binding;
    private ProjectViewModel viewModel;
    private ProjectAdapter projectAdapter;
    private SnapshotAdapter snapshotAdapter;
    private GalleryAdapter projectGalleryAdapter;
    private Project selectedProject;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
    private List<Project> allProjectsList = new ArrayList<>();
    private ChatMessageAdapter chatMessageAdapter;
    private androidx.lifecycle.LiveData<List<ru.mogcommunity.rbr_project.data.model.ChatMessage>> currentChatLiveData;
    private androidx.lifecycle.LiveData<List<Snapshot>> currentSnapshotsLiveData;
    private String projectSearchQuery = "";
    private ViewTreeObserver.OnGlobalLayoutListener keyboardLayoutListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProjectsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProjectViewModel.class);

        setupRecyclerViews();
        setupListeners();
        observeViewModel();
    }

    private void setupRecyclerViews() {
        binding.rvProjectsHorizontal.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        projectAdapter = new ProjectAdapter(new ArrayList<>(), project -> selectProject(project));
        binding.rvProjectsHorizontal.setAdapter(projectAdapter);
        binding.rvProjectsHorizontal.addItemDecoration(new SpaceItemDecoration(8, true));

        binding.rvSnapshotsTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        snapshotAdapter = new SnapshotAdapter(new ArrayList<>(), new SnapshotAdapter.OnSnapshotClickListener() {
            @Override
            public void onDeleteClick(Snapshot snapshot) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle("Удалить снимок?")
                        .setMessage("Вы уверены, что хотите удалить эту контрольную точку?")
                        .setPositiveButton("Удалить", (dialog, which) -> viewModel.deleteSnapshot(snapshot))
                        .setNegativeButton("Отмена", null)
                        .show();
            }

            @Override
            public void onAiAnalysisClick(Snapshot snapshot) {
                if (selectedProject != null) {
                    viewModel.runAiAnalysis(snapshot, selectedProject.getDescription());
                }
            }

            @Override
            public void onDiscussClick(Snapshot snapshot) {
                TabLayout.Tab chatTab = binding.tabLayoutProjectModes.getTabAt(1);
                if (chatTab != null) {
                    chatTab.select();
                }

                String text = "По поводу анализа снимка \"" + snapshot.getTitle() + "\": ";
                binding.inputChatMessage.setText(text);
                binding.inputChatMessage.setSelection(text.length());
                binding.inputChatMessage.requestFocus();

                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(binding.inputChatMessage, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
        binding.rvSnapshotsTimeline.setAdapter(snapshotAdapter);

        binding.rvProjectGalleryGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));
        projectGalleryAdapter = new GalleryAdapter(new ArrayList<>());
        binding.rvProjectGalleryGrid.setAdapter(projectGalleryAdapter);
        binding.rvProjectGalleryGrid.addItemDecoration(new SpaceItemDecoration(6, false));

        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        chatMessageAdapter = new ChatMessageAdapter(new ArrayList<>());
        binding.rvChatMessages.setAdapter(chatMessageAdapter);
    }

    private void setupListeners() {
        View.OnClickListener addProjectClick = v -> {
            CreateProjectBottomSheet bottomSheet = new CreateProjectBottomSheet();
            bottomSheet.show(getChildFragmentManager(), "CreateProjectBottomSheet");
        };

        binding.btnAddProject.setOnClickListener(addProjectClick);
        binding.btnCreateFirstProject.setOnClickListener(addProjectClick);

        binding.btnDeleteProject.setOnClickListener(v -> {
            if (selectedProject != null) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle("Удалить проект?")
                        .setMessage("Вы действительно хотите удалить проект \"" + selectedProject.getName() + "\" и всю историю изменений?")
                        .setPositiveButton("Удалить", (dialog, which) -> {
                            viewModel.deleteProject(selectedProject);
                            selectedProject = null;
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });

        binding.btnAddSnapshot.setOnClickListener(v -> {
            if (selectedProject != null) {
                AddSnapshotBottomSheet bottomSheet = AddSnapshotBottomSheet.newInstance(selectedProject.getId());
                bottomSheet.show(getChildFragmentManager(), "AddSnapshotBottomSheet");
            }
        });

        binding.btnSearchProjects.setOnClickListener(v -> {
            if (binding.layoutSearchProjects.getVisibility() == View.VISIBLE) {
                binding.layoutSearchProjects.setVisibility(View.GONE);
                binding.inputSearchProjects.setText("");
                projectSearchQuery = "";
                filterProjects();
            } else {
                binding.layoutSearchProjects.setVisibility(View.VISIBLE);
                binding.inputSearchProjects.requestFocus();
            }
        });

        binding.inputSearchProjects.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                projectSearchQuery = s.toString();
                filterProjects();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        binding.tabLayoutProjectModes.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    binding.rvSnapshotsTimeline.setVisibility(View.VISIBLE);
                    binding.layoutAiChat.setVisibility(View.GONE);
                    binding.rvProjectGalleryGrid.setVisibility(View.GONE);
                } else if (position == 1) {
                    binding.rvSnapshotsTimeline.setVisibility(View.GONE);
                    binding.layoutAiChat.setVisibility(View.VISIBLE);
                    binding.rvProjectGalleryGrid.setVisibility(View.GONE);
                } else {
                    binding.rvSnapshotsTimeline.setVisibility(View.GONE);
                    binding.layoutAiChat.setVisibility(View.GONE);
                    binding.rvProjectGalleryGrid.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        binding.btnSendChatMessage.setOnClickListener(v -> {
            if (selectedProject != null) {
                String text = binding.inputChatMessage.getText().toString();
                if (!text.trim().isEmpty()) {
                    viewModel.sendChatMessage(selectedProject.getId(), text);
                    binding.inputChatMessage.setText("");
                }
            }
        });

        keyboardLayoutListener = () -> {
            if (binding == null) {
                return;
            }
            android.graphics.Rect visibleRect = new android.graphics.Rect();
            binding.getRoot().getWindowVisibleDisplayFrame(visibleRect);
            int screenHeight = binding.getRoot().getRootView().getHeight();
            int keyboardHeight = screenHeight - visibleRect.bottom;
            boolean isKeyboardVisible = keyboardHeight > screenHeight * 0.15f;

            if (getActivity() instanceof ru.mogcommunity.rbr_project.MainActivity) {
                ((ru.mogcommunity.rbr_project.MainActivity) getActivity())
                        .setBottomNavigationVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
            }

            if (isKeyboardVisible) {
                if (chatMessageAdapter != null && chatMessageAdapter.getItemCount() > 0) {
                    binding.rvChatMessages.post(() -> {
                        if (binding != null && chatMessageAdapter != null) {
                            binding.rvChatMessages.scrollToPosition(chatMessageAdapter.getItemCount() - 1);
                        }
                    });
                }
            }
        };

        binding.getRoot().getViewTreeObserver().addOnGlobalLayoutListener(keyboardLayoutListener);
    }

    private void filterProjects() {
        List<Project> filtered = new ArrayList<>();
        String query = projectSearchQuery.trim().toLowerCase();

        for (Project p : allProjectsList) {
            if (query.isEmpty() ||
                    p.getName().toLowerCase().contains(query) ||
                    (p.getDescription() != null && p.getDescription().toLowerCase().contains(query))) {
                filtered.add(p);
            }
        }

        projectAdapter.updateList(filtered);

        if (filtered.isEmpty()) {
            binding.cardEmptyState.setVisibility(View.VISIBLE);
            binding.scrollProjectDetails.setVisibility(View.GONE);
        } else {
            binding.cardEmptyState.setVisibility(View.GONE);
            binding.scrollProjectDetails.setVisibility(View.VISIBLE);

            if (selectedProject == null) {
                String lastId = viewModel.getActiveProjectId();
                if (lastId == null || lastId.isEmpty()) {
                    if (getContext() != null) {
                        lastId = ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(getContext()).getLastProjectId();
                    } else {
                        lastId = "";
                    }
                }
                Project lastProject = null;
                if (lastId != null && !lastId.isEmpty()) {
                    for (Project p : filtered) {
                        if (p.getId().equals(lastId)) {
                            lastProject = p;
                            break;
                        }
                    }
                }
                if (lastProject != null) {
                    selectProject(lastProject);
                } else {
                    selectProject(filtered.get(0));
                }
            } else {
                boolean found = false;
                for (Project p : filtered) {
                    if (p.getId().equals(selectedProject.getId())) {
                        selectProject(p);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    selectProject(filtered.get(0));
                }
            }
        }
    }

    private void observeViewModel() {
        viewModel.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
            if (projects != null) {
                allProjectsList = projects;
            } else {
                allProjectsList = new ArrayList<>();
            }
            filterProjects();
        });

        viewModel.getAiError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.clearAiError();
            }
        });

        viewModel.getIsAiLoading().observe(getViewLifecycleOwner(), loading -> {
            snapshotAdapter.setAiLoading(loading);
        });

        viewModel.getIsChatLoading().observe(getViewLifecycleOwner(), loading -> {
            binding.chatProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
            binding.btnSendChatMessage.setEnabled(!loading);
            binding.inputChatMessage.setEnabled(!loading);
        });

        viewModel.getChatError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.clearChatError();
            }
        });
    }

    private void selectProject(Project project) {
        boolean isNewProject = selectedProject == null || !selectedProject.getId().equals(project.getId());
        selectedProject = project;
        viewModel.setActiveProjectId(project.getId());
        if (getContext() != null) {
            ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(getContext()).setLastProjectId(project.getId());
        }
        projectAdapter.setSelectedProject(project);

        if (allProjectsList != null) {
            for (int i = 0; i < allProjectsList.size(); i++) {
                if (allProjectsList.get(i).getId().equals(project.getId())) {
                    binding.rvProjectsHorizontal.scrollToPosition(i);
                    break;
                }
            }
        }

        binding.textProjectName.setText(project.getName());
        binding.textProjectDesc.setText(project.getDescription());
        binding.textProjectDate.setText("Дата начала: " + dateFormat.format(new Date(project.getCreatedAt())));
        binding.textSnapshotsCount.setText("Снимков: " + project.getSnapshotsCount());

        if (isNewProject) {
            TabLayout.Tab firstTab = binding.tabLayoutProjectModes.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();
            }

            if (currentSnapshotsLiveData != null) {
                currentSnapshotsLiveData.removeObservers(getViewLifecycleOwner());
            }
            currentSnapshotsLiveData = viewModel.getSnapshotsForProject(project.getId());
            currentSnapshotsLiveData.observe(getViewLifecycleOwner(), snapshots -> {
                snapshotAdapter.updateList(snapshots);

                List<Snapshot> gallerySnapshots = new ArrayList<>();
                if (snapshots != null) {
                    for (Snapshot s : snapshots) {
                        if (s.getImageUrl() != null && !s.getImageUrl().trim().isEmpty()) {
                            gallerySnapshots.add(s);
                        }
                    }
                }
                projectGalleryAdapter.updateList(gallerySnapshots);
            });

            if (currentChatLiveData != null) {
                currentChatLiveData.removeObservers(getViewLifecycleOwner());
            }
            currentChatLiveData = viewModel.getChatMessagesForProject(project.getId());
            currentChatLiveData.observe(getViewLifecycleOwner(), messages -> {
                chatMessageAdapter.updateList(messages);
                if (messages != null && !messages.isEmpty()) {
                    binding.cardEmptyChat.setVisibility(View.GONE);
                    binding.rvChatMessages.setVisibility(View.VISIBLE);
                    binding.rvChatMessages.post(() -> binding.rvChatMessages.scrollToPosition(messages.size() - 1));
                } else {
                    binding.cardEmptyChat.setVisibility(View.VISIBLE);
                    binding.rvChatMessages.setVisibility(View.GONE);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null && keyboardLayoutListener != null) {
            binding.getRoot().getViewTreeObserver().removeOnGlobalLayoutListener(keyboardLayoutListener);
        }
        if (getActivity() instanceof ru.mogcommunity.rbr_project.MainActivity) {
            ((ru.mogcommunity.rbr_project.MainActivity) getActivity()).setBottomNavigationVisibility(View.VISIBLE);
        }
        super.onDestroyView();
        binding = null;
    }
}