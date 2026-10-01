package com.watchable;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.watchable.R;

public class OnboardingActivity extends AppCompatActivity {
    private EditText nameInput;
    private Button nextBtn;
    private TextView charCount;
    private LinearLayout inputStep, successStep;
    private TextView successName;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppPreferences.get(this).hasUserName()) {
            startActivity(new Intent(this, ContainerActivity.class));
            finish(); return;
        }
        setContentView(R.layout.activity_onboarding);
        nameInput   = findViewById(R.id.onboarding_name_input);
        nextBtn     = findViewById(R.id.onboarding_next_btn);
        charCount   = findViewById(R.id.onboarding_char_count);
        inputStep   = findViewById(R.id.onboarding_input_step);
        successStep = findViewById(R.id.onboarding_success_step);
        successName = findViewById(R.id.onboarding_success_name);
        nextBtn.setEnabled(false);

        android.widget.ImageView welcomeIcon = findViewById(R.id.onboarding_welcome_icon);
        if (welcomeIcon != null) {
            com.bumptech.glide.Glide.with(this)
                .asGif()
                .load("https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/welcome_icon.gif")
                .into(welcomeIcon);
        }

        nameInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String txt = s.toString().trim();
                nextBtn.setEnabled(!txt.isEmpty());
                nextBtn.setAlpha(txt.isEmpty() ? 0.4f : 1f);
                charCount.setText(s.length() + "/10 chars");
            }
        });

        nextBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                if (name.isEmpty()) return;
                if (name.length() > 10) name = name.substring(0, 10);
                showSuccessStep(name);
            }
        });

        Button startBtn = findViewById(R.id.onboarding_start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AppPreferences p = AppPreferences.get(OnboardingActivity.this);
                p.setOnboarded(true);
                startActivity(new Intent(OnboardingActivity.this, ContainerActivity.class));
                finish();
            }
        });
    }

    private void showSuccessStep(String name) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
        AppPreferences.get(this).setUserName(name);
        successName.setText(name + "!");
        inputStep.setVisibility(View.GONE);
        successStep.setVisibility(View.VISIBLE);
        successStep.setAlpha(0f);
        successStep.animate().alpha(1f).setDuration(300).start();
    }
}
