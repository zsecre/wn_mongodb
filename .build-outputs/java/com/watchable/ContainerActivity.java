package com.watchable;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.watchable.R;

public class ContainerActivity extends AppCompatActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AppPreferences.get(this).hasUserName()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        // Sync existing profile to Firebase /users node so analytics dashboard has display name
        AppPreferences.get(this).syncProfileToFirebase();

        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        // Check for remote APK update in the background
        RemoteUpdateManager.get().checkForUpdate(this, false);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            showServerQualityNoticeIfNeeded();
        }

        bottomNav.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                public boolean onNavigationItemSelected(MenuItem item) {
                    Fragment fragment = null;
                    int id = item.getItemId();
                    if      (id == R.id.nav_all)     fragment = new HomeFragment();
                    else if (id == R.id.nav_movies)  fragment = new MoviesFragment();
                    else if (id == R.id.nav_tv)      fragment = new TvFragment();
                    else if (id == R.id.nav_library) fragment = new LibraryFragment();
                    else if (id == R.id.nav_me)      fragment = new MeFragment();
                    if (fragment != null) { loadFragment(fragment); return true; }
                    return false;
                }
            });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit();
    }

    private void showServerQualityNoticeIfNeeded() {
        final AppPreferences prefs = AppPreferences.get(this);
        if (prefs.isDontShowServerNotice()) {
            return;
        }

        findViewById(android.R.id.content).post(new Runnable() {
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) return;

                final Dialog dialog = new Dialog(ContainerActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_server_quality_notice);

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                final CheckBox cbDontShowAgain = dialog.findViewById(R.id.cb_dont_show_again);
                Button btnGotIt = dialog.findViewById(R.id.btn_got_it);
                ImageButton btnClose = dialog.findViewById(R.id.btn_close_notice);

                View.OnClickListener dismissListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (cbDontShowAgain != null && cbDontShowAgain.isChecked()) {
                            prefs.setDontShowServerNotice(true);
                        }
                        dialog.dismiss();
                    }
                };

                if (btnGotIt != null) {
                    btnGotIt.setOnClickListener(dismissListener);
                }
                if (btnClose != null) {
                    btnClose.setOnClickListener(dismissListener);
                }

                dialog.show();
            }
        });
    }
}
