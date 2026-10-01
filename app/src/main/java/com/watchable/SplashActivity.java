package com.watchable;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private boolean isFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        VideoView splashVideo = findViewById(R.id.splash_video);
        
        String[] intros = {
            "https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Intro_1.mp4",
            "https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Intro_2.mp4",
            "https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Intro_3.mp4",
            "https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/Intro_4.mp4"
        };
        String videoPath = intros[new java.util.Random().nextInt(intros.length)];
        
        splashVideo.setVideoURI(Uri.parse(videoPath));
        splashVideo.setOnPreparedListener(mp -> {
            mp.setLooping(false);
            splashVideo.start();
        });
        
        splashVideo.setOnCompletionListener(mp -> goToNext());
        splashVideo.setOnErrorListener((mp, what, extra) -> {
            goToNext();
            return true;
        });
        
        new Handler(Looper.getMainLooper()).postDelayed(this::goToNext, 8000);
    }
    
    private void goToNext() {
        if (!isFinished) {
            isFinished = true;
            startActivity(new Intent(SplashActivity.this, ContainerActivity.class));
            finish();
        }
    }
}
