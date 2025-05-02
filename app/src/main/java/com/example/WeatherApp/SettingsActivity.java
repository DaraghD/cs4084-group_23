package com.example.WeatherApp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
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
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if (item.getItemId() == android.R.id.home) {
            finish(); // go back to previous screen
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            Preference logout = findPreference("logout_button_custom");
            Log.e("Logout", logout.toString());
            if (logout != null) {
                logout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
                {
                    @Override
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        Log.e("Logout", "Clicked");
                        handleLogoutClick();
                        return true;
                    }
                });
            }
        }

        private void handleLogoutClick(){
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("Are you sure you want to logout?")
                    .setTitle("Logout")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // CONFIRM
                            SharedPreferences pref = requireContext().getSharedPreferences("user", MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            // reset prefs to not log back in
                            editor.putString("uid", null);
                            editor.apply();
                            Intent i = new Intent(requireContext(), WelcomeActivity.class);
                            startActivity(i);
                            requireActivity().finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // CANCEL
                            dialog.cancel(); // Dismiss the dialog on cancel
                        }
                    });
            // Create the AlertDialog object and show it
            AlertDialog dialog = builder.create();
            dialog.show();
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
