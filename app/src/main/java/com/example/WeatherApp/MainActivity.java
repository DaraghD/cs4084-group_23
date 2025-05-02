package com.example.WeatherApp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.WeatherApp.ui.home.WeatherImageFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    public static final String API_KEY = "0ec2f3bfd7e39f7e44f11e00bbd31a81";
    private static final int LOCATION_REQ = 1001;
    public static final String EXTRA_UID = "uid";

    private String currentUid;
    private String units = "metric";

    private TextView greetingView;
    private TextView currentCity;
    private TextView currentTime;
    private TextView currentTemp;
    private TextView currentDetails;
    private LinearLayout favoritesContainer;
    private List<String> favorites = new ArrayList<String>();

    private FusedLocationProviderClient locClient;
    private final OkHttpClient http = new OkHttpClient();
    private boolean stateSaved = false;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        favorites = new ArrayList<>(); // reset favourites on create
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button settings_button = findViewById(R.id.settings_button);
        SearchView search_button = findViewById(R.id.search_button);
        Button editFavBtn = findViewById(R.id.edit_favorites_button);
        Button mapBtn = findViewById(R.id.map_button);
        search_button.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPlace(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // This is called as the user types
                return false;
            }
        });
        settings_button.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class); // Replace 'SecondActivity.class' with the class of the Activity you want to open
            startActivity(intent);
        });

        currentUid = getIntent().getStringExtra(EXTRA_UID);
        if (currentUid == null) {
            Toast.makeText(this, "Error: no user logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ConstraintLayout root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return insets;
        });

        greetingView = findViewById(R.id.greetingView);
        currentCity = findViewById(R.id.currentCity);
        currentTime = findViewById(R.id.currentTime);
        currentTemp = findViewById(R.id.currentTemp);
        favoritesContainer = findViewById(R.id.favoritesContainer);

        locClient = LocationServices.getFusedLocationProviderClient(this);

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        loadUserAndPrefs(currentUid);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadUserAndPrefs(currentUid);
            favorites = new ArrayList<>();
            swipeRefreshLayout.setRefreshing(false);
        });


        editFavBtn = findViewById(R.id.edit_favorites_button);
        editFavBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, EditFavoritesActivity.class);
            intent.putExtra(EXTRA_UID, currentUid);
            startActivity(intent);
        });


        mapBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        stateSaved = false; // Reset the flag
        favorites = new ArrayList<>(); // reset favourites on resume
        loadUserAndPrefs(currentUid); // Load data and start location updates here
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        stateSaved = true;
    }

    private void loadUserAndPrefs(String uid) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("users").child(uid)
                .get().addOnSuccessListener(snap -> {
                    String name = snap.child("displayName").getValue(String.class);
                    String prefUnits = snap.child("preferences")
                            .child("units").getValue(String.class);
                    if (name != null) greetingView.setText("Hi, " + name);
                    if ("imperial".equals(prefUnits)) units = "imperial";
                    fetchWeather(uid, null, null);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user prefs", Toast.LENGTH_SHORT).show()
                );
    }

    // if lon and lat are null, the users current location is used
    public void fetchWeather(String uid, Double lon, Double lat) {
        // 1) Read updated preferences here
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String temperatureUnit = preferences.getString("temperature_unit", "celsius");

        units = temperatureUnit.equals("celsius") ? "metric" : "imperial";

        // 2) Permissions check
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_REQ
            );
            return;
        }

        // 3) Get location and proceed
        locClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        Double longitude;
                        Double latitude;
                        if (lon != null && lat != null) {
                            longitude = lon;
                            latitude = lat;
                        } else {
                            longitude = loc.getLongitude();
                            latitude = loc.getLatitude();
                        }
                        fetchByCoords(
                                latitude,
                                longitude,
                                "Your Location",
                                () -> fetchFavorites(uid)
                        );
                    } else {
                        Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show();
                        fetchFavorites(uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Location error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    fetchFavorites(uid);
                });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQ
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            fetchWeather(currentUid, null, null);
        } else {
            // Bug here, this is showing up when it shouldnt sometimes
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    public void searchPlace(String place) {
        String url_req = "https://nominatim.openstreetmap.org/search?q=" +
                Uri.encode(place) +
                "&format=json&limit=1";
        Request request = new Request.Builder().url(url_req).header("User-Agent", "WeatherApp").build();
        http.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "Error: " + response.code(), Toast.LENGTH_SHORT).show()
                    );
                }
                String responseData = response.body().string();
                try {
                    JSONArray jsonArray = new JSONArray(responseData);
                    if (jsonArray.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this,
                                "No results found",
                                Toast.LENGTH_SHORT).show());
                    }
                    JSONObject place = jsonArray.getJSONObject(0);
                    Double lat = Double.parseDouble(place.getString("lat"));
                    Double lon = Double.parseDouble(place.getString("lon"));
                    fetchWeather(currentUid, lon, lat);

                } catch (Exception e) {
                    Log.e("NOMINATIM", "Error parsing JSON", e);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error:" + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * Fetch and display the “Current Location” card
     */
    private void fetchByCoords(double lat,
                               double lon,
                               String label,
                               Runnable nextStep) {

        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?lat=" + lat
                + "&lon=" + lon
                + "&units=" + units
                + "&appid=" + API_KEY;

        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                label + " error", Toast.LENGTH_SHORT).show()
                );
                nextStep.run();
            }

            @Override
            public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Error " + res.code(), Toast.LENGTH_SHORT).show()
                    );
                    nextStep.run();
                    return;
                }
                try {
                    JSONObject j = new JSONObject(res.body().string());
                    JSONObject main = j.getJSONObject("main");
                    JSONObject sys  = j.getJSONObject("sys");
                    JSONObject wind = j.getJSONObject("wind");

                    String city = j.getString("name");
                    long dt = j.getLong("dt") * 1000L;
                    double temp = main.getDouble("temp");
                    double feels = main.getDouble("feels_like");
                    double tmin = main.getDouble("temp_min");
                    double tmax = main.getDouble("temp_max");
                    int hum = main.getInt("humidity");
                    int press = main.getInt("pressure");
                    int vis = j.optInt("visibility", -1);
                    double windSpd = wind.getDouble("speed");
                    int windDeg = wind.optInt("deg", 0);
                    long sunrise = sys.getLong("sunrise") * 1000L;
                    long sunset = sys.getLong("sunset") * 1000L;
                    String desc = j.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");

                    runOnUiThread(() -> {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String temperatureUnit = preferences.getString("temperature_unit", "celsius");
                        String speedUnit = preferences.getString("speed_unit_preference", "kmh"); // kmh or mph
                        String timeFormat = preferences.getString("time_format_preference", "24"); // 12 or 24
                        String pressureUnit = preferences.getString("pressure_unit_preference", "hpa"); // hpa, mmhg, inhg
                        String tempSymbol = temperatureUnit.equals("celsius") ? "C" : "F";

                        // Convert wind speed (API gives m/s)
                        double windSpeedConverted = windSpd;
                        String windSpeedUnitLabel = "m/s";
                        if (speedUnit.equals("kmh")) {
                            windSpeedConverted = windSpd * 3.6;
                            windSpeedUnitLabel = "km/h";
                        } else if (speedUnit.equals("mph")) {
                            windSpeedConverted = windSpd * 2.237;
                            windSpeedUnitLabel = "mph";
                        }

                        // Convert pressure (API gives hPa)
                        double pressureConverted = press;
                        String pressureLabel = "hPa";
                        if (pressureUnit.equals("mmhg")) {
                            pressureConverted = press * 0.75006;
                            pressureLabel = "mmHg";
                        } else if (pressureUnit.equals("inhg")) {
                            pressureConverted = press * 0.02953;
                            pressureLabel = "inHg";
                        } else if (pressureUnit.equals("psi")) {
                            pressureConverted = press * 0.0145038;
                            pressureLabel = "psi";
                        }

                        currentCity.setText(city);
                        currentTime.setText(
                                DateFormat.format(
                                        timeFormat.equals("24") ? "MMM d, HH:mm" : "MMM d, h:mm a",
                                        dt
                                )
                        );

                        currentTemp.setText(String.format("%.1f°%s", temp, tempSymbol));

                        TextView descView = findViewById(R.id.description);
                        TextView feelsLikeView = findViewById(R.id.feelsLike);
                        TextView humidityView = findViewById(R.id.humidity);
                        TextView pressureView = findViewById(R.id.pressure);
                        TextView windView = findViewById(R.id.wind);
                        TextView sunriseView = findViewById(R.id.sunrise);
                        TextView sunsetView = findViewById(R.id.sunset);
                        TextView lowView = findViewById(R.id.low);
                        TextView highView = findViewById(R.id.high);

                        descView.setText(desc);
                        feelsLikeView.setText(String.format("%.1f°%s", feels, tempSymbol));
                        humidityView.setText(String.format("%d%%", hum));
                        pressureView.setText(String.format("%.1f %s", pressureConverted, pressureLabel));
                        windView.setText(String.format("%.1f %s", windSpeedConverted, windSpeedUnitLabel));
                        sunriseView.setText(DateFormat.format(timeFormat.equals("24") ? "HH:mm" : "h:mm a", sunrise));
                        sunsetView.setText(DateFormat.format(timeFormat.equals("24") ? "HH:mm" : "h:mm a", sunset));
                        lowView.setText(String.format("%.1f°%s", tmin, tempSymbol));
                        highView.setText(String.format("%.1f°%s", tmax, tempSymbol));

                        WeatherImageFragment weatherImageFragment = new WeatherImageFragment();
                        Bundle bundle = new Bundle();
                        bundle.putString("weatherDesc", desc);
                        weatherImageFragment.setArguments(bundle);

                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.weatherImageContainer, weatherImageFragment);
                        fragmentTransaction.commit();
                    });
