package com.example.WeatherApp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String API_KEY      = "0ec2f3bfd7e39f7e44f11e00bbd31a81";
    private static final int    LOCATION_REQ = 1001;
    private static final String EXTRA_UID    = "uid";

    private String                            currentUid;
    private TextView                          greetingView;
    private TextView                          mainTextView;
    private FusedLocationProviderClient       locClient;
    private OkHttpClient                      http      = new OkHttpClient();
    private String                            units     = "metric";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // 1) Read the UID passed from Login/Register
        currentUid = getIntent().getStringExtra(EXTRA_UID);
        if (currentUid == null) {
            // No UID → show error and bail
            setContentView(R.layout.activity_main);
            TextView err = findViewById(R.id.mainTextView);
            err.setText("Error: no user logged in");
            return;
        }

        ConstraintLayout root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets s = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(s.left, s.top, s.right, s.bottom);
            return insets;
        });

        greetingView = findViewById(R.id.greetingView);
        mainTextView = findViewById(R.id.mainTextView);
        locClient    = LocationServices.getFusedLocationProviderClient(this);

        loadUserAndPrefs(currentUid);
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
                });
    }

    private void fetchWeather(String uid) {
        StringBuilder out = new StringBuilder();

        boolean fineOK   = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseOK = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineOK || !coarseOK) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
                    LOCATION_REQ);
            return;
        }

        locClient.getCurrentLocation(
                        LocationRequest.PRIORITY_HIGH_ACCURACY,
                        null
                )
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        fetchByCoords(
                                loc.getLatitude(),
                                loc.getLongitude(),
                                "Your Location",
                                out,
                                () -> fetchFavorites(uid, out)
                        );
                    } else {
                        out.append("Location unavailable\n\n");
                        fetchFavorites(uid, out);
                    }
                })
                .addOnFailureListener(e -> {
                    out.append("Location error: ")
                            .append(e.getMessage())
                            .append("\n\n");
                    fetchFavorites(uid, out);
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
            // use same currentUid
            fetchWeather(currentUid);
        } else {
            mainTextView.setText("Location permission denied");
        }
    }

    private void fetchFavorites(String uid, StringBuilder out) {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        root.child("userFavorites").child(uid)
                .get().addOnSuccessListener(favSnap -> {
                    List<String> ids = new ArrayList<>();
                    favSnap.getChildren().forEach(c -> ids.add(c.getKey()));
                    if (ids.isEmpty()) {
                        updateUI(out.append("\nNo favorites"));
                    } else {
                        out.append("\nFavorites:\n");
                        fetchByIds(ids, out);
                    }
                });
    }

    private void fetchByIds(List<String> ids, StringBuilder out) {
        final int total = ids.size();
        final int[] done = {0};

        for (String cityId : ids) {
            String url = "https://api.openweathermap.org/data/2.5/weather"
                    + "?id=" + cityId
                    + "&units=" + units
                    + "&appid=" + API_KEY;

            Request req = new Request.Builder().url(url).build();
            http.newCall(req).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    appendLine("[" + cityId + "] error", out, done, total);
                }
                @Override public void onResponse(Call call, Response res) throws IOException {
                    if (!res.isSuccessful()) {
                        appendLine("[" + cityId + "] " + res.code(), out, done, total);
                        return;
                    }
                    try {
                        JSONObject j     = new JSONObject(res.body().string());
                        String city      = j.getString("name");
                        JSONObject main  = j.getJSONObject("main");
                        double temp      = main.getDouble("temp");
                        int hum          = main.getInt("humidity");
                        double wind      = j.getJSONObject("wind").getDouble("speed");
                        String desc      = j.getJSONArray("weather")
                                .getJSONObject(0)
                                .getString("description");
                        String tfmt      = String.format(
                                "%.1f°%s",
                                temp,
                                units.equals("metric") ? "C" : "F"
                        );
                        String line      = String.format(
                                "%s: %s, %s, Hum %d%%, Wind %.1f %s\n",
                                city, desc, tfmt, hum, wind,
                                units.equals("metric") ? "m/s" : "mph"
                        );
                        appendLine(line, out, done, total);
                    } catch (JSONException ex) {
                        appendLine("[" + cityId + "] parse error", out, done, total);
                    }
                }
            });
        }
    }

    private void fetchByCoords(double lat, double lon,
                               String label,
                               StringBuilder out,
                               Runnable nextStep) {
        StringBuilder lb = new StringBuilder(label);
        try {
            Geocoder gc     = new Geocoder(this, Locale.getDefault());
            List<Address> a = gc.getFromLocation(lat, lon, 1);
            if (!a.isEmpty()) {
                Address addr   = a.get(0);
                String city    = addr.getLocality();
                String state   = addr.getAdminArea();
                String country = addr.getCountryName();
                lb.append(" (")
                        .append(city).append(", ")
                        .append(state).append(", ")
                        .append(country).append(")");
            }
        } catch (IOException ignored) { }

        final String fullLabel = lb.toString();
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?lat=" + lat
                + "&lon=" + lon
                + "&units=" + units
                + "&appid=" + API_KEY;

        Request req = new Request.Builder().url(url).build();
        http.newCall(req).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                appendLine(fullLabel + ": location error\n\n", out, new int[]{0}, 1);
                nextStep.run();
            }
            @Override public void onResponse(Call call, Response res) throws IOException {
                StringBuilder local = new StringBuilder();
                try {
                    JSONObject j    = new JSONObject(res.body().string());
                    JSONObject main = j.getJSONObject("main");
                    double temp     = main.getDouble("temp");
                    int hum         = main.getInt("humidity");
                    String desc     = j.getJSONArray("weather")
                            .getJSONObject(0)
                            .getString("description");
                    String tfmt     = String.format(
                            "%.1f°%s",
                            temp,
                            units.equals("metric") ? "C" : "F"
                    );
                    local.append(fullLabel)
                            .append(": ")
                            .append(desc).append(", ")
                            .append(tfmt).append(", Hum ")
                            .append(hum).append("%\n\n");
                } catch (JSONException ex) {
                    local.append(fullLabel).append(": parse error\n\n");
                }
                appendLine(local.toString(), out, new int[]{0}, 1);
                nextStep.run();
            }
        });
    }

    private synchronized void appendLine(String line,
                                         StringBuilder buf,
                                         int[] done,
                                         int total) {
        buf.append(line);
        done[0]++;
        if (done[0] == total) {
            updateUI(buf);
        }
    }

    private void updateUI(StringBuilder buf) {
        runOnUiThread(() -> mainTextView.setText(buf.toString()));
    }
}
