package com.example.WeatherApp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.WeatherApp.R;
import com.example.WeatherApp.databinding.FragmentWeatherimageBinding;

public class WeatherImageFragment extends Fragment {

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_weatherimage, container, false);

        ImageView imageView = root.findViewById(R.id.rainIcon);

        String desc = getArguments().getString("weatherDesc", "default");

        if (desc.contains("rain")){
            imageView.setImageResource(R.drawable.rain_icon);
        } else if (desc.contains("sun")) {
            imageView.setImageResource(R.drawable.sun_icon);
        }else if (desc.contains("cloud")){
            imageView.setImageResource(R.drawable.cloudy_icon);
        }else{
            imageView.setImageResource(R.drawable.storm_icon);
        }

        return root;
    }
}
