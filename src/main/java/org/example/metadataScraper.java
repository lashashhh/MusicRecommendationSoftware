package org.example;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

public class metadataScraper {
    private static final String API_KEY = "f57b66b50c5683a01d2f2ee6d09d9d12";
    private static final String QUERRY = "INSERT INTO weekly_top_cache (track_name, artist_name, album_name, playcount, tags, album_image, cached_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, NOW())";
    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            .build();

    public static void setClient(OkHttpClient newClient){
        client = newClient;
    }

    private static boolean isUsefulTag(String raw) {
        if (raw == null) return false;

        String t = raw.trim().toLowerCase();

        if (t.isEmpty()) return false;
        if (t.contains("http") || t.contains("www.") || t.contains("@")) {
            return false;
        }

        int wordCount = t.split("\\s+").length;

        if (t.length() > 40 && wordCount >= 4 && t.matches(".*\\d{4}.*")) {
            return false;
        }
        if (t.matches(".*\\d+.*") && t.contains("fm")) {
            return false;
        }
        if (t.length() > 50 && wordCount >= 5) {
            return false;
        }

        return true;
    }

    private static String canonicalTagKey(String raw) {
        if (raw == null) return "";
        String t = raw.toLowerCase().trim();
        t = t.replaceAll("[\\s_\\-]+", "");
        return t;
    }

