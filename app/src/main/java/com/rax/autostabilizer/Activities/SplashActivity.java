package com.rax.autostabilizer.Activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.rax.autostabilizer.R;
import com.rax.autostabilizer.databinding.ActivitySplashBinding;

public class SplashActivity extends AppCompatActivity {
    ActivitySplashBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash);

        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.splash_video_720p);
        mBinding.vvBrandVideo.setVideoURI(uri);
        mBinding.vvBrandVideo.setZOrderOnTop(true);
        mBinding.vvBrandVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                //startActivity(new Intent(SplashActivity.this, StabilizerListActivity.class));
                Intent intent = new Intent(SplashActivity.this, StabilizerListActivity.class);
                startActivity(intent);
                finish();
            }
        });
        mBinding.vvBrandVideo.start();
    }
}