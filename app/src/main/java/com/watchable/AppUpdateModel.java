package com.watchable;

import com.google.gson.JsonObject;

public class AppUpdateModel {
    private int versionCode = 1;
    private String versionName = "1.0.0";
    private boolean forceUpdate = false;
    private String title = "Update Available";
    private String changelog = "• Bug fixes and performance improvements";
    private String apkUrl = "";
    private String downloadSize = "";

    public static AppUpdateModel fromJson(JsonObject obj) {
        AppUpdateModel model = new AppUpdateModel();
        if (obj == null) return model;

        if (obj.has("app_update") && obj.get("app_update").isJsonObject()) {
            obj = obj.getAsJsonObject("app_update");
        }

        if (obj.has("latest_version_code") && !obj.get("latest_version_code").isJsonNull()) {
            model.versionCode = obj.get("latest_version_code").getAsInt();
        } else if (obj.has("versionCode") && !obj.get("versionCode").isJsonNull()) {
            model.versionCode = obj.get("versionCode").getAsInt();
        }

        if (obj.has("latest_version_name") && !obj.get("latest_version_name").isJsonNull()) {
            model.versionName = obj.get("latest_version_name").getAsString();
        } else if (obj.has("versionName") && !obj.get("versionName").isJsonNull()) {
            model.versionName = obj.get("versionName").getAsString();
        }

        if (obj.has("force_update") && !obj.get("force_update").isJsonNull()) {
            model.forceUpdate = obj.get("force_update").getAsBoolean();
        } else if (obj.has("forceUpdate") && !obj.get("forceUpdate").isJsonNull()) {
            model.forceUpdate = obj.get("forceUpdate").getAsBoolean();
        }

        if (obj.has("title") && !obj.get("title").isJsonNull()) {
            model.title = obj.get("title").getAsString();
        }

        if (obj.has("changelog") && !obj.get("changelog").isJsonNull()) {
            model.changelog = obj.get("changelog").getAsString();
        }

        if (obj.has("apk_download_url") && !obj.get("apk_download_url").isJsonNull()) {
            model.apkUrl = normalizeUrl(obj.get("apk_download_url").getAsString());
        } else if (obj.has("apkUrl") && !obj.get("apkUrl").isJsonNull()) {
            model.apkUrl = normalizeUrl(obj.get("apkUrl").getAsString());
        } else if (obj.has("url") && !obj.get("url").isJsonNull()) {
            model.apkUrl = normalizeUrl(obj.get("url").getAsString());
        }

        if (obj.has("size") && !obj.get("size").isJsonNull()) {
            model.downloadSize = obj.get("size").getAsString();
        } else if (obj.has("downloadSize") && !obj.get("downloadSize").isJsonNull()) {
            model.downloadSize = obj.get("downloadSize").getAsString();
        }

        return model;
    }

    private static String normalizeUrl(String url) {
        if (url == null) return "";
        url = url.trim();
        // Handle Google Drive share links
        if (url.contains("drive.google.com")) {
            String fileId = null;
            if (url.contains("/file/d/")) {
                int start = url.indexOf("/file/d/") + 8;
                int end = url.indexOf("/", start);
                if (end == -1) end = url.indexOf("?", start);
                if (end == -1) fileId = url.substring(start);
                else fileId = url.substring(start, end);
            } else if (url.contains("id=")) {
                int start = url.indexOf("id=") + 3;
                int end = url.indexOf("&", start);
                if (end == -1) fileId = url.substring(start);
                else fileId = url.substring(start, end);
            }
            if (fileId != null && !fileId.isEmpty()) {
                return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download";
            }
        }
        // Handle tmpfiles.org web link to direct dl link
        if (url.contains("tmpfiles.org/") && !url.contains("tmpfiles.org/dl/")) {
            url = url.replace("tmpfiles.org/", "tmpfiles.org/dl/");
        }
        return url;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public boolean isForceUpdate() {
        return forceUpdate;
    }

    public String getTitle() {
        return title;
    }

    public String getChangelog() {
        return changelog;
    }

    public String getApkUrl() {
        return apkUrl;
    }

    public String getDownloadSize() {
        return downloadSize;
    }
}