    public static void fetchTracks(String tag, int totalLimit) throws IOException {
        int page = 1;
        int collected = 0;
        int pageLimit = 350;

        while (collected < totalLimit) {
            String url = "https://ws.audioscrobbler.com/2.0/?method=tag.gettoptracks&tag=" +
                    URLEncoder.encode(tag, StandardCharsets.UTF_8) + "&api_key=" + API_KEY + "&format=json" + "&limit="
                    + pageLimit + "&page=" + page;

            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                String json = response.body().string();
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                JsonObject jsonTracks = root.getAsJsonObject("tracks");
                JsonArray tracks = jsonTracks.getAsJsonArray("track");

                if (tracks == null || tracks.isEmpty()) {
                    break;
                }

                for (int i = 0; i < tracks.size() && collected < totalLimit; i++) {
                    JsonObject track = tracks.get(i).getAsJsonObject();

                    String name = track.has("name") ? track.get("name").getAsString() : "unknown track";
                    String artist = track.has("artist") ? track.getAsJsonObject("artist").get("name").getAsString() : "unknown artist";
                    String playcountFromTag = track.has("playcount") ? track.get("playcount").getAsString() : "N/A";

                    TrackInfo info = fetchTrackInfo(artist, name);

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }

                    long finalPLaycount;
                    if (info.playcount > 0){
                        finalPLaycount = info.playcount;
                    } else if (!"N/A".equals(playcountFromTag)) {
                        finalPLaycount = Long.parseLong(playcountFromTag);
                    } else {
                        finalPLaycount = 0;
                    }

                    collected++;

                    System.out.println((i + 1) + ". " + name + " | " + artist + " | Album: " + info.album + " | Plays: " + finalPLaycount + " | Tags: " + info.tags + " | Album Image: " + info.albumImage);

                    try (Connection conn = Database.connect()) {
                        PreparedStatement songInsert = conn.prepareStatement(
                                "INSERT IGNORE INTO songs (track_name, artist_name, album_name, playcount, tags, album_image, source_tag) " +
                                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                                Statement.RETURN_GENERATED_KEYS
                        );
                        PreparedStatement songSelect = conn.prepareStatement(
                                "SELECT id FROM songs WHERE artist_name = ? AND track_name = ?"
                        );

                        songInsert.setString(1, name);
                        songInsert.setString(2, artist);
                        songInsert.setString(3, info.album);
                        songInsert.setLong(4, finalPLaycount);
                        songInsert.setString(5, info.tags);
                        songInsert.setString(6, info.albumImage);
                        songInsert.setString(7, tag);

                        int songId = -1;

                        int affected = songInsert.executeUpdate();
                        if (affected > 0) {
                            try (ResultSet rs = songInsert.getGeneratedKeys()) {
                                if (rs.next()) {
                                    songId = rs.getInt(1);
                                }
                            }
                        } else {
                            songSelect.setString(1, artist);
                            songSelect.setString(2, name);
                            try (ResultSet rs = songSelect.executeQuery()) {
                                if (rs.next()) {
                                    songId = rs.getInt(1);
                                }
                            }
                        }
                        if (songId != -1 && info.tags != null && !"N/A".equals(info.tags)) {
                            String[] tagList = info.tags.split(",\\s*");
                            for (String rawTag : tagList) {
                                String t = rawTag.trim();
                                if (t.isEmpty()) continue;
                                if (!isUsefulTag(t)) continue;

                                String canonical = canonicalTagKey(t);
                                if (canonical.isEmpty()) continue;

                                PreparedStatement tagStmt = conn.prepareStatement(
                                        "INSERT IGNORE INTO tags (song_id, tag_name) VALUES (?, ?)"
                                );
                                tagStmt.setInt(1, songId);
                                tagStmt.setString(2, canonical);
                                tagStmt.executeUpdate();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            page++;

            try{
                Thread.sleep(300);
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }
    }

        public static class TrackInfo {
            public String album;
            public String tags;
            public String albumImage;
            public long playcount;

            TrackInfo(String album, String tags, String albumImage, long playcount) {
                this.album = album;
                this.tags = tags;
                this.albumImage = albumImage;
                this.playcount = playcount;
            }
        }

    private static TrackInfo fetchTrackInfo (String artist, String track) {
        String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + API_KEY +
                "&artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8) +
                "&track=" + URLEncoder.encode(track, StandardCharsets.UTF_8) +
                "&format=json";

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return new TrackInfo("N/A", "N/A", "N/A", 0L);
            }

            String json = response.body().string();

            JsonElement rootElement = JsonParser.parseString(json);
            if (rootElement == null || !rootElement.isJsonObject()) {
                return new TrackInfo("N/A", "N/A", "N/A", 0L);
            }
            JsonObject root = rootElement.getAsJsonObject();
            if (root.has("error")) {
                return new TrackInfo("N/A", "N/A", "N/A", 0L);
            }

            String album = "(N/A)";
            StringBuilder tagsBuilder = new StringBuilder();
            String albumImage = "(N/A)";
            long playcount = 0L;

            JsonElement trackElement = root.get("track");
            if (trackElement == null || !trackElement.isJsonObject()) {
                return new TrackInfo("N/A", "N/A", "N/A", 0L);
            }

            JsonObject trackObj = trackElement.getAsJsonObject();

            if (trackObj.has("playcount")) {
                try {
                    playcount = trackObj.get("playcount").getAsLong();
                } catch (Exception ignored) {}
            }
            if (trackObj.has("album")) {
                JsonObject albumObj = trackObj.getAsJsonObject("album");
                if (albumObj.has("title")) {
                    album = albumObj.get("title").getAsString();
                }
                if (albumObj.has("image")) {
                    JsonArray images = albumObj.getAsJsonArray("image");
                    for (JsonElement imgElem : images) {
                        JsonObject imgObj = imgElem.getAsJsonObject();
                        if (imgObj.has("size") && "extralarge".equals(imgObj.get("size").getAsString())) {
                            String img = imgObj.get("#text").getAsString();
                            if (img != null && !img.isBlank()) {
                                albumImage = img;
                            }
                            break;
                        }
                    }
                }
            }
            if (trackObj.has("toptags")) {
                JsonObject toptags = trackObj.getAsJsonObject("toptags");
                if (toptags.has("tag")) {
                    JsonArray tagsArray = toptags.getAsJsonArray("tag");
                    for (int i = 0; i < Math.min(5, tagsArray.size()); i++) {
                        JsonObject tagObj = tagsArray.get(i).getAsJsonObject();
                        if (tagObj.has("name")) {
                            if (tagsBuilder.length() > 0) tagsBuilder.append(", ");
                            tagsBuilder.append(tagObj.get("name").getAsString());
                        }
                    }
                }
            }
            return new TrackInfo(
                    album,
                    tagsBuilder.length() > 0 ? tagsBuilder.toString() : "N/A",
                    albumImage,
                    playcount
            );
        } catch (IOException e) {
            System.err.println("[TRACK_INFO_ERROR] " + artist + " - " + track + " : " + e.getMessage());
            return new TrackInfo("N/A", "N/A", "N/A", 0L);
        }
    }



    public static String[] getTopTags(int limit) throws IOException {
        String url = "https://ws.audioscrobbler.com/2.0/?method=tag.getTopTags&api_key=" + "f57b66b50c5683a01d2f2ee6d09d9d12" + "&format=json";

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String json = response.body().string();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject tagsObj = root.getAsJsonObject("toptags");
            JsonArray tagsArray = tagsObj.getAsJsonArray("tag");

            String[] tags = new String[Math.min(limit, tagsArray.size())];
            for (int i = 0; i < tags.length; i++) {
                JsonObject tagObj = tagsArray.get(i).getAsJsonObject();
                tags[i] = tagObj.get("name").getAsString();
            }
            return tags;
        }
    }

    public static void fetchTracksForTopTags (int tagLimit, int tracksPerTag) throws IOException {
        String[] tags = getTopTags(tagLimit);
        for (String tag : tags) {
            System.out.println("\n=== Fetching for tag: " + tag + " ===");
            fetchTracks(tag, tracksPerTag);
        }
    }

    public static void fetchWeeklyTopTracks(int limit) throws IOException {
        try (Connection conn = Database.connect()){
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT MAX(cached_at) AS last_cached FROM weekly_top_cache"
            );
            if (rs.next() && rs.getTimestamp("last_cached") != null) {
                long diff = System.currentTimeMillis() - rs.getTimestamp("last_cached").getTime();
                long days = diff / (24 * 60 * 60 * 1000);
                if (days < 7) {
                    System.out.println("[CACHE] using cached weekly top tracks (less than a week old)");
                    return;
                } else {
                    System.out.println("[CACHE] Cache expired. Refreshing data...");
                    conn.createStatement().executeUpdate("DELETE FROM weekly_top_cache");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String url = "https://ws.audioscrobbler.com/2.0/?method=chart.gettoptracks&api_key=" + API_KEY + "&format=json&limit=" + limit;
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

            String json = response.body().string();
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonArray tracks = root.getAsJsonObject("tracks").getAsJsonArray("track");

            if (tracks == null || tracks.isEmpty()) {
                System.out.println("[WeeklyTopTracks] No tracks found");
                return;
            }

            try (Connection conn = Database.connect()) {
                PreparedStatement stmt = conn.prepareStatement(QUERRY);
                int count = 0;
                for (JsonElement el : tracks) {
                    if (count >= limit) break;
                    JsonObject track = el.getAsJsonObject();

                    String name = track.has("name") ? track.get("name").getAsString() : "unknown track";
                    String artist = track.has("artist") ? track.getAsJsonObject("artist").get("name").getAsString() : "unknown artist";
                    String playcountStr = track.has("playcount") ? track.get("playcount").getAsString() : "0";

                    String chartImage = "(N/A)";
                    if (track.has("image")) {
                        JsonArray images = track.getAsJsonArray("image");
                        for (JsonElement imgElem : images) {
                            JsonObject imgObj = imgElem.getAsJsonObject();
                            if (imgObj.has("size") && imgObj.get("size").getAsString().equals("extralarge")) {
                                String imageUrl = imgObj.get("#text").getAsString();
                                if (imageUrl != null && !imageUrl.isBlank()){
                                    if(imageUrl.startsWith("http://")){
                                        chartImage = "https://" + imageUrl.substring("http://".length());
                                    } else {
                                        chartImage = imageUrl;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    TrackInfo info = fetchTrackInfo(artist, name);

                    String finalImage = (info.albumImage != null && !info.albumImage.isBlank() && !"N/A".equals(info.albumImage)) ? info.albumImage : chartImage;
                    long finalPlaycount;
                    if(info.playcount > 0){
                        finalPlaycount = info.playcount;
                    } else {
                        finalPlaycount = Long.parseLong(playcountStr);
                    }

                    stmt.setString(1, name);
                    stmt.setString(2, artist);
                    stmt.setString(3, info.album);
                    stmt.setLong(4, finalPlaycount);
                    stmt.setString(5, info.tags);
                    stmt.setString(6, finalImage);
                    stmt.executeUpdate();


                    System.out.println((count + 1) + ". " + name + " | " + artist + " | Plays: " + finalPlaycount + " | Tags: " + info.tags + " | Album Image: " + finalImage);

                    count++;
                }
                stmt.executeBatch();
                System.out.println("[WeeklyTopTracks] Successfully fetched and saved top " + count + " tracks of the week");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}