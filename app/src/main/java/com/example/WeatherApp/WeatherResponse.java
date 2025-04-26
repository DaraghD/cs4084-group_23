package com.example.WeatherApp;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WeatherResponse {
    public String name;
    public long dt;               // timestamp
    public int visibility;        // metres

    @SerializedName("weather")
    public List<Weather> weather;

    @SerializedName("main")
    public Main main;

    @SerializedName("wind")
    public Wind wind;

    @SerializedName("clouds")
    public Clouds clouds;

    @SerializedName("sys")
    public Sys sys;

    @SerializedName("rain")
    public Precipitation rain;

    @SerializedName("snow")
    public Precipitation snow;

    public static class Weather {
        public String main;        // e.g. “Clouds”
        public String description; // e.g. “broken clouds”
        public String icon;        // icon code
    }

    public static class Main {
        public double temp;
        @SerializedName("feels_like") public double feelsLike;
        @SerializedName("temp_min")   public double tempMin;
        @SerializedName("temp_max")   public double tempMax;
        public int pressure;       // hPa
        public int humidity;       // %
    }

    public static class Wind {
        public double speed;       // m/s or mph
        public int deg;            // wind direction
    }

    public static class Clouds {
        public int all;            // cloudiness %
    }

    public static class Sys {
        public long sunrise;       // UNIX UTC
        public long sunset;        // UNIX UTC
    }

    public static class Precipitation {
        @SerializedName("1h") public double oneH;
        @SerializedName("3h") public double threeH;
    }
}

