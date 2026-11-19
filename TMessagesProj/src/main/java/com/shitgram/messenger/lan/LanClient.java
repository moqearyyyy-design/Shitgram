/*
 * Shitgram LAN Client
 * Communicates with the local Shitgram backend server.
 */
package com.shitgram.messenger.lan;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.telegram.messenger.AndroidUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LanClient {
    private static final String TAG = "LanClient";
    private static final String PREFS_NAME = "shitgram_lan";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_STARS = "stars";

    private static LanClient instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String serverUrl;
    private String token;
    private String userId;
    private String userName;
    private int stars;
    private LanClientListener listener;

    public interface LanClientListener {
        void onMessagesReceived(List<LanMessage> messages);
        void onMessageSent(LanMessage message);
        void onGiftSent(LanMessage gift);
        void onStarsUpdated(int stars);
        void onError(String error);
        void onUserListReceived(List<LanUser> users);
    }

    public static class LanMessage {
        public long id;
        public String userId;
        public String name;
        public String text;
        public long sentAt;
        public String type; // "text" or "gift"
        public String to; // for gifts
        public int cost; // for gifts

        public boolean isOutgoing() {
            return userId != null && userId.equals(LanClient.getInstance(null).getUserId());
        }

        public boolean isGift() {
            return "gift".equals(type);
        }
    }

    public static class LanUser {
        public String id;
        public String name;
        public int stars;
    }

    private LanClient(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadFromPrefs();
    }

    public static synchronized LanClient getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new LanClient(context);
        }
        return instance;
    }

    public static LanClient getInstanceUnsafe() {
        return instance;
    }

    private void loadFromPrefs() {
        serverUrl = prefs.getString(KEY_SERVER_URL, "");
        token = prefs.getString(KEY_TOKEN, "");
        userId = prefs.getString(KEY_USER_ID, "");
        userName = prefs.getString(KEY_USER_NAME, "");
        stars = prefs.getInt(KEY_STARS, 0);
    }

    private void saveToPrefs() {
        prefs.edit()
                .putString(KEY_SERVER_URL, serverUrl)
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putInt(KEY_STARS, stars)
                .apply();
    }

    public void setListener(LanClientListener listener) {
        this.listener = listener;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty() && userId != null && !userId.isEmpty();
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String url) {
        this.serverUrl = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        saveToPrefs();
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public int getStars() {
        return stars;
    }

    public void login(String name, String serverUrl, LanLoginCallback callback) {
        this.serverUrl = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("name", name);
                String response = post("/api/session", body.toString(), null);
                JSONObject json = new JSONObject(response);
                if (json.has("token")) {
                    token = json.getString("token");
                    JSONObject user = json.getJSONObject("user");
                    userId = user.getString("id");
                    userName = user.getString("name");
                    stars = user.getInt("stars");
                    saveToPrefs();
                    AndroidUtilities.runOnUIThread(() -> callback.onSuccess());
                } else {
                    String error = json.optString("error", "Unknown error");
                    AndroidUtilities.runOnUIThread(() -> callback.onError(error));
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                AndroidUtilities.runOnUIThread(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public interface LanLoginCallback {
        void onSuccess();
        void onError(String error);
    }

    public void fetchMessages(long after) {
        executor.execute(() -> {
            try {
                String url = "/api/messages?after=" + after;
                String response = get(url);
                JSONObject json = new JSONObject(response);
                if (json.has("messages")) {
                    JSONArray arr = json.getJSONArray("messages");
                    List<LanMessage> messages = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        messages.add(parseMessage(arr.getJSONObject(i)));
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onMessagesReceived(messages);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch messages error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
            }
        });
    }

    public void sendMessage(String text) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("text", text);
                String response = post("/api/messages", body.toString(), token);
                JSONObject json = new JSONObject(response);
                if (json.has("id")) {
                    LanMessage msg = parseMessage(json);
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onMessageSent(msg);
                        }
                    });
                } else {
                    String error = json.optString("error", "Failed to send");
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onError(error);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Send message error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
            }
        });
    }

    public void sendGift(String toName, String title, int cost) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("to", toName);
                body.put("title", title);
                body.put("cost", cost);
                String response = post("/api/gifts/send", body.toString(), token);
                JSONObject json = new JSONObject(response);
                if (json.has("id")) {
                    LanMessage gift = parseMessage(json);
                    stars = json.optInt("stars", stars - cost);
                    saveToPrefs();
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onGiftSent(gift);
                            listener.onStarsUpdated(stars);
                        }
                    });
                } else {
                    String error = json.optString("error", "Failed to send gift");
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onError(error);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Send gift error", e);
                AndroidUtilities.runOnUIThread(() -> {
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
            }
        });
    }

    public void fetchMe() {
        executor.execute(() -> {
            try {
                String response = get("/api/me");
                JSONObject json = new JSONObject(response);
                stars = json.getInt("stars");
                saveToPrefs();
                AndroidUtilities.runOnUIThread(() -> {
                    if (listener != null) {
                        listener.onStarsUpdated(stars);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Fetch me error", e);
            }
        });
    }

    public void fetchUsers() {
        executor.execute(() -> {
            try {
                String response = get("/api/messages?after=0");
                JSONObject json = new JSONObject(response);
                if (json.has("messages")) {
                    JSONArray arr = json.getJSONArray("messages");
                    List<LanUser> users = new ArrayList<>();
                    // Extract unique users from messages
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject msg = arr.getJSONObject(i);
                        String uid = msg.optString("userId");
                        String name = msg.optString("name");
                        if (uid != null && name != null) {
                            boolean found = false;
                            for (LanUser u : users) {
                                if (u.id.equals(uid)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                LanUser u = new LanUser();
                                u.id = uid;
                                u.name = name;
                                u.stars = 0; // We don't have stars in message list
                                users.add(u);
                            }
                        }
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        if (listener != null) {
                            listener.onUserListReceived(users);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Fetch users error", e);
            }
        });
    }

    public void logout() {
        token = "";
        userId = "";
        userName = "";
        stars = 0;
        saveToPrefs();
    }

    private LanMessage parseMessage(JSONObject json) throws JSONException {
        LanMessage msg = new LanMessage();
        msg.id = json.getLong("id");
        msg.userId = json.getString("userId");
        msg.name = json.getString("name");
        msg.text = json.getString("text");
        msg.sentAt = json.getLong("sentAt");
        msg.type = json.getString("type");
        if (json.has("to")) msg.to = json.getString("to");
        if (json.has("cost")) msg.cost = json.getInt("cost");
        return msg;
    }

    private String get(String path) throws Exception {
        return request("GET", path, null, token);
    }

    private String post(String path, String body, String authToken) throws Exception {
        return request("POST", path, body, authToken);
    }

    private String request(String method, String path, String body, String authToken) throws Exception {
        URL url = new URL(serverUrl + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");
        if (authToken != null && !authToken.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + authToken);
        }

        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode >= 400) {
            throw new Exception("HTTP " + responseCode + ": " + response);
        }

        return response.toString();
    }

    public void shutdown() {
        executor.shutdown();
    }
}