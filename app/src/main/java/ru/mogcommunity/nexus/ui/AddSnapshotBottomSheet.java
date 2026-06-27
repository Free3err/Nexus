package ru.mogcommunity.rbrproject.ui;

import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import ru.mogcommunity.rbrproject.R;
import ru.mogcommunity.rbrproject.databinding.BottomSheetAddSnapshotBinding;
import ru.mogcommunity.rbrproject.viewmodel.ProjectViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.UUID;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AddSnapshotBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_PROJECT_ID = "project_id";

    private BottomSheetAddSnapshotBinding binding;
    private ProjectViewModel viewModel;
    private String projectId;
    private final List<Uri> selectedPhotoUris = new java.util.ArrayList<>();
    private PhotoPickerAdapter photoPickerAdapter;

    private final ActivityResultLauncher<String> selectPhotosLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris != null && !uris.isEmpty()) {
                    selectedPhotoUris.addAll(uris);
                    updatePhotoPickerAdapter();
                }
            }
    );

    private void updatePhotoPickerAdapter() {
        if (photoPickerAdapter == null) {
            photoPickerAdapter = new PhotoPickerAdapter(
                    selectedPhotoUris,
                    () -> selectPhotosLauncher.launch("image/*"),
                    position -> {
                        selectedPhotoUris.remove((int) position);
                        updatePhotoPickerAdapter();
                    }
            );
            binding.rvMediaPreviews.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(
                    getContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
            binding.rvMediaPreviews.setAdapter(photoPickerAdapter);
        } else {
            photoPickerAdapter.notifyDataSetChanged();
        }
    }

    private class PhotoPickerAdapter extends RecyclerView.Adapter<PhotoPickerAdapter.ViewHolder> {
        private final List<Uri> uris;
        private final Runnable onAddClick;
        private final java.util.function.Consumer<Integer> onDeleteClick;

        public PhotoPickerAdapter(List<Uri> uris, Runnable onAddClick, java.util.function.Consumer<Integer> onDeleteClick) {
            this.uris = uris;
            this.onAddClick = onAddClick;
            this.onDeleteClick = onDeleteClick;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == uris.size()) {
                return 1; 
            }
            return 0; 
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo_picker_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (getItemViewType(position) == 1) {
                holder.bindAddButton();
            } else {
                holder.bindPhoto(uris.get(position), position);
            }
        }

        @Override
        public int getItemCount() {
            return uris.size() + 1; 
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private final ImageView imgPhoto;
            private final View layoutAddButton;
            private final TextView textMainBadge;
            private final View btnDeletePhoto;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgPhoto = itemView.findViewById(R.id.img_photo);
                layoutAddButton = itemView.findViewById(R.id.layout_add_button);
                textMainBadge = itemView.findViewById(R.id.text_main_badge);
                btnDeletePhoto = itemView.findViewById(R.id.btn_delete_photo);
            }

            public void bindAddButton() {
                imgPhoto.setVisibility(View.GONE);
                layoutAddButton.setVisibility(View.VISIBLE);
                textMainBadge.setVisibility(View.GONE);
                btnDeletePhoto.setVisibility(View.GONE);
                itemView.setOnClickListener(v -> {
                    if (onAddClick != null) {
                        onAddClick.run();
                    }
                });
            }

            public void bindPhoto(Uri uri, int position) {
                imgPhoto.setVisibility(View.VISIBLE);
                layoutAddButton.setVisibility(View.GONE);
                textMainBadge.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                btnDeletePhoto.setVisibility(View.VISIBLE);

                com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(uri)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_dialog_alert)
                        .into(imgPhoto);

                btnDeletePhoto.setOnClickListener(v -> {
                    if (onDeleteClick != null) {
                        onDeleteClick.accept(position);
                    }
                });
                itemView.setOnClickListener(v -> {
                    if (position > 0 && position < uris.size()) {
                        Uri clickedUri = uris.remove(position);
                        uris.add(0, clickedUri);
                        notifyDataSetChanged();
                    }
                });
            }
        }
    }

    public static AddSnapshotBottomSheet newInstance(String projectId) {
        AddSnapshotBottomSheet fragment = new AddSnapshotBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTheme() {
        if (getContext() != null && ru.mogcommunity.rbrproject.data.PreferenceManager.getInstance(getContext()).isDynamicColorsEnabled()) {
            return R.style.DynamicBottomSheetDialogTheme;
        } else {
            return R.style.CustomBottomSheetDialogTheme;
        }
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.content.Context context = requireContext();
        if (ru.mogcommunity.rbrproject.data.PreferenceManager.getInstance(context).isDynamicColorsEnabled()) {
            context = com.google.android.material.color.DynamicColors.wrapContextIfAvailable(context, getTheme());
        }
        return new com.google.android.material.bottomsheet.BottomSheetDialog(context, getTheme());
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.content.Context themedContext = new android.view.ContextThemeWrapper(requireContext(), getTheme());
        LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = BottomSheetAddSnapshotBinding.inflate(themedInflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        android.app.Dialog dialog = getDialog();
        if (dialog instanceof com.google.android.material.bottomsheet.BottomSheetDialog) {
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
                (com.google.android.material.bottomsheet.BottomSheetDialog) dialog;
            
            if (bottomSheetDialog.getWindow() != null) {
                bottomSheetDialog.getWindow().setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                );
            }

            View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<?> behavior = 
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ProjectViewModel.class);

        setupListeners();
        updatePhotoPickerAdapter();
        applyErrorColoring(binding.switchError.isChecked());
    }

    private void setupListeners() {
        binding.switchError.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.inputLayoutErrorLog.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            applyErrorColoring(isChecked);
        });

        binding.btnSubmitSnapshot.setOnClickListener(v -> {
            String title = binding.inputSnapshotTitle.getText() != null ? binding.inputSnapshotTitle.getText().toString().trim() : "";
            String desc = binding.inputSnapshotDesc.getText() != null ? binding.inputSnapshotDesc.getText().toString().trim() : "";
            String tags = binding.inputSnapshotTags.getText() != null ? binding.inputSnapshotTags.getText().toString().trim() : "";
            boolean hasError = binding.switchError.isChecked();
            String errorLog = hasError && binding.inputErrorLog.getText() != null ? binding.inputErrorLog.getText().toString().trim() : "";

            if (title.isEmpty()) {
                binding.inputLayoutSnapshotTitle.setError("Введите название этапа");
                return;
            }

            String snapshotId = UUID.randomUUID().toString();

            Uri localImageUri = selectedPhotoUris.isEmpty() ? null : selectedPhotoUris.get(0);
            String localImageString = localImageUri != null ? localImageUri.toString() : "";

            List<Uri> secondaryImageUris = new java.util.ArrayList<>();
            if (selectedPhotoUris.size() > 1) {
                secondaryImageUris.addAll(selectedPhotoUris.subList(1, selectedPhotoUris.size()));
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < secondaryImageUris.size(); i++) {
                sb.append(secondaryImageUris.get(i).toString());
                if (i < secondaryImageUris.size() - 1) {
                    sb.append(",");
                }
            }
            String secondaryImages = sb.toString();

            viewModel.addSnapshot(snapshotId, projectId, title, desc, hasError, errorLog, localImageString, localImageUri, tags, secondaryImages, secondaryImageUris);
            dismiss();
        });
    }

    private void applyErrorColoring(boolean hasError) {
        int errorColor;
        int primaryColor;
        int outlineColor;
        int surfaceVariantColor;

        int errorContainerColor;

        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getDialog() != null) {
            android.content.Context ctx = requireDialog().getContext();
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true)) {
                errorColor = typedValue.data;
            } else {
                errorColor = getResources().getColor(R.color.error);
            }
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorErrorContainer, typedValue, true)) {
                errorContainerColor = typedValue.data;
            } else {
                errorContainerColor = androidx.core.graphics.ColorUtils.setAlphaComponent(errorColor, 76);
            }
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true)) {
                primaryColor = typedValue.data;
            } else {
                primaryColor = getResources().getColor(R.color.primary);
            }
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOutline, typedValue, true)) {
                outlineColor = typedValue.data;
            } else {
                outlineColor = getResources().getColor(R.color.outline);
            }
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)) {
                surfaceVariantColor = typedValue.data;
            } else {
                surfaceVariantColor = getResources().getColor(R.color.surface_variant);
            }
        } else {
            errorColor = getResources().getColor(R.color.error);
            errorContainerColor = androidx.core.graphics.ColorUtils.setAlphaComponent(errorColor, 76);
            primaryColor = getResources().getColor(R.color.primary);
            outlineColor = getResources().getColor(R.color.outline);
            surfaceVariantColor = getResources().getColor(R.color.surface_variant);
        }

        binding.switchError.setThumbTintList(ColorStateList.valueOf(hasError ? errorColor : outlineColor));
        binding.switchError.setTrackTintList(ColorStateList.valueOf(hasError ? errorContainerColor : surfaceVariantColor));

        binding.cardErrorToggle.setStrokeColor(ColorStateList.valueOf(hasError ? errorColor : outlineColor));

        int[][] states = new int[][] {
            new int[] { android.R.attr.state_focused },
            new int[] { android.R.attr.state_hovered },
            new int[] {}
        };
        int[] colors;
        if (hasError) {
            colors = new int[] {
                errorColor,
                errorColor,
                errorColor
            };
        } else {
            colors = new int[] {
                primaryColor,
                primaryColor,
                outlineColor
            };
        }
        ColorStateList selectorStateList = new ColorStateList(states, colors);

        binding.inputLayoutSnapshotTitle.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutSnapshotDesc.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutSnapshotTags.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutErrorLog.setBoxStrokeColorStateList(selectorStateList);

        binding.inputLayoutSnapshotTitle.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}

