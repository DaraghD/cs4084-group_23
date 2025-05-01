package com.example.WeatherApp;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

public class LogoutPreference extends Preference {
    public LogoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.logout_button); // Use full layout, not widgetLayout
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        Button button = (Button) holder.findViewById(R.id.logout_button_custom);
        if (button != null) {
            button.setOnClickListener(v -> {
                if (getOnPreferenceClickListener() != null) {
                    getOnPreferenceClickListener().onPreferenceClick(this);
                }
            });
        }
    }
}
