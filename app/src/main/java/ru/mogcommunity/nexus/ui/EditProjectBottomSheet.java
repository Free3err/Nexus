package ru.mogcommunity.rbr_project.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import ru.mogcommunity.rbr_project.R;
import ru.mogcommunity.rbr_project.databinding.BottomSheetEditProjectBinding;
import ru.mogcommunity.rbr_project.viewmodel.ProjectViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProjectBottomSheet extends BottomSheetDialogFragment {
    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";
    private static final String ARG_PROJECT_DESC = "project_desc";
    private static final String ARG_PROJECT_CONFIG = "project_config";

    private BottomSheetEditProjectBinding binding;
    private ProjectViewModel viewModel;

    private String projectId;
    private String projectName;
    private String projectDesc;
    private String projectConfig;

    public static EditProjectBottomSheet newInstance(String projectId, String name, String desc, String configEnv) {
        EditProjectBottomSheet fragment = new EditProjectBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_PROJECT_ID, projectId);
        args.putString(ARG_PROJECT_NAME, name);
        args.putString(ARG_PROJECT_DESC, desc);
        args.putString(ARG_PROJECT_CONFIG, configEnv);
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
        android.content.Context context = getContext();
        if (context != null && ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(context).isDynamicColorsEnabled()) {
            context = com.google.android.material.color.DynamicColors.wrapContextIfAvailable(context, R.style.DynamicBottomSheetDialogTheme);
            return new com.google.android.material.bottomsheet.BottomSheetDialog(context, R.style.DynamicBottomSheetDialogTheme);
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            projectId = getArguments().getString(ARG_PROJECT_ID);
            projectName = getArguments().getString(ARG_PROJECT_NAME);
            projectDesc = getArguments().getString(ARG_PROJECT_DESC);
            projectConfig = getArguments().getString(ARG_PROJECT_CONFIG);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.content.Context themedContext = new android.view.ContextThemeWrapper(requireContext(), getTheme());
        LayoutInflater themedInflater = inflater.cloneInContext(themedContext);
        binding = BottomSheetEditProjectBinding.inflate(themedInflater, container, false);
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

        applyThemedColoring();

        binding.inputProjectName.setText(projectName);
        binding.inputProjectDesc.setText(projectDesc);
        binding.inputProjectConfig.setText(projectConfig != null ? projectConfig : "");

        binding.btnSubmitProject.setOnClickListener(v -> {
            String name = binding.inputProjectName.getText() != null ? binding.inputProjectName.getText().toString().trim() : "";
            String desc = binding.inputProjectDesc.getText() != null ? binding.inputProjectDesc.getText().toString().trim() : "";
            String config = binding.inputProjectConfig.getText() != null ? binding.inputProjectConfig.getText().toString().trim() : "";

            if (name.isEmpty()) {
                binding.inputLayoutProjectName.setError("Введите название проекта");
                return;
            }

            binding.inputLayoutProjectName.setError(null);
            
            try {
                viewModel.updateProjectDetails(projectId, name, desc, config);
                dismiss();
            } catch (Exception e) {
                android.util.Log.e("RBR_EditProject", "Error editing project", e);
                com.google.android.material.snackbar.Snackbar.make(view, "Ошибка редактирования проекта: " + e.getMessage(), com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void applyThemedColoring() {
        int primaryColor;
        int outlineColor;

        android.util.TypedValue typedValue = new android.util.TypedValue();
        android.content.Context ctx = getContext();
        if (ctx != null) {
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
        } else {
            primaryColor = getResources().getColor(R.color.primary);
            outlineColor = getResources().getColor(R.color.outline);
        }

        int[][] states = new int[][] {
            new int[] { android.R.attr.state_focused },
            new int[] { android.R.attr.state_hovered },
            new int[] {}
        };
        int[] colors = new int[] {
            primaryColor,
            primaryColor,
            outlineColor
        };
        android.content.res.ColorStateList selectorStateList = new android.content.res.ColorStateList(states, colors);

        binding.inputLayoutProjectName.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutProjectDesc.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutProjectConfig.setBoxStrokeColorStateList(selectorStateList);
        binding.inputLayoutProjectName.setError(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
