package com.example.WeatherApp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HourForecastAdapter extends RecyclerView.Adapter<HourForecastAdapter.HourViewHolder> {

    public static class HourData {
        public long timestamp;
        public double temperature;
        public String iconCode;

        public HourData(long timestamp, double temperature, String iconCode) {
            this.timestamp = timestamp;
            this.temperature = temperature;
            this.iconCode = iconCode;
        }
    }

    private final List<HourData> hourList;

    public HourForecastAdapter(List<HourData> hourList) {
        this.hourList = hourList;
    }

    @NonNull
    @Override
    public HourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hour, parent, false);
        return new HourViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HourViewHolder holder, int position) {
        HourData data = hourList.get(position);

        // Format time
        String hour = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(data.timestamp * 1000L));
        holder.hourText.setText(hour);

        // Set temperature
        holder.tempText.setText(String.format(Locale.getDefault(), "%.0f°", data.temperature));

        // Load icon from OpenWeatherMap
        String iconUrl = "https://openweathermap.org/img/wn/" + data.iconCode + "@2x.png";
        new DownloadImageTask(holder.weatherIcon).execute(iconUrl);
    }

    @Override
    public int getItemCount() {
        return hourList.size();
    }

    public static class HourViewHolder extends RecyclerView.ViewHolder {
        TextView hourText, tempText;
        ImageView weatherIcon;

        public HourViewHolder(@NonNull View itemView) {
            super(itemView);
            hourText = itemView.findViewById(R.id.hourText);
            tempText = itemView.findViewById(R.id.tempText);
            weatherIcon = itemView.findViewById(R.id.weatherIcon);
        }
    }
}
