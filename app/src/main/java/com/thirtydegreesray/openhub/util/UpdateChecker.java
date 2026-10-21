package com.thirtydegreesray.openhub.util;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thirtydegreesray.openhub.BuildConfig;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Manual update checker.
 * <p>
 * Queries the latest GitHub release of the fork repository and compares the
 * tag name with the currently installed version. Callback is always invoked
 * on the main thread.
 */
public final class UpdateChecker {

    public interface Callback {
        /**
         * @param result parsed release info, null when {@code error} is not null
         * @param error  network / parse error message, null on success
         */
        void onResult(@Nullable Result result, @Nullable String error);
    }

    public static class Result {
        public final boolean hasUpdate;
        public final String latestVersion;
        /** direct apk asset url when available, otherwise the releases page */
        public final String downloadUrl;

        Result(boolean hasUpdate, String latestVersion, String downloadUrl) {
            this.hasUpdate = hasUpdate;
            this.latestVersion = latestVersion;
            this.downloadUrl = downloadUrl;
        }
    }

    private static final String LATEST_RELEASE_API =
            "https://api.github.com/repos/rentianyu/OpenHub/releases/latest";
    private static final String RELEASE_PAGE =
            "https://github.com/rentianyu/OpenHub/releases/latest";

    private static final OkHttpClient client = new OkHttpClient();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private UpdateChecker() {
    }

    public static void check(final Callback callback) {
        Request request = new Request.Builder()
                .url(LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "OpenHub-Android/" + BuildConfig.VERSION_NAME)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                post(callback, null, e.getMessage() == null ? "network error" : e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                String body = null;
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        post(callback, null, "HTTP " + response.code());
                        return;
                    }
                    body = response.body().string();
                    post(callback, parseRelease(body), null);
                } catch (Exception e) {
                    post(callback, null, e.getMessage() == null ? "parse error" : e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }

    private static Result parseRelease(String json) {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        String tag = root.has("tag_name") && !root.get("tag_name").isJsonNull()
                ? root.get("tag_name").getAsString() : "";
        String latest = normalizeVersion(tag);
        if (latest.isEmpty()) {
            throw new IllegalStateException("no release tag found");
        }

        String apkUrl = null;
        if (root.has("assets") && root.get("assets").isJsonArray()) {
            JsonArray assets = root.getAsJsonArray("assets");
            for (JsonElement element : assets) {
                if (!element.isJsonObject()) continue;
                JsonObject asset = element.getAsJsonObject();
                String name = asset.has("name") ? asset.get("name").getAsString() : "";
                if (name.endsWith("-release.apk")) {
                    apkUrl = asset.has("browser_download_url")
                            ? asset.get("browser_download_url").getAsString() : null;
                    break;
                }
            }
            if (apkUrl == null) {
                for (JsonElement element : assets) {
                    if (!element.isJsonObject()) continue;
                    JsonObject asset = element.getAsJsonObject();
                    String name = asset.has("name") ? asset.get("name").getAsString() : "";
                    if (name.endsWith(".apk")) {
                        apkUrl = asset.has("browser_download_url")
                                ? asset.get("browser_download_url").getAsString() : null;
                        break;
                    }
                }
            }
        }

        boolean hasUpdate = isNewerVersion(latest, BuildConfig.VERSION_NAME);
        return new Result(hasUpdate, latest, apkUrl != null ? apkUrl : RELEASE_PAGE);
    }

    private static void post(final Callback callback, final Result result, final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(result, error);
            }
        });
    }

    static String normalizeVersion(String version) {
        if (version == null) return "";
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        return v;
    }

    /**
     * true when {@code remote} is newer than {@code current}.
     * Comparison is segment based, non numeric segments count as 0,
     * so "3.4" &gt; "3.3.2" and "3.3.2" == "3.3.2".
     */
    public static boolean isNewerVersion(String remote, String current) {
        String r = normalizeVersion(remote);
        String c = normalizeVersion(current);
        if (r.isEmpty()) return false;
        String[] remoteParts = r.split("[.\\-_+]");
        String[] currentParts = c.split("[.\\-_+]");
        int count = Math.max(remoteParts.length, currentParts.length);
        for (int i = 0; i < count; i++) {
            int rv = i < remoteParts.length ? toInt(remoteParts[i]) : 0;
            int cv = i < currentParts.length ? toInt(currentParts[i]) : 0;
            if (rv > cv) return true;
            if (rv < cv) return false;
        }
        return false;
    }

    private static int toInt(String segment) {
        String digits = segment.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
