package org.example;
import com.google.gson.*;
import okhttp3.*;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class metadataScraper {
    private static final String API_KEY = "f57b66b50c5683a01d2f2ee6d09d9d12";
    private static OkHttpClient client = new OkHttpClient();

    public static void setClient(OkHttpClient newClient){
        client = newClient;
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

               // System.out.println("Top " + totalLimit + "track " + tracks);
                for (int i = 0; i < tracks.size() && collected < totalLimit; i++) {
                    JsonObject track = tracks.get(i).getAsJsonObject();

                    String name = track.has("name") ? track.get("name").getAsString() : "unknown track";
                    String artist = track.has("artist") ? track.getAsJsonObject("artist").get("name").getAsString() : "unknown artist";
                    String playcount = track.has("playcount") ? track.get("playcount").getAsString() : "N/A";

                    TrackInfo info = fetchTrackInfo(artist, name);

                    collected++;

                    System.out.println((i + 1) + ". " + name + " | " + artist + " | Album: " + info.album + " | Plays: " + playcount + " | Tags: " + info.tags + " | Album Image: " + info.albumImage);

                    try (Connection conn = Database.connect()){
                        PreparedStatement stmt = conn.prepareStatement("INSERT INTO songs (track_name, artist_name, album_name, playcount, tags, album_image, source_tag) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                        stmt.setString(1, name);
                        stmt.setString(2, artist);
                        stmt.setString(3, info.album);
                        stmt.setLong(4, playcount.equals("N/A") ? 0 : Long.parseLong(playcount));
                        stmt.setString(5, info.tags);
                        stmt.setString(6, info.albumImage);
                        stmt.setString(7, tag);
                        stmt.executeUpdate();

                        ResultSet rs = stmt.getGeneratedKeys();
                        int songId = -1;
                        if (rs.next()){
                            songId = rs.getInt(1);
                        }

                        if (songId != -1 && !info.tags.equals("N/A")){
                            String[] tagList = info.tags.split(",\\s*");
                            for (String t : tagList) {
                                PreparedStatement tagStmt = conn.prepareStatement(
                                        "INSERT INTO tags (song_id, tag_name) VALUES (?, ?)"
                                );
                                tagStmt.setInt(1, songId);
                                tagStmt.setString(2, t.trim());
                                tagStmt.executeUpdate();
                            }
                        }
                    } catch (Exception e){
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

            TrackInfo(String album, String tags, String albumImage) {
                this.album = album;
                this.tags = tags;
                this.albumImage = albumImage;

            }
        }

        private static TrackInfo fetchTrackInfo (String artist, String track) throws IOException {
            String url = "https://ws.audioscrobbler.com/2.0/?method=track.getInfo&api_key=" + API_KEY +
                    "&artist=" + URLEncoder.encode(artist, StandardCharsets.UTF_8) +
                    "&track=" + URLEncoder.encode(track, StandardCharsets.UTF_8) +
                    "&format=json";

            Request request = new Request.Builder().url(url).build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return new TrackInfo("N/A", "N/A", "N/A");
                }

                String json = response.body().string();
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                String album = "(N/A)";
                StringBuilder tagsBuilder = new StringBuilder();
                String albumImage = "(N/A)";

                if (root.has("track")) {
                    JsonObject trackObj = root.getAsJsonObject("track");

                    if (trackObj.has("album")) {
                        JsonObject albumObj = trackObj.getAsJsonObject("album");
                        if (albumObj.has("title")) {
                            album = albumObj.get("title").getAsString();
                        }
                        if (albumObj.has("image")) {
                            JsonArray images = albumObj.getAsJsonArray("image");
                            for (JsonElement imgElem : images) {
                                JsonObject imgObj = imgElem.getAsJsonObject();
                                if (imgObj.has("size") && imgObj.get("size").getAsString().equals("extralarge")) {
                                    albumImage = imgObj.get("#text").getAsString();
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
                }

                return new TrackInfo(album, tagsBuilder.length() > 0 ? tagsBuilder.toString() : "N/A", albumImage);
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
}