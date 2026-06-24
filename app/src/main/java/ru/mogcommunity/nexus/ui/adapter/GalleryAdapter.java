package ru.mogcommunity.rbr_project.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.databinding.ItemGalleryBinding;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder> {
    private final List<Snapshot> snapshots;
    private final ProjectPhotoAdapter.OnPhotoClickListener listener;

    public GalleryAdapter(List<Snapshot> snapshots, ProjectPhotoAdapter.OnPhotoClickListener listener) {
        this.snapshots = snapshots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGalleryBinding binding = ItemGalleryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new GalleryViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        Snapshot snapshot = snapshots.get(position);
        holder.bind(snapshot);
    }

    @Override
    public int getItemCount() {
        return snapshots.size();
    }

    public void updateList(List<Snapshot> newSnapshots) {
        snapshots.clear();
        if (newSnapshots != null) {
            snapshots.addAll(newSnapshots);
        }
        notifyDataSetChanged();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        private final ItemGalleryBinding binding;
        private final ProjectPhotoAdapter.OnPhotoClickListener listener;

        private final java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault());

        GalleryViewHolder(ItemGalleryBinding binding, ProjectPhotoAdapter.OnPhotoClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Snapshot snapshot) {
            binding.textGalleryCaption.setText(snapshot.getTitle());
            binding.textGalleryDate.setText(dateFormat.format(new java.util.Date(snapshot.getTimestamp())));

            String imgUrl = snapshot.getImageUrl();
            if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(Uri.parse(imgUrl))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(binding.imgGalleryItem);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(snapshot);
                }
            });
        }
    }
}

