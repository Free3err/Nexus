package ru.mogcommunity.rbr_project.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.databinding.ItemProjectPhotoBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProjectPhotoAdapter extends RecyclerView.Adapter<ProjectPhotoAdapter.PhotoViewHolder> {
    private final List<Snapshot> snapshots;
    private final OnPhotoClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    public interface OnPhotoClickListener {
        void onPhotoClick(Snapshot snapshot);
    }

    public ProjectPhotoAdapter(List<Snapshot> snapshots, OnPhotoClickListener listener) {
        this.snapshots = snapshots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProjectPhotoBinding binding = ItemProjectPhotoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PhotoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        holder.bind(snapshots.get(position));
    }

    @Override
    public int getItemCount() {
        return snapshots.size();
    }

    class PhotoViewHolder extends RecyclerView.ViewHolder {
        private final ItemProjectPhotoBinding binding;

        PhotoViewHolder(ItemProjectPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Snapshot snapshot) {
            binding.textPhotoDate.setText(dateFormat.format(new Date(snapshot.getTimestamp())));

            String imgUrl = snapshot.getImageUrl();
            if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(Uri.parse(imgUrl))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(binding.imgSnapshotPhoto);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(snapshot);
                }
            });
        }
    }
}
