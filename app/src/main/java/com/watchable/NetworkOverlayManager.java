package com.watchable;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.lang.ref.WeakReference;

public class NetworkOverlayManager implements Application.ActivityLifecycleCallbacks {

    private static final String GIF_URL = "https://raw.githubusercontent.com/zsecre/Watchable/refs/heads/main/loading.gif";
    private static NetworkOverlayManager instance;

    private final Application application;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private WeakReference<Activity> currentActivityRef = new WeakReference<>(null);

    private boolean isConnected = true;
    private boolean wasEverDisconnected = false;
    private Runnable hideOverlayRunnable;

    private final Runnable connectivityCheckRunnable = new Runnable() {
        @Override
        public void run() {
            boolean available = checkNetworkAvailable(application);
            if (available != isConnected) {
                isConnected = available;
                if (!isConnected) {
                    wasEverDisconnected = true;
                    showLostOverlayOnCurrentActivity();
                } else {
                    if (wasEverDisconnected) {
                        showRestoredOverlayOnCurrentActivity();
                    }
                }
            }
        }
    };

    private NetworkOverlayManager(Application application) {
        this.application = application;
    }

    public static synchronized void init(Application application) {
        if (instance == null) {
            instance = new NetworkOverlayManager(application);
            instance.start();
        }
    }

    public static NetworkOverlayManager getInstance() {
        return instance;
    }

    private void start() {
        application.registerActivityLifecycleCallbacks(this);

        // Check initial connectivity
        isConnected = checkNetworkAvailable(application);
        wasEverDisconnected = !isConnected;

        // Preload GIF in memory/disk
        try {
            Glide.with(application)
                .asGif()
                .load(GIF_URL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(Glide.with(application).asGif().load(R.raw.loading))
                .preload();
        } catch (Exception ignored) {}

        // Listen for connectivity changes
        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        try {
            ConnectivityManager cm = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return;

            ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    evaluateConnectivityDebounced();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    evaluateConnectivityDebounced();
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
                    evaluateConnectivityDebounced();
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                cm.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
                cm.registerNetworkCallback(request, networkCallback);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void evaluateConnectivityDebounced() {
        mainHandler.removeCallbacks(connectivityCheckRunnable);
        mainHandler.postDelayed(connectivityCheckRunnable, 250);
    }

    public static boolean isNetworkAvailable(Context context) {
        return checkNetworkAvailable(context);
    }

    private static boolean checkNetworkAvailable(Context context) {
        if (context == null) return true;
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network active = cm.getActiveNetwork();
                if (active == null) return false;
                NetworkCapabilities caps = cm.getNetworkCapabilities(active);
                return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                return info != null && info.isConnected();
            }
        } catch (Exception e) {
            return true;
        }
    }

    private void showLostOverlayOnCurrentActivity() {
        mainHandler.post(() -> {
            Activity act = currentActivityRef.get();
            if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                showLostOverlay(act);
            }
        });
    }

    private void showRestoredOverlayOnCurrentActivity() {
        mainHandler.post(() -> {
            Activity act = currentActivityRef.get();
            if (act != null && !act.isFinishing() && !act.isDestroyed()) {
                showRestoredOverlay(act);
            }
        });
    }

    private void showLostOverlay(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        if (hideOverlayRunnable != null) {
            mainHandler.removeCallbacks(hideOverlayRunnable);
            hideOverlayRunnable = null;
        }

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View overlay = root.findViewById(R.id.network_loading_overlay);
        if (overlay == null) {
            overlay = LayoutInflater.from(activity).inflate(R.layout.layout_network_overlay, root, false);
            root.addView(overlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }

        overlay.setVisibility(View.VISIBLE);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        overlay.setOnClickListener(v -> {}); // Absorb click events

        TextView tv = overlay.findViewById(R.id.network_loading_text);
        if (tv != null) {
            tv.setText(R.string.network_connection_lost);
        }

        ImageView gifView = overlay.findViewById(R.id.network_loading_gif);
        if (gifView != null) {
            loadGif(activity, gifView);
        }
    }

    private void showRestoredOverlay(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View overlay = root.findViewById(R.id.network_loading_overlay);
        if (overlay == null || overlay.getVisibility() != View.VISIBLE) {
            return;
        }

        TextView tv = overlay.findViewById(R.id.network_loading_text);
        if (tv != null) {
            tv.setText(R.string.network_connection_restored);
        }

        if (hideOverlayRunnable != null) {
            mainHandler.removeCallbacks(hideOverlayRunnable);
        }

        hideOverlayRunnable = () -> {
            if (isConnected) {
                removeOverlay(activity);
            }
        };
        mainHandler.postDelayed(hideOverlayRunnable, 1500);
    }

    private void removeOverlay(Activity activity) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        ViewGroup root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        View overlay = root.findViewById(R.id.network_loading_overlay);
        if (overlay != null) {
            ImageView gifView = overlay.findViewById(R.id.network_loading_gif);
            if (gifView != null) {
                try {
                    Glide.with(activity).clear(gifView);
                } catch (Exception ignored) {}
            }
            root.removeView(overlay);
        }
    }

    private void loadGif(Context context, ImageView gifView) {
        try {
            Glide.with(context)
                .asGif()
                .load(GIF_URL)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(Glide.with(context).asGif().load(R.raw.loading))
                .into(gifView);
        } catch (Exception e) {
            try {
                Glide.with(context).asGif().load(R.raw.loading).into(gifView);
            } catch (Exception ignored) {}
        }
    }

    // ActivityLifecycleCallbacks
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivityRef = new WeakReference<>(activity);
        if (!isConnected) {
            showLostOverlay(activity);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        if (hideOverlayRunnable != null) {
            mainHandler.removeCallbacks(hideOverlayRunnable);
        }
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        removeOverlay(activity);
        Activity current = currentActivityRef.get();
        if (current == activity) {
            currentActivityRef.clear();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}
    @Override
    public void onActivityStarted(@NonNull Activity activity) {}
    @Override
    public void onActivityStopped(@NonNull Activity activity) {}
    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}
}