// 5-day / 3-hour forecast for hourly display
                    String forecastUrl = "https://api.openweathermap.org/data/2.5/forecast"
                            + "?lat=" + lat
                            + "&lon=" + lon
                            + "&units=" + units
                            + "&appid=" + API_KEY;

                    Request forecastReq = new Request.Builder().url(forecastUrl).build();
                    http.newCall(forecastReq).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NonNull Call call, @NonNull IOException e) {
                            runOnUiThread(() ->
                                    Toast.makeText(MainActivity.this, "Failed to load hourly forecast", Toast.LENGTH_SHORT).show()
                            );
                        }

                        @Override
                        public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                            if (!response.isSuccessful()) return;
                            try {
                                JSONObject json = new JSONObject(response.body().string());
                                var list = json.getJSONArray("list");
                                List<HourForecastAdapter.HourData> hours = new ArrayList<>();

                                for (int i = 0; i < Math.min(8, list.length()); i++) {
                                    JSONObject item = list.getJSONObject(i);
                                    long time = item.getLong("dt");
                                    double temp = item.getJSONObject("main").getDouble("temp");
                                    String icon = item.getJSONArray("weather").getJSONObject(0).getString("icon");

                                    hours.add(new HourForecastAdapter.HourData(time, temp, icon));
                                }

                                runOnUiThread(() -> {

                                    RecyclerView recycler = findViewById(R.id.hourlyRecycler);
                                    recycler.setHasFixedSize(true);
                                    recycler.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                                    recycler.setAdapter(new HourForecastAdapter(hours));
                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (JSONException ignored) {
                }
                nextStep.run();
            }
        });
    }

    private void fetchFavorites(String uid) {
        // this runs on whichever thread called fetchFavorites, might be background!
        runOnUiThread(() -> {
            // 0) clear any old cards (now safely on main thread)
            favoritesContainer.removeAllViews();

            // 1) now fetch from Firebase (its listeners themselves execute on main thread)
            DatabaseReference root = FirebaseDatabase.getInstance().getReference();
            root.child("userFavorites").child(uid)
                    .get()
                    .addOnSuccessListener(favSnap -> {
                        Set<String> unique_city_ids = new HashSet<>();
                        for(var c: favSnap.getChildren()){
                            unique_city_ids.add(c.getKey());
                        }
                        for (String cityId : unique_city_ids) {
                            if(favorites != null)
                                if(!this.favorites.contains(cityId)) { // if not in favourites, add to favourites
                                    fetchFavoriteById(cityId);
                                    this.favorites.add(cityId);
                                }
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load favorites",
                                    Toast.LENGTH_SHORT).show()
                    );
        });
    }

    private void fetchFavoriteById(String cityId) {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?id=" + cityId
                + "&units=" + units
                + "&appid=" + API_KEY;

        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // skip failures
            }

            @Override
            public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) return;
                try {
                    JSONObject j = new JSONObject(res.body().string());
                    String city = j.getString("name");
                    long dt = j.getLong("dt") * 1000L;
                    JSONObject main = j.getJSONObject("main");
                    JSONObject wind = j.getJSONObject("wind");

                    double temp = main.getDouble("temp");
                    int hum = main.getInt("humidity");
                    double windSpd = wind.getDouble("speed");
                    String desc = j
                            .getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");

                    runOnUiThread(() -> {

                        LinearLayout parent = findViewById(R.id.favoritesContainer);
                        View card = getLayoutInflater()
                                .inflate(R.layout.item_weather, parent, false);

                        TextView tvCity = card.findViewById(R.id.cityName);
                        TextView tvTime = card.findViewById(R.id.timeStamp);
                        TextView tvDesc = card.findViewById(R.id.description);
                        TextView tvTemp = card.findViewById(R.id.temperature);
                        TextView tvDet = card.findViewById(R.id.details);

                        tvCity.setText(city);
                        tvTime.setText(DateFormat.format("h:mm a", dt));
                        tvDesc.setText(desc);
                        tvTemp.setText(String.format(
                                "%.1f°%s",
                                temp,
                                units.equals("metric") ? "C" : "F"
                        ));
                        tvDet.setText(String.format(
                                "Hum %d%% | Wind %.1f %s",
                                hum,
                                windSpd,
                                units.equals("metric") ? "m/s" : "mph"
                        ));

                        parent.addView(card);
                    });
                } catch (JSONException ignored) {
                }
            }

            public String getCurrentUid() {
                return currentUid;
            }

        });
    }

}
