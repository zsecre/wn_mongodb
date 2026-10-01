package com.watchable;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import com.watchable.R;
import com.bumptech.glide.Glide;
import android.widget.ScrollView;
import android.widget.GridLayout;
import android.widget.ImageView;

public class MeFragment extends Fragment {
    private View root;

    private static final String[] ANIME_AVATARS = {
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime1",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime2",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime3",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime4",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime5",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime6",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime7",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime8",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime9",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime10",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime11",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime12",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime13",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime14",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime15",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime16",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime17",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime18",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime19",
        "https://api.dicebear.com/9.x/adventurer/png?seed=Anime20"
    };

    private void loadMyAvatar(AppPreferences prefs, ImageView img, TextView text) {
        String url = prefs.getUserAvatar();
        if (url != null && !url.isEmpty()) {
            img.setVisibility(View.VISIBLE);
            if (text != null) text.setVisibility(View.GONE);
            Glide.with(this).load(url).circleCrop().into(img);
        } else {
            img.setVisibility(View.GONE);
            if (text != null) {
                text.setVisibility(View.VISIBLE);
                String n = prefs.getUserName();
                if (n != null && !n.isEmpty()) {
                    text.setText(n.substring(0, 1).toUpperCase());
                } else {
                    text.setText("U");
                }
            }
        }
    }

