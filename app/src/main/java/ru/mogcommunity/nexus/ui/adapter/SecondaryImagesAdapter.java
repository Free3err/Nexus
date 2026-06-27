package ru.mogcommunity.rbrproject.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import ru.mogcommunity.rbrproject.databinding.ItemSecondaryImageBinding;

import java.util.List;

public class SecondaryImagesAdapter extends RecyclerView.Adapter<SecondaryImagesAdapter.ViewHolder> {
    private final List<String> imageUrls;
    private final OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(String url);
    }

    public SecondaryImagesAdapter(List<String> imageUrls, OnImageClickListener listener) {
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSecondaryImageBinding binding = ItemSecondaryImageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(imageUrls.get(position));
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSecondaryImageBinding binding;

        ViewHolder(ItemSecondaryImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String url) {
            if (url != null && !url.trim().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(Uri.parse(url.trim()))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(binding.imgSecondary);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(url);
                }
            });
        }
    }
}
