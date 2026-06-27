package ru.mogcommunity.rbrproject.ui.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ru.mogcommunity.rbrproject.R;
import ru.mogcommunity.rbrproject.data.model.Project;
import ru.mogcommunity.rbrproject.databinding.ItemProjectHorizontalBinding;

import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ProjectViewHolder> {
    private final List<Project> projects;
    private final OnProjectClickListener listener;
    private Project selectedProject;

    public interface OnProjectClickListener {
        void onProjectClick(Project project);
    }

    public ProjectAdapter(List<Project> projects, OnProjectClickListener listener) {
        this.projects = projects;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectHorizontalBinding binding = ItemProjectHorizontalBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
        Project project = projects.get(position);
        holder.bind(project, project.equals(selectedProject));
    }

    @Override
    public int getItemCount() {
        return projects.size();
    }

    public void updateList(List<Project> newProjects) {
        projects.clear();
        if (newProjects != null) {
            projects.addAll(newProjects);
        }
        notifyDataSetChanged();
    }

    public void setSelectedProject(Project project) {
        if (this.selectedProject != null && this.selectedProject.getId().equals(project.getId())) {
            this.selectedProject = project;
            return;
        }
        int oldIndex = -1;
        int newIndex = -1;
        
        for (int i = 0; i < projects.size(); i++) {
            if (selectedProject != null && projects.get(i).getId().equals(selectedProject.getId())) {
                oldIndex = i;
            }
            if (projects.get(i).getId().equals(project.getId())) {
                newIndex = i;
            }
        }
        
        this.selectedProject = project;
        
        if (oldIndex != -1) {
            notifyItemChanged(oldIndex);
        }
        if (newIndex != -1) {
            notifyItemChanged(newIndex);
        }
    }

    class ProjectViewHolder extends RecyclerView.ViewHolder {
        private final ItemProjectHorizontalBinding binding;

        ProjectViewHolder(ItemProjectHorizontalBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Project project, boolean isSelected) {
            binding.textProjectTitle.setText(project.getName());

            int primaryColor;
            int outlineColor;

            android.util.TypedValue typedValuePrimary = new android.util.TypedValue();
            if (itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValuePrimary, true)) {
                primaryColor = typedValuePrimary.data;
            } else {
                primaryColor = itemView.getContext().getResources().getColor(R.color.primary);
            }

            android.util.TypedValue typedValueOutline = new android.util.TypedValue();
            if (itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValueOutline, true)) {
                outlineColor = typedValueOutline.data;
            } else {
                outlineColor = itemView.getContext().getResources().getColor(R.color.outline);
            }

            binding.cardProject.setStrokeColor(ColorStateList.valueOf(isSelected ? primaryColor : outlineColor));
            binding.cardProject.setStrokeWidth(isSelected ? 6 : 3);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProjectClick(project);
                }
            });
        }
    }
}

