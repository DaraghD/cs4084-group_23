package com.example.WeatherApp;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class WeatherAdapter
        extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    public interface Listener {
        void onItemClick(WeatherResponse wr);
    }

    private final List<WeatherResponse> items = new ArrayList<>();
    private final String unitsSuffix;
    private final Listener listener;

    public WeatherAdapter(String units, Listener listener) {
        this.unitsSuffix = units.equals("metric") ? "C" : "F";
        this.listener    = listener;
    }

    public void setData(List<WeatherResponse> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_weather, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        WeatherResponse wr = items.get(i);

        // City name
        h.cityName.setText(wr.name);

        // Timestamp (if dt present; else you can omit)
        if (wr.dt > 0) {
            h.timeStamp.setText(
                    DateFormat.format("h:mm a", wr.dt * 1000L)
            );
        }

        // Description
        String desc = wr.weather.get(0).description;
        h.description.setText(desc);

        // Temperature
        h.temperature.setText(String.format(
                "%.1f°%s",
                wr.main.temp,
                unitsSuffix
        ));

        // Additional details (humidity & wind)
        h.details.setText(String.format(
                "Hum %d%% | Wind %.1f %s",
                wr.main.humidity,
                wr.wind.speed,
                unitsSuffix.equals("C") ? "m/s" : "mph"
        ));

        // Click-through for detail screen
        h.itemView.setOnClickListener(v -> listener.onItemClick(wr));
    }

    @Override public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView cityName, timeStamp, description, temperature, details;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cityName    = itemView.findViewById(R.id.cityName);
            timeStamp   = itemView.findViewById(R.id.timeStamp);
            description = itemView.findViewById(R.id.description);
            temperature = itemView.findViewById(R.id.temperature);
            details     = itemView.findViewById(R.id.details);
        }
    }
}

