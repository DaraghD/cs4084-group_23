package com.example.WeatherApp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapClickListener {
    private static final int LOCATION_REQ = 1001;
    private GoogleMap map;
    private FusedLocationProviderClient locClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        locClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment f = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        f.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        // register click listener for taps on the map
        map.setOnMapClickListener(this);

        // check permissions
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQ
            );
            return;
        }

        // enable "My Location"
        map.setMyLocationEnabled(true);

        // center on last known location and fetch weather
        locClient.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 10f));
                fetchAndShow(here.latitude, here.longitude);
            } else {
                Toast.makeText(this, "Location unavailable", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Called when the user taps on the map.
     */
    @Override
    public void onMapClick(@NonNull LatLng point) {
        // animate camera to the tapped location
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, map.getCameraPosition().zoom));
        // fetch weather & show marker at tapped location
        fetchAndShow(point.latitude, point.longitude);
    }

    private void fetchAndShow(double lat, double lon) {
        String units = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("temperature_unit", "celsius")
                .equals("celsius") ? "metric" : "imperial";

        WeatherService.fetchByCoords(lat, lon, units, this, new WeatherService.Callback() {
            @Override
            public void onSuccess(WeatherService.WeatherData wd) {
                runOnUiThread(() -> {
                    LatLng pos = new LatLng(lat, lon);
                    map.clear();
                    map.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(wd.city)
                            .snippet(String.format(
                                    "%s, %.1f°%s, %d%% hum",
                                    wd.getDescription(),
                                    wd.getTemp(),
                                    units.equals("metric") ? "C" : "F",
                                    wd.getHumidity()))
                    ).showInfoWindow();
                });
            }

            @Override
            public void onFailure(String err) {
                runOnUiThread(() ->
                        Toast.makeText(MapActivity.this,
                                "Weather error: " + err,
                                Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQ && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            onMapReady(map);
        }
    }
}