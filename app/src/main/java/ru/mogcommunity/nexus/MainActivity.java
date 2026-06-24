package ru.mogcommunity.rbr_project;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.color.DynamicColors;
import ru.mogcommunity.rbr_project.data.PreferenceManager;
import ru.mogcommunity.rbr_project.databinding.ActivityMainBinding;
import ru.mogcommunity.rbr_project.ui.GalleryFragment;
import ru.mogcommunity.rbr_project.ui.ProjectsFragment;
import ru.mogcommunity.rbr_project.ui.SettingsFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

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
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();

        if (savedInstanceState == null) {
            loadFragment(new ProjectsFragment());
        }
    }

    private void setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.action_projects) {
                fragment = new ProjectsFragment();
            } else if (itemId == R.id.action_gallery) {
                fragment = new GalleryFragment();
            } else if (itemId == R.id.action_settings) {
                fragment = new SettingsFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }

    public void setBottomNavigationVisibility(int visibility) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setVisibility(visibility);
        }
    }
}

