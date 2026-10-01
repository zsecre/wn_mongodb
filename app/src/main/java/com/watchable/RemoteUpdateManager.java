package com.watchable;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class RemoteUpdateManager implements Application.ActivityLifecycleCallbacks {

    private static RemoteUpdateManager instance;
    private final OkHttpClient client;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isChecking = false;

    private File pendingInstallApkFile = null;
    private boolean pendingForceUpdate = false;
    private WeakReference<Dialog> currentDialogRef = new WeakReference<>(null);

    private RemoteUpdateManager() {
        client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build();
    }

    public static synchronized void init(Application application) {
        if (instance == null) {
            instance = new RemoteUpdateManager();
        }
        if (application != null) {
            application.registerActivityLifecycleCallbacks(instance);
        }
    }

    public static synchronized RemoteUpdateManager get() {
        if (instance == null) {
            instance = new RemoteUpdateManager();
        }
        return instance;
    }

    public void checkForUpdate(final Activity activity, final boolean isManualCheck) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        if (isChecking) return;

        isChecking = true;

        if (isManualCheck) {
            Toast.makeText(activity, "Checking for updates...", Toast.LENGTH_SHORT).show();
        }

        Request request = new Request.Builder()
            .url(Constants.APP_UPDATE_URL)
            .addHeader("User-Agent", "WatchNinja-Android/" + BuildConfig.VERSION_NAME)
            .addHeader("Cache-Control", "no-cache")
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                isChecking = false;
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        if (isManualCheck) {
                            Toast.makeText(activity, "Unable to check for updates. Please verify your internet connection.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isChecking = false;
                if (!response.isSuccessful()) {
                    if (isManualCheck) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    Toast.makeText(activity, "Could not fetch update information.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    return;
                }

                String responseBody = "";
                if (response.body() != null) {
                    responseBody = response.body().string();
                }

                try {
                    JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                    final AppUpdateModel updateModel = AppUpdateModel.fromJson(json);

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing() || activity.isDestroyed()) return;

                            int currentVersionCode = BuildConfig.VERSION_CODE;
                            if (updateModel.getVersionCode() > currentVersionCode) {
                                showUpdateDialog(activity, updateModel);
                            } else if (isManualCheck) {
                                Toast.makeText(activity, "WatchNinja is up to date! (v" + BuildConfig.VERSION_NAME + ")", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    if (isManualCheck) {
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    Toast.makeText(activity, "Failed to parse update information.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }
        });
    }

    public void showUpdateDialog(final Activity activity, final AppUpdateModel model) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app_update);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialog.setCancelable(!model.isForceUpdate());
        dialog.setCanceledOnTouchOutside(!model.isForceUpdate());

        currentDialogRef = new WeakReference<>(dialog);

        TextView tvTitle = dialog.findViewById(R.id.tv_update_title);
        TextView tvVersion = dialog.findViewById(R.id.tv_update_version);
        TextView tvChangelog = dialog.findViewById(R.id.tv_update_changelog);
        final LinearLayout layoutProgress = dialog.findViewById(R.id.layout_download_progress);
        final TextView tvStatus = dialog.findViewById(R.id.tv_download_status);
        final TextView tvPercentage = dialog.findViewById(R.id.tv_download_percentage);
        final ProgressBar pbProgress = dialog.findViewById(R.id.pb_download_progress);
        final Button btnUpdateNow = dialog.findViewById(R.id.btn_update_now);
        final Button btnUpdateLater = dialog.findViewById(R.id.btn_update_later);
        final ImageButton btnClose = dialog.findViewById(R.id.btn_close_update);

        if (model.getTitle() != null && !model.getTitle().isEmpty()) {
            tvTitle.setText(model.getTitle());
        }

        String versionText = "v" + model.getVersionName() + " (Current: v" + BuildConfig.VERSION_NAME + ")";
        if (model.getDownloadSize() != null && !model.getDownloadSize().isEmpty()) {
            versionText += " • " + model.getDownloadSize();
        }
        tvVersion.setText(versionText);

        if (model.getChangelog() != null && !model.getChangelog().isEmpty()) {
            tvChangelog.setText(model.getChangelog());
        }

        if (model.isForceUpdate()) {
            btnUpdateLater.setVisibility(View.GONE);
            btnClose.setVisibility(View.GONE);
        } else {
            btnUpdateLater.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        // Check if target APK was already downloaded previously
        final File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        final File apkFile = new File(downloadDir, "WatchNinja_Update.apk");
        boolean isAlreadyDownloaded = false;

        if (apkFile.exists() && apkFile.length() > 0) {
            try {
                PackageManager pm = activity.getPackageManager();
                PackageInfo pi = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
                if (pi != null && pi.versionCode == model.getVersionCode()) {
                    isAlreadyDownloaded = true;
                }
            } catch (Exception ignored) {}
        }

        if (isAlreadyDownloaded) {
            layoutProgress.setVisibility(View.VISIBLE);
            pbProgress.setProgress(100);
            tvPercentage.setText("100%");
            tvStatus.setText("Update is downloaded and ready to install.");
            btnUpdateNow.setText("Install Update");
            btnUpdateNow.setEnabled(true);
            btnUpdateNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    installApk(activity, apkFile, model.isForceUpdate());
                }
            });
        } else {
            btnUpdateNow.setText("Update Now");
            btnUpdateNow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String apkUrl = model.getApkUrl();
                    if (apkUrl == null || apkUrl.isEmpty()) {
                        Toast.makeText(activity, "Download link not available.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    btnUpdateNow.setEnabled(false);
                    btnUpdateNow.setText("Downloading...");
                    btnUpdateLater.setVisibility(View.GONE);
                    btnClose.setVisibility(View.GONE);
                    layoutProgress.setVisibility(View.VISIBLE);

                    startDownload(activity, apkUrl, dialog, tvStatus, tvPercentage, pbProgress, btnUpdateNow, model.isForceUpdate());
                }
            });
        }

        dialog.show();
    }

    private void startDownload(final Activity activity,
                              final String apkUrl,
                              final Dialog dialog,
                              final TextView tvStatus,
                              final TextView tvPercentage,
                              final ProgressBar pbProgress,
                              final Button btnUpdateNow,
                              final boolean isForceUpdate) {

        final File downloadDir = activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null && !downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        final File apkFile = new File(downloadDir, "WatchNinja_Update.apk");
        if (apkFile.exists()) {
            apkFile.delete();
        }

        Request request = new Request.Builder()
            .url(apkUrl)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        tvStatus.setText("Download failed. Tap Update Now to retry.");
                        btnUpdateNow.setEnabled(true);
                        btnUpdateNow.setText("Retry Update");
                        Toast.makeText(activity, "Download error. Please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            tvStatus.setText("Server error. Tap to retry.");
                            btnUpdateNow.setEnabled(true);
                            btnUpdateNow.setText("Retry Update");
                            Toast.makeText(activity, "Failed to download update file.", Toast.LENGTH_LONG).show();
                        }
                    });
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }

                final long totalBytes = body.contentLength();
                InputStream is = null;
                FileOutputStream fos = null;

                try {
                    is = body.byteStream();
                    fos = new FileOutputStream(apkFile);

                    byte[] buffer = new byte[8192];
                    long bytesDownloaded = 0;
                    int read;
                    long lastUpdateTime = 0;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        bytesDownloaded += read;

                        long now = System.currentTimeMillis();
                        if (now - lastUpdateTime > 100 || bytesDownloaded == totalBytes) {
                            lastUpdateTime = now;
                            final int progress = totalBytes > 0 ? (int) ((bytesDownloaded * 100) / totalBytes) : 0;
                            final long currentDownloaded = bytesDownloaded;

                            mainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (activity.isFinishing() || activity.isDestroyed()) return;
                                    pbProgress.setProgress(progress);
                                    tvPercentage.setText(progress + "%");
                                    if (totalBytes > 0) {
                                        double currentMb = currentDownloaded / (1024.0 * 1024.0);
                                        double totalMb = totalBytes / (1024.0 * 1024.0);
                                        tvStatus.setText(String.format("Downloading: %.1f MB / %.1f MB", currentMb, totalMb));
                                    } else {
                                        tvStatus.setText(String.format("Downloaded: %.1f MB", currentDownloaded / (1024.0 * 1024.0)));
                                    }
                                }
                            });
                        }
                    }

                    fos.flush();

                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            tvStatus.setText("Download complete! Preparing installer...");
                            pbProgress.setProgress(100);
                            tvPercentage.setText("100%");
                            btnUpdateNow.setText("Install Update");
                            btnUpdateNow.setEnabled(true);

                            btnUpdateNow.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    installApk(activity, apkFile, isForceUpdate);
                                }
                            });

                            // Directly invoke install APK
                            installApk(activity, apkFile, isForceUpdate);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            tvStatus.setText("Download interrupted. Tap to retry.");
                            btnUpdateNow.setEnabled(true);
                            btnUpdateNow.setText("Retry Update");
                        }
                    });
                } finally {
                    try { if (is != null) is.close(); } catch (Exception ignored) {}
                    try { if (fos != null) fos.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    public static void installApk(Context context, File apkFile) {
        get().installApk(context, apkFile, false);
    }

    public void installApk(Context context, File apkFile, boolean isForceUpdate) {
        if (context == null || apkFile == null || !apkFile.exists()) {
            return;
        }

        try {
            // Check unknown source install permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.getPackageManager().canRequestPackageInstalls()) {
                    pendingInstallApkFile = apkFile;
                    pendingForceUpdate = isForceUpdate;
                    
                    Intent unknownAppIntent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName())
                    );
                    if (!(context instanceof Activity)) {
                        unknownAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    }
                    context.startActivity(unknownAppIntent);
                    Toast.makeText(context, "Please allow WatchNinja to install updates, then return to the app", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Permission is granted! Clear pending state
            pendingInstallApkFile = null;

            Uri apkUri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                apkFile
            );

            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!(context instanceof Activity)) {
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(installIntent);

            // Dismiss dialog once installer is successfully presented (if not forced)
            if (!isForceUpdate) {
                Dialog activeDialog = currentDialogRef.get();
                if (activeDialog != null && activeDialog.isShowing()) {
                    try {
                        activeDialog.dismiss();
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to launch installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ActivityLifecycleCallbacks to auto-launch installer when returning from Settings
    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        if (pendingInstallApkFile != null && pendingInstallApkFile.exists()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activity.getPackageManager().canRequestPackageInstalls()) {
                File apkToInstall = pendingInstallApkFile;
                boolean force = pendingForceUpdate;
                pendingInstallApkFile = null;
                installApk(activity, apkToInstall, force);
            }
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {}

    @Override
    public void onActivityPaused(@NonNull Activity activity) {}

    @Override
    public void onActivityStopped(@NonNull Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}
