package ru.mogcommunity.rbr_project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.color.DynamicColors;
import ru.mogcommunity.rbr_project.MainActivity;
import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.databinding.ActivityLoginBinding;
import ru.mogcommunity.rbr_project.viewmodel.AuthViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;
    private boolean isSignUpMode = false;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        float fontScale = ru.mogcommunity.rbr_project.data.PreferenceManager.getInstance(newBase).getFontScale();
        android.content.res.Configuration config = new android.content.res.Configuration(newBase.getResources().getConfiguration());
        config.fontScale = fontScale;
        android.content.Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PreferenceManager preferenceManager = PreferenceManager.getInstance(this);
        if (preferenceManager.isDynamicColorsEnabled()) {
            DynamicColors.applyToActivityIfAvailable(this);
        }
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupTabs();
        setupListeners();
        observeViewModel();
    }

    private void setupTabs() {
        binding.loginTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position == 0) {
                    binding.panelAutonomous.setVisibility(View.VISIBLE);
                    binding.panelCloud.setVisibility(View.GONE);
                } else {
                    binding.panelAutonomous.setVisibility(View.GONE);
                    binding.panelCloud.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupListeners() {
        binding.btnEnterAutonomous.setOnClickListener(v -> viewModel.signInAnonymously());

        binding.textToggleSignup.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            if (isSignUpMode) {
                binding.btnEnterCloud.setText("Зарегистрироваться");
                binding.textToggleSignup.setText("Уже есть аккаунт? Войти");
            } else {
                binding.btnEnterCloud.setText("Войти в облако");
                binding.textToggleSignup.setText("Нет аккаунта? Зарегистрироваться");
            }
        });

        binding.btnEnterCloud.setOnClickListener(v -> {
            String email = binding.inputEmail.getText() != null ? binding.inputEmail.getText().toString() : "";
            String password = binding.inputPassword.getText() != null ? binding.inputPassword.getText().toString() : "";
            if (isSignUpMode) {
                viewModel.signUpWithEmailAndPassword(email, password);
            } else {
                viewModel.signInWithEmailAndPassword(email, password);
            }
        });
    }

    private void observeViewModel() {
        viewModel.getIsLoggedIn().observe(this, loggedIn -> {
            if (loggedIn) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        viewModel.getIsLoading().observe(this, loading -> {
            binding.loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);
        });

        viewModel.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

