package com.example.WeatherApp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;


public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SharedPreferences prefs;
        private final SharedPreferences.OnSharedPreferenceChangeListener listener =
                (sharedPreferences, key) -> {
                    if (key.equals("temperature_unit") || key.equals("speed_unit_preference")
                            || key.equals("time_format_preference") || key.equals("pressure_unit_preference")) {
                        // No need to fetchWeather here.
                        // When SettingsActivity finishes, MainActivity.onResume() will handle it.
                        Toast.makeText(getContext(), "Changes will apply when you return.", Toast.LENGTH_SHORT).show();
                    }
                };


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (prefs != null) {
                prefs.unregisterOnSharedPreferenceChangeListener(listener);
            }
        }
    }
}
