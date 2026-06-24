package ru.mogcommunity.rbr_project.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.color.DynamicColors;
import ru.mogcommunity.rbr_project.MainActivity;
import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.databinding.ActivitySplashBinding;
import com.google.firebase.auth.FirebaseAuth;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;

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
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1500);
        binding.splashTitle.startAnimation(fadeIn);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            boolean firebaseLoggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
            boolean localLoggedIn = !PreferenceManager.getInstance(SplashActivity.this).getUserId().isEmpty();
            
            Intent intent;
            if (firebaseLoggedIn || localLoggedIn) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
            startActivity(intent);
            finish();
        }, 2000);
    }
}

