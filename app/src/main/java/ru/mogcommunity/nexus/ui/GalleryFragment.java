package ru.mogcommunity.rbrproject.ui;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import ru.mogcommunity.rbrproject.R;
import ru.mogcommunity.rbrproject.data.model.Project;
import ru.mogcommunity.rbrproject.data.model.Snapshot;
import ru.mogcommunity.rbrproject.databinding.FragmentGalleryBinding;
import ru.mogcommunity.rbrproject.ui.adapter.GalleryProjectAdapter;
import ru.mogcommunity.rbrproject.ui.adapter.GalleryProjectAdapter.GalleryProject;
import ru.mogcommunity.rbrproject.ui.adapter.ProjectPhotoAdapter;
import ru.mogcommunity.rbrproject.ui.helper.SpaceItemDecoration;
import ru.mogcommunity.rbrproject.viewmodel.ProjectViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class GalleryFragment extends Fragment {
    private FragmentGalleryBinding binding;
    private ProjectViewModel viewModel;
    private GalleryProjectAdapter adapter;

    private List<Project> allProjects = new ArrayList<>();
    private List<Snapshot> allSnapshots = new ArrayList<>();
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProjectViewModel.class);

        setupRecyclerView();
        setupSearchListener();
        observeViewModel();
    }

    private void setupRecyclerView() {
        binding.rvGallery.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GalleryProjectAdapter(new ArrayList<>(), new ProjectPhotoAdapter.OnPhotoClickListener() {
            @Override
            public void onPhotoClick(Snapshot snapshot) {
                showFullScreenImage(snapshot.getImageUrl());
            }
        });
        binding.rvGallery.setAdapter(adapter);
        binding.rvGallery.addItemDecoration(new SpaceItemDecoration(8, false));
    }

    private void setupSearchListener() {
        binding.inputSearchGallery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                combineAndDisplayData();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void observeViewModel() {
        viewModel.getAllProjects().observe(getViewLifecycleOwner(), projects -> {
            if (projects != null) {
                allProjects = projects;
                combineAndDisplayData();
            }
        });

        viewModel.getAllGallerySnapshots().observe(getViewLifecycleOwner(), snapshots -> {
            if (snapshots != null) {
                allSnapshots = snapshots;
                combineAndDisplayData();
            }
        });
    }

    private void combineAndDisplayData() {
        List<GalleryProject> galleryProjectsList = new ArrayList<>();

        Map<String, List<Snapshot>> snapshotsByProject = new HashMap<>();
        for (Snapshot snapshot : allSnapshots) {
            if (snapshot.getImageUrl() != null && !snapshot.getImageUrl().trim().isEmpty()) {
                List<Snapshot> list = snapshotsByProject.get(snapshot.getProjectId());
                if (list == null) {
                    list = new ArrayList<>();
                    snapshotsByProject.put(snapshot.getProjectId(), list);
                }
                list.add(snapshot);
            }
        }

        for (Project project : allProjects) {
            List<Snapshot> projectSnapshots = snapshotsByProject.get(project.getId());
            if (projectSnapshots == null) {
                projectSnapshots = new ArrayList<>();
            }

            Collections.sort(projectSnapshots, (s1, s2) -> Long.compare(s2.getTimestamp(), s1.getTimestamp()));

            if (!projectSnapshots.isEmpty()) {
                galleryProjectsList.add(new GalleryProject(project, projectSnapshots));
            }
        }

        List<GalleryProject> filteredList = new ArrayList<>();
        String query = searchQuery.trim().toLowerCase();

        for (GalleryProject gp : galleryProjectsList) {
            if (query.isEmpty() ||
                    gp.getProject().getName().toLowerCase().contains(query) ||
                    (gp.getProject().getDescription() != null && gp.getProject().getDescription().toLowerCase().contains(query))) {
                filteredList.add(gp);
            }
        }

        adapter.updateList(filteredList);
    }

    private void showFullScreenImage(String imageUrl) {
        if (getContext() == null || imageUrl == null || imageUrl.trim().isEmpty()) return;

        Dialog dialog = new Dialog(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.dialog_full_screen_image);

        ImageView imageView = dialog.findViewById(R.id.img_full_screen);
        Glide.with(this)
                .load(Uri.parse(imageUrl))
                .error(android.R.drawable.ic_dialog_alert)
                .into(imageView);

        imageView.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