    private void showAvatarSelectionDialog(final AppPreferences prefs, final ImageView avatarImg, final TextView avatarText) {
        ScrollView scroll = new ScrollView(requireContext());
        GridLayout grid = new GridLayout(requireContext());
        grid.setColumnCount(4);
        int padding = (int) (12 * getResources().getDisplayMetrics().density);
        grid.setPadding(padding, padding, padding, padding);
        
        int availableWidth = getResources().getDisplayMetrics().widthPixels - (int) (96 * getResources().getDisplayMetrics().density); // roughly margins included
        int colWidth = availableWidth / 4;
        int margin = (int) (4 * getResources().getDisplayMetrics().density);
        int size = colWidth - (margin * 2);
        
        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
            .setTitle("Select Avatar")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Remove Avatar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    prefs.setUserAvatar("");
                    loadMyAvatar(prefs, avatarImg, avatarText);
                }
            })
            .create();

        for (final String url : ANIME_AVATARS) {
            ImageView img = new ImageView(requireContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = size;
            params.height = size;
            params.setMargins(margin, margin, margin, margin);
            img.setLayoutParams(params);
            img.setBackgroundResource(R.drawable.bg_avatar_cyan);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            
            Glide.with(this).load(url).circleCrop().into(img);
            
            img.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    prefs.setUserAvatar(url);
                    loadMyAvatar(prefs, avatarImg, avatarText);
                    dialog.dismiss();
                }
            });
            grid.addView(img);
        }
        
        scroll.addView(grid);
        dialog.show();
    }

    @Nullable
    public View onCreateView(@NonNull LayoutInflater inf, ViewGroup c, Bundle s) {
        root = inf.inflate(R.layout.fragment_me, c, false);
        View searchBtn = root.findViewById(R.id.header_search_btn);
        if (searchBtn != null) {
            searchBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    startActivity(new Intent(requireContext(), SearchActivity.class));
                }
            });
        }
        
        View notifBtn = root.findViewById(R.id.header_notification_btn);
        final View notifBadge = root.findViewById(R.id.header_notification_badge);
        if (notifBtn != null) {
            notifBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    NotificationsBottomSheet sheet = NotificationsBottomSheet.newInstance();
                    sheet.setCountListener(new NotificationsBottomSheet.CountListener() {
                        public void onCountReady(int unreadCount) {
                            if (notifBadge != null) {
                                notifBadge.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
                    sheet.show(getChildFragmentManager(), "NotificationsBottomSheet");
                }
            });
        }
        return root;
    }

    private android.os.Handler badgeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable badgeRunnable = new Runnable() {
        @Override
        public void run() {
            refreshNotificationBadge();
            badgeHandler.postDelayed(this, 10000);
        }
    };

    public void onResume() {
        super.onResume();
        if (root != null) setupContent();
        badgeHandler.post(badgeRunnable);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        badgeHandler.removeCallbacks(badgeRunnable);
    }

    private void refreshNotificationBadge() {
        final View notifBadge = root.findViewById(R.id.header_notification_badge);
        if (notifBadge == null || !isAdded()) return;
        String userId = AppPreferences.get(requireContext()).getUserId();
        if (userId == null) {
            android.content.SharedPreferences prefs = requireContext().getSharedPreferences("video_stats", android.content.Context.MODE_PRIVATE);
            userId = prefs.getString("user_id_uuid", null);
        }
        if (userId != null) {
            DatabaseService.get().getUnreadNotificationCount(userId, new DatabaseService.DataCallback<Integer>() {
                @Override
                public void onSuccess(Integer unread) {
                    if (getActivity() != null && notifBadge != null) {
                        notifBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                    }
                }

                @Override
                public void onError(Exception e) {}
            });
        }
    }

    private void setupContent() {
        refreshNotificationBadge();
        final AppPreferences prefs = AppPreferences.get(requireContext());
        String name = prefs.getUserName();

        final View avatarContainer = root.findViewById(R.id.me_avatar_container);
        final TextView avatarText  = root.findViewById(R.id.me_avatar_text);
        final ImageView avatarImg  = root.findViewById(R.id.me_avatar_img);
        final TextView nameText    = root.findViewById(R.id.me_name_text);
        TextView statCollected     = root.findViewById(R.id.stat_collected);
        TextView statHistory       = root.findViewById(R.id.stat_history);

        // FIXED: me_edit_btn is <ImageButton> in XML, not <Button>
        ImageButton editBtn        = root.findViewById(R.id.me_edit_btn);
        Button clearCacheBtn       = root.findViewById(R.id.clear_cache_btn);
        Button resetBtn            = root.findViewById(R.id.reset_btn);
        View cardCheckUpdate       = root.findViewById(R.id.card_check_update);
        View cardPrivacyPolicy     = root.findViewById(R.id.card_privacy_policy);
        TextView tvVersionLabel    = root.findViewById(R.id.tv_app_version_label);

        if (tvVersionLabel != null) {
            tvVersionLabel.setText("Version " + BuildConfig.VERSION_NAME + " (Build " + BuildConfig.VERSION_CODE + ")");
        }

        if (cardCheckUpdate != null) {
            cardCheckUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (getActivity() != null) {
                        RemoteUpdateManager.get().checkForUpdate(getActivity(), true);
                    }
                }
            });
        }

        if (cardPrivacyPolicy != null) {
            cardPrivacyPolicy.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPrivacyPolicyDialog();
                }
            });
        }

        final SwitchCompat themeSwitch = root.findViewById(R.id.theme_switch);
        final TextView themeLabel      = root.findViewById(R.id.theme_label);
        final ImageView themeIcon      = root.findViewById(R.id.theme_icon);
        
        if (themeIcon != null) {
            themeIcon.setImageResource(prefs.isDarkMode() ? R.drawable.ic_moon : R.drawable.ic_sun);
        }

        // Populate name + avatar
        if (name != null && !name.isEmpty()) {
            if (avatarText != null)
                avatarText.setText(String.valueOf(name.charAt(0)).toUpperCase());
            if (nameText != null)
                nameText.setText(name);
        }
        
        loadMyAvatar(prefs, avatarImg, avatarText);
        
        if (avatarContainer != null) {
            avatarContainer.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showAvatarSelectionDialog(prefs, avatarImg, avatarText);
                }
            });
        }

        // Stats
        if (statCollected != null)
            statCollected.setText(String.valueOf(prefs.getWatchlist().size()));
        if (statHistory != null)
            statHistory.setText(String.valueOf(prefs.getHistory().size()));

        // Theme switch
        if (themeSwitch != null) {
            themeSwitch.setChecked(!prefs.isDarkMode());
            if (themeLabel != null)
                themeLabel.setText(prefs.isDarkMode() ? "Dark Mode Active" : "Light Mode Active");
            themeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton btn, boolean checked) {
                    prefs.setDarkMode(!checked);
                    if (themeLabel != null)
                        themeLabel.setText(!checked ? "Dark Mode Active" : "Light Mode Active");
                    
                    if (!checked) {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                    } else {
                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
                    }
                    if (getActivity() != null) getActivity().recreate();
                }
            });
        }

        // Edit name
        if (editBtn != null) {
            editBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showEditNameDialog(prefs, nameText, avatarImg, avatarText);
                }
            });
        }
        
        // Clear Cache
        if (clearCacheBtn != null) {
            clearCacheBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Clear Media Cache")
                        .setMessage("This will delete all temporarily cached video streaming buffers and image cache to free up storage space. Your watchlist and history will NOT be deleted.")
                        .setPositiveButton("Clear Cache", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            // Clear Glide disk cache
                                            Glide.get(requireContext().getApplicationContext()).clearDiskCache();
                                            // Clear WebView cache
                                            if (getActivity() != null) {
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            new android.webkit.WebView(requireContext()).clearCache(true);
                                                        } catch (Exception ignored) {}
                                                    }
                                                });
                                            }
                                            // Delete app internal cache dir
                                            java.io.File cacheDir = requireContext().getCacheDir();
                                            deleteDirRecursive(cacheDir);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (getActivity() != null) {
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    android.widget.Toast.makeText(requireContext(), "Media cache cleared successfully!", android.widget.Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    }
                                }).start();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });
        }

        // Reset
        if (resetBtn != null) {
            resetBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    new AlertDialog.Builder(requireContext())
                        .setTitle("Reset Application")
                        .setMessage("Are you sure? All your data will be permanently deleted.")
                        .setPositiveButton("Yes, Reset", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                prefs.clearAll();
                                Intent i = new Intent(requireContext(), OnboardingActivity.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            });
        }
    }

    private void showEditNameDialog(final AppPreferences prefs,
                                    final TextView nameView,
                                    final ImageView avatarImg,
                                    final TextView avatarView) {
        final EditText input = new EditText(requireContext());
        input.setText(prefs.getUserName());
        input.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(10) });
        input.setSelectAllOnFocus(true);

        new AlertDialog.Builder(requireContext())
            .setTitle("Edit Name")
            .setView(input)
            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) {
                    String n = input.getText().toString().trim();
                    if (!n.isEmpty()) {
                        prefs.setUserName(n);
                        if (nameView != null)   nameView.setText(n);
                        loadMyAvatar(prefs, avatarImg, avatarView);
                    }
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showPrivacyPolicyDialog() {
        if (getContext() == null) return;
        ScrollView scrollView = new ScrollView(requireContext());
        TextView tv = new TextView(requireContext());
        int pad = (int) (18 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);
        tv.setTextColor(getResources().getColor(R.color.text_primary));
        tv.setLineSpacing(0, 1.25f);
        tv.setTextSize(13);
        tv.setText("Privacy Policy for WatchNinja\n"
                + "Last Updated: September 29, 2026\n\n"
                + "1. Information We Collect\n"
                + "WatchNinja does not require user registration or personal account details. Your watchlists, viewing history, and preferences are stored strictly locally on your device.\n\n"
                + "2. Third-Party Services\n"
                + "WatchNinja connects to third-party services including The Movie Database (TMDb) and Jikan/MyAnimeList for media information, and Google AdMob for advertisements.\n\n"
                + "3. Data Control & Deletion\n"
                + "You can clear cached data anytime using 'Clear Media Cache' or delete all local history and settings using 'Reset Application'.\n\n"
                + "4. Contact Us\n"
                + "If you have questions regarding privacy, email us at artbeastdm@gmail.com.");
        scrollView.addView(tv);

        new AlertDialog.Builder(requireContext())
            .setTitle("WatchNinja Privacy Policy")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show();
    }

    private static boolean deleteDirRecursive(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDirRecursive(new java.io.File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }
}
