package com.example.WeatherApp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    private final ImageView imageView;

    public DownloadImageTask(ImageView imageView) {
        this.imageView = imageView;
    }

    @Override
    protected Bitmap doInBackground(String... urls) {
        try {
            InputStream input = new URL(urls[0]).openStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null; // optional: return placeholder image
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null)
            imageView.setImageBitmap(result);
    }
}
