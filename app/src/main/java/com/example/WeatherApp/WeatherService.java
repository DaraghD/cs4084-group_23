package com.example.WeatherApp;

import android.content.Context;
import android.os.AsyncTask;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;


public class WeatherService {
    // replace with your actual OpenWeatherMap API key
    private static final String API_KEY = "0ec2f3bfd7e39f7e44f11e00bbd31a81";

    public interface Callback {
        void onSuccess(WeatherData data);
        void onFailure(String errorMessage);
    }

    public static void fetchByCoords(double lat, double lon, String units, Context ctx, Callback cb) {
        String url = String.format(
                Locale.US,
                "https://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&units=%s&appid=%s",
                lat, lon, units, API_KEY
        );
        new FetchTask(cb).execute(url);
    }

    private static class FetchTask extends AsyncTask<String, Void, Object> {
        private final Callback callback;
        public FetchTask(Callback cb) { this.callback = cb; }

        @Override
        protected Object doInBackground(String... urls) {
            try {
                URL u = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection)u.openConnection();
                conn.setRequestMethod("GET");
                conn.connect();

                if (conn.getResponseCode() != 200) {
                    return "HTTP error: " + conn.getResponseCode();
                }

                WeatherData data = new Gson()
                        .fromJson(new InputStreamReader(conn.getInputStream()), WeatherData.class);
                return data;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result instanceof WeatherData) {
                callback.onSuccess((WeatherData)result);
            } else {
                callback.onFailure((String)result);
            }
        }
    }

    // === your data model matching the JSON ===
    public static class WeatherData {
        // “name” is the city name field in OpenWeatherMap
        @SerializedName("name")
        public String city;

        // “weather” is an array in the JSON; we only care about the first element
        @SerializedName("weather")
        public Weather[] weather;

        // “main” holds temp & humidity
        @SerializedName("main")
        public Main main;

        public String getDescription() {
            return weather != null && weather.length > 0
                    ? weather[0].description
                    : "n/a";
        }
        public double getTemp()    { return main.temp; }
        public int    getHumidity(){ return main.humidity; }

        public static class Weather {
            @SerializedName("description")
            public String description;
        }

        public static class Main {
            @SerializedName("temp")
            public double temp;

            @SerializedName("humidity")
            public int humidity;
        }
    }
}
