package ru.mogcommunity.rbr_project.ui.adapter;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.data.model.Snapshot;
import ru.mogcommunity.rbr_project.databinding.ItemSnapshotTimelineBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SnapshotAdapter extends RecyclerView.Adapter<SnapshotAdapter.SnapshotViewHolder> {
    private final List<Snapshot> snapshots;
    private final OnSnapshotClickListener listener;
    private boolean isAiLoading = false;
    private String loadingSnapshotId = "";
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());

    public interface OnSnapshotClickListener {
        void onDeleteClick(Snapshot snapshot);
        void onAiAnalysisClick(Snapshot snapshot);
        void onDiscussClick(Snapshot snapshot);
    }

    public SnapshotAdapter(List<Snapshot> snapshots, OnSnapshotClickListener listener) {
        this.snapshots = snapshots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SnapshotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSnapshotTimelineBinding binding = ItemSnapshotTimelineBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SnapshotViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SnapshotViewHolder holder, int position) {
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

    public void setAiLoading(boolean loading) {
        this.isAiLoading = loading;
        if (!loading) {
            this.loadingSnapshotId = "";
        }
        notifyDataSetChanged();
    }

    class SnapshotViewHolder extends RecyclerView.ViewHolder {
        private final ItemSnapshotTimelineBinding binding;

        SnapshotViewHolder(ItemSnapshotTimelineBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Snapshot snapshot) {
            binding.textSnapshotTitle.setText(snapshot.getTitle());
            binding.textSnapshotTime.setText(dateTimeFormat.format(new Date(snapshot.getTimestamp())));
            binding.textSnapshotDesc.setText(snapshot.getDescription());

            String imgUrl = snapshot.getImageUrl();
            if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                binding.imgSnapshot.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(Uri.parse(imgUrl))
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(binding.imgSnapshot);
            } else {
                binding.imgSnapshot.setVisibility(View.GONE);
            }

            int errorColor;
            int outlineColor;

            android.util.TypedValue typedValueError = new android.util.TypedValue();
            if (itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValueError, true)) {
                errorColor = typedValueError.data;
            } else {
                errorColor = itemView.getContext().getResources().getColor(R.color.error);
            }

            android.util.TypedValue typedValueOutline = new android.util.TypedValue();
            if (itemView.getContext().getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValueOutline, true)) {
                outlineColor = typedValueOutline.data;
            } else {
                outlineColor = itemView.getContext().getResources().getColor(R.color.outline);
            }

            if (snapshot.isHasError()) {
                binding.cardSnapshot.setStrokeColor(ColorStateList.valueOf(errorColor));
                binding.cardSnapshot.setStrokeWidth(4);
                binding.layoutErrorDetails.setVisibility(View.VISIBLE);
                binding.textErrorLog.setText(snapshot.getErrorLog());

                String aiPlan = snapshot.getAiAnalysisPlan();
                if (aiPlan != null && !aiPlan.trim().isEmpty()) {
                    binding.layoutAiResponse.setVisibility(View.VISIBLE);
                    binding.textAiPlan.setText(aiPlan);

                    ru.mogcommunity.rbr_project.data.PreferenceManager preferenceManager =
                            ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(itemView.getContext());
                    String selectedModel = preferenceManager.getSelectedModel();
                    String modelLabel = "Gemini 3.1 Flash Lite";
                    if ("qwen_1.5b".equals(selectedModel)) {
                        modelLabel = "Qwen2.5 1.5B";
                    }
                    binding.textAiLabel.setText("Рекомендация ИИ (" + modelLabel + "):" );

                    binding.btnDiscussAi.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onDiscussClick(snapshot);
                        }
                    });
                } else {
                    binding.layoutAiResponse.setVisibility(View.GONE);
                }

                boolean itemIsLoading = isAiLoading && snapshot.getId().equals(loadingSnapshotId);
                binding.progressAi.setVisibility(itemIsLoading ? View.VISIBLE : View.GONE);
                binding.btnAiAnalysis.setVisibility(itemIsLoading ? View.INVISIBLE : View.VISIBLE);

                binding.btnAiAnalysis.setOnClickListener(v -> {
                    loadingSnapshotId = snapshot.getId();
                    notifyItemChanged(getAdapterPosition());
                    if (listener != null) {
                        listener.onAiAnalysisClick(snapshot);
                    }
                });
            } else {
                binding.cardSnapshot.setStrokeColor(ColorStateList.valueOf(outlineColor));
                binding.cardSnapshot.setStrokeWidth(2);
                binding.layoutErrorDetails.setVisibility(View.GONE);
            }

            binding.btnDeleteSnapshot.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(snapshot);
                }
            });
        }
    }
}

