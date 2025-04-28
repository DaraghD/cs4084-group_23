package com.example.WeatherApp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
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

import com.example.WeatherApp.ui.home.WeatherImageFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.material.card.MaterialCardView;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final String API_KEY      = "0ec2f3bfd7e39f7e44f11e00bbd31a81";
    private static final int    LOCATION_REQ = 1001;
    private static final String EXTRA_UID    = "uid";

    private String  currentUid;
    private String  units = "metric";

    private TextView greetingView;
    private TextView currentCity;
    private TextView currentTime;
    private TextView currentTemp;
    private TextView currentDetails;
    private LinearLayout favoritesContainer;

    private FusedLocationProviderClient locClient;
    private final OkHttpClient http = new OkHttpClient();
    private boolean stateSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        Button settings_button = findViewById(R.id.settings_button);
        settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class); // Replace 'SecondActivity.class' with the class of the Activity you want to open
                startActivity(intent);
            }
        });

        // 1) Read UID
        currentUid = getIntent().getStringExtra(EXTRA_UID);
        if (currentUid == null) {
            Toast.makeText(this, "Error: no user logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2) Insets handling
        ConstraintLayout root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return insets;
        });

        // 3) Wire up views
        greetingView        = findViewById(R.id.greetingView);
        currentCity         = findViewById(R.id.currentCity);
        currentTime         = findViewById(R.id.currentTime);
        currentTemp         = findViewById(R.id.currentTemp);
        favoritesContainer  = findViewById(R.id.favoritesContainer);

        locClient = LocationServices.getFusedLocationProviderClient(this);

        // 4) Load user prefs & start
        loadUserAndPrefs(currentUid);
    }
    @Override
    protected void onResume() {
        super.onResume();
        stateSaved = false; // Reset the flag
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
                    String name      = snap.child("displayName").getValue(String.class);
                    String prefUnits = snap.child("preferences")
                            .child("units").getValue(String.class);
                    if (name != null) greetingView.setText("Hi, " + name);
                    if ("imperial".equals(prefUnits)) units = "imperial";
                    fetchWeather(uid);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load user prefs", Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchWeather(String uid) {
        // Permissions
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

        // Get location
        locClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        fetchByCoords(
                                loc.getLatitude(),
                                loc.getLongitude(),
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
            fetchWeather(currentUid);
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    /** Fetch and display the “Current Location” card */
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
            @Override public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                label + " error", Toast.LENGTH_SHORT).show()
                );
                nextStep.run();
            }
            @Override public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Error " + res.code(), Toast.LENGTH_SHORT).show()
                    );
                    nextStep.run();
                    return;
                }
                try {
                    JSONObject j    = new JSONObject(res.body().string());
                    JSONObject main = j.getJSONObject("main");
                    JSONObject sys  = j.getJSONObject("sys");
                    JSONObject wind = j.getJSONObject("wind");

                    String city     = j.getString("name");
                    long   dt       = j.getLong("dt") * 1000L;
                    double temp     = main.getDouble("temp");
                    double feels    = main.getDouble("feels_like");
                    double tmin     = main.getDouble("temp_min");
                    double tmax     = main.getDouble("temp_max");
                    int    hum      = main.getInt("humidity");
                    int    press    = main.getInt("pressure");
                    int    vis      = j.optInt("visibility", -1);
                    double windSpd  = wind.getDouble("speed");
                    int    windDeg  = wind.optInt("deg", 0);
                    long   sunrise  = sys.getLong("sunrise") * 1000L;
                    long   sunset   = sys.getLong("sunset")  * 1000L;
                    String desc     = j.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");

                    runOnUiThread(() -> {
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String temperatureUnit = preferences.getString("temperature_unit", "celsius");
                        String speedUnit = preferences.getString("speed_unit_preference", "kmh"); // Default to km/h
                        String timeFormat = preferences.getString("time_format_preference", "24"); // Default to 24-hour
                        String pressureUnit = preferences.getString("pressure_unit_preference", "hpa"); // Default to hPa

                        currentCity.setText(city);
                        currentTime.setText(
                                DateFormat.format("MMM d, h:mm a", dt)
                        );
                        currentTemp.setText(
                                String.format("%.1f°%s",
                                        temp,
                                        temperatureUnit.equals("celsius")?"C":"F")
                        );
                        //TODO: make sure all measurements here are correct e.g km/h vs m/s, the measurement from the api may be farenheit but switching the symbol isnt enough need to do conversion.
                        //TODO: does api support other pressure units?
                        String details = String.format(
                                "Feels like %.1f° | %s\n" +
                                        "Low: %.1f°, High: %.1f°\n" +
                                        "Hum: %d%% | Press: %dhPa | Vis: %s\n" +
                                        "Wind: %.1f %s, %d°\n" +
                                        "🌅 %s  🌇 %s",
                                feels,
                                desc,
                                tmin, tmax,
                                hum,
                                press,
                                (vis>=0?vis+"m":"n/a"),
                                windSpd,
                                speedUnit.equals("kmh")?"km/h":"mph",
                                windDeg,
                                DateFormat.format(timeFormat.equals("24")?"HH:mm":"h:mm a", sunrise),
                                DateFormat.format(timeFormat.equals("24")?"HH:mm":"h:mm a", sunset)
                        );

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
                        feelsLikeView.setText(String.valueOf(feels + "°C"));
                        humidityView.setText(String.valueOf(hum + "%"));
                        pressureView.setText(String.valueOf(press + " hPa"));
                        windView.setText(String.valueOf(windSpd + " km/h"));
                        sunriseView.setText(DateFormat.format("h:mm a", sunrise));
                        sunsetView.setText(DateFormat.format("h:mm a", sunset));
                        lowView.setText(String.valueOf(tmin + "°C"));
                        highView.setText(String.valueOf(tmax + "°C"));

                        WeatherImageFragment weatherImageFragment = new WeatherImageFragment();

                        Bundle bundle = new Bundle();
                        bundle.putString("weatherDesc", desc);
                        weatherImageFragment.setArguments(bundle);

                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                        fragmentTransaction.replace(R.id.weatherImageContainer, weatherImageFragment);
                        fragmentTransaction.commit();
                    });
                } catch (JSONException ignored) {}
                nextStep.run();
            }
        });
    }

    /** Fetch favorites and render a mini-card for each */
    private void fetchFavorites(String uid) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("userFavorites").child(uid)
                .get().addOnSuccessListener(favSnap -> {
                    for (var c : favSnap.getChildren()) {
                        String cityId = c.getKey();
                        fetchFavoriteById(cityId);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load favorites",
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchFavoriteById(String cityId) {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?id="    + cityId
                + "&units=" + units
                + "&appid=" + API_KEY;

        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                // skip failures
            }
            @Override public void onResponse(Call call, Response res) throws IOException {
                if (!res.isSuccessful()) return;
                try {
                    JSONObject j    = new JSONObject(res.body().string());
                    String    city  = j.getString("name");
                    long      dt    = j.getLong("dt") * 1000L;
                    JSONObject main = j.getJSONObject("main");
                    JSONObject wind = j.getJSONObject("wind");

                    double temp    = main.getDouble("temp");
                    int    hum     = main.getInt("humidity");
                    double windSpd = wind.getDouble("speed");
                    String desc    = j
                            .getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");

                    runOnUiThread(() -> {
                        // 1) Inflate the card layout
                        LinearLayout parent = findViewById(R.id.favoritesContainer);
                        View card = getLayoutInflater()
                                .inflate(R.layout.item_weather, parent, false);

                        // 2) Bind its views
                        TextView tvCity = card.findViewById(R.id.cityName);
                        TextView tvTime = card.findViewById(R.id.timeStamp);
                        TextView tvDesc = card.findViewById(R.id.description);
                        TextView tvTemp = card.findViewById(R.id.temperature);
                        TextView tvDet  = card.findViewById(R.id.details);

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

                        // 3) Add it to the container
                        parent.addView(card);
                    });
                } catch (JSONException ignored) { }
            }
        });
    }

}
