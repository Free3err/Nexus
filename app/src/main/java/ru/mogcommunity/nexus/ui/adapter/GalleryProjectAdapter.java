package ru.mogcommunity.rbr_project.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ru.mogcommunity.rbr_project.data.model.Project;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.databinding.ItemGalleryProjectBinding;

import java.util.List;

public class GalleryProjectAdapter extends RecyclerView.Adapter<GalleryProjectAdapter.GalleryProjectViewHolder> {

    public static class GalleryProject {
        private final Project project;
        private final List<Snapshot> snapshots;

        public GalleryProject(Project project, List<Snapshot> snapshots) {
            this.project = project;
            this.snapshots = snapshots;
        }

        public Project getProject() {
            return project;
        }

        public List<Snapshot> getSnapshots() {
            return snapshots;
        }
    }

    private final List<GalleryProject> items;
    private final ProjectPhotoAdapter.OnPhotoClickListener photoClickListener;

    public GalleryProjectAdapter(List<GalleryProject> items, ProjectPhotoAdapter.OnPhotoClickListener photoClickListener) {
        this.items = items;
        this.photoClickListener = photoClickListener;
    }

    @NonNull
    @Override
    public GalleryProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGalleryProjectBinding binding = ItemGalleryProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GalleryProjectViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryProjectViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateList(List<GalleryProject> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    class GalleryProjectViewHolder extends RecyclerView.ViewHolder {
        private final ItemGalleryProjectBinding binding;

        GalleryProjectViewHolder(ItemGalleryProjectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(GalleryProject item) {
            binding.textProjectName.setText(item.getProject().getName());
            binding.textProjectDesc.setText(item.getProject().getDescription());

            binding.rvProjectPhotos.setLayoutManager(new LinearLayoutManager(
                    itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            ProjectPhotoAdapter photoAdapter = new ProjectPhotoAdapter(item.getSnapshots(), photoClickListener);
            binding.rvProjectPhotos.setAdapter(photoAdapter);
        }
    }
}
