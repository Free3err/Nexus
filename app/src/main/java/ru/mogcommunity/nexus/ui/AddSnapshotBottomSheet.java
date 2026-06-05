package ru.mogcommunity.rbr_project.ui;

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

import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.databinding.BottomSheetAddSnapshotBinding;
import ru.mogcommunity.rbr_project.viewmodel.ProjectViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.UUID;

public class AddSnapshotBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_PROJECT_ID = "project_id";

    private BottomSheetAddSnapshotBinding binding;
    private ProjectViewModel viewModel;
    private String projectId;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    binding.imgPreview.setVisibility(View.VISIBLE);
                    binding.imgPreview.setImageURI(uri);
                }
            }
    );

    public static AddSnapshotBottomSheet newInstance(String projectId) {
        AddSnapshotBottomSheet fragment = new AddSnapshotBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getTheme() {
        if (getContext() != null && ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(getContext()).isDynamicColorsEnabled()) {
            return R.style.DynamicBottomSheetDialogTheme;
        } else {
            return R.style.CustomBottomSheetDialogTheme;
        }
    }

    @NonNull
    @Override
    public android.app.Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        android.content.Context context = requireContext();
        if (ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(context).isDynamicColorsEnabled()) {
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
        applyErrorColoring(binding.switchError.isChecked());
    }

    private void setupListeners() {
        binding.btnTakePhoto.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        binding.switchError.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.inputLayoutErrorLog.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            applyErrorColoring(isChecked);
        });

        binding.btnSubmitSnapshot.setOnClickListener(v -> {
            String title = binding.inputSnapshotTitle.getText() != null ? binding.inputSnapshotTitle.getText().toString().trim() : "";
            String desc = binding.inputSnapshotDesc.getText() != null ? binding.inputSnapshotDesc.getText().toString().trim() : "";
            boolean hasError = binding.switchError.isChecked();
            String errorLog = hasError && binding.inputErrorLog.getText() != null ? binding.inputErrorLog.getText().toString().trim() : "";

            if (title.isEmpty()) {
                binding.inputLayoutSnapshotTitle.setError("Введите название этапа");
                return;
            }

            String snapshotId = UUID.randomUUID().toString();
            String localImageString = selectedImageUri != null ? selectedImageUri.toString() : "";

            viewModel.addSnapshot(snapshotId, projectId, title, desc, hasError, errorLog, localImageString, selectedImageUri);
            dismiss();
        });
    }

    private void applyErrorColoring(boolean hasError) {
        int errorColor;
        int primaryColor;
        int outlineColor;
        int surfaceVariantColor;

        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getDialog() != null) {
            android.content.Context ctx = requireDialog().getContext();
            
            if (ctx.getTheme().resolveAttribute(com.google.android.material.R.attr.colorError, typedValue, true)) {
                errorColor = typedValue.data;
            } else {
                errorColor = getResources().getColor(R.color.error);
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
            primaryColor = getResources().getColor(R.color.primary);
            outlineColor = getResources().getColor(R.color.outline);
            surfaceVariantColor = getResources().getColor(R.color.surface_variant);
        }

        binding.switchError.setThumbTintList(ColorStateList.valueOf(hasError ? errorColor : outlineColor));
        binding.switchError.setTrackTintList(ColorStateList.valueOf(hasError ? errorColor : surfaceVariantColor));

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
        binding.inputLayoutErrorLog.setBoxStrokeColorStateList(selectorStateList);

        binding.inputLayoutSnapshotTitle.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

