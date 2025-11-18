package org.example;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Recommender {
    private static final double LIKE_BASE_WEIGHT = 1.0;
    private static final double EXCLUDE_BASE_WEIGHT = -2.0;
    private static final double FEEDBACK_HALFLIFE_DAYS = 60.0;

    private static final double MIN_IDF = 0.2;
    private static final double MAX_IDF = 2.5;

    public static class Rec {
        public final int songId;
        public final String track;
        public final String artist;
        public final double score;
        public final String tags;
        public final long playcount;

        public Rec(int songId, String track, String artist, double score, String tags, long playcount) {
            this.songId = songId;
            this.track = track;
            this.artist = artist;
            this.score = score;
            this.tags = tags;
            this.playcount = playcount;
        }
    }

    public static List<Rec> recommendHybrid(Connection conn, int userId, int limit) throws Exception {
        if (count(conn, "SELECT COUNT(*) FROM user_likes WHERE user_id = " + userId) == 0) {
            return topFromSongsByPlaycount(conn, limit);
        }

        int totalSongs = count(conn, "SELECT COUNT(*) FROM songs");

        double maxLogPlaycount = computeMaxLogPlaycount(conn);

        Map<String, Integer> df = new HashMap<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT tag_name, COUNT(DISTINCT song_id) df FROM tags GROUP BY tag_name")) {
            while (rs.next()) df.put(rs.getString(1).toLowerCase(), rs.getInt(2));
        }

       //+1 per like, -2 per exclusion
        Map<String, Double> user = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT t.tag_name, ul.created_at " +
                        "FROM user_likes ul JOIN tags t ON t.song_id = ul.song_id " +
                        "WHERE ul.user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rawTag = rs.getString(1);
                    if (rawTag == null) continue;
                    String tag = rawTag.trim().toLowerCase();
                    if (tag.isEmpty()) continue;

                    Timestamp ts = rs.getTimestamp(2);
                    double decay = computeTimeDecay(ts);
                    double w = LIKE_BASE_WEIGHT * decay;
                    user.merge(tag, w, Double::sum);
                }
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT t.tag_name, ux.created_at " +
                        "FROM user_exclusions ux JOIN tags t ON t.song_id = ux.song_id " +
                        "WHERE ux.user_id = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String rawTag = rs.getString(1);
                    if (rawTag == null) continue;
                    String tag = rawTag.trim().toLowerCase();
                    if (tag.isEmpty()) continue;

                    Timestamp ts = rs.getTimestamp(2);
                    double decay = computeTimeDecay(ts);
                    double w = EXCLUDE_BASE_WEIGHT * decay;
                    user.merge(tag, w, Double::sum);
                }
            }
        }

        if (user.isEmpty()) return topFromSongsByPlaycount(conn, limit); // edge case

        Map<String, Double> userTfidf = new HashMap<>();
        for (var e : user.entrySet()) {
            int d = Math.max(1, df.getOrDefault(e.getKey(), 1));
            double idf = Math.log(1.0 + (double) totalSongs / d);

            if (idf < MIN_IDF) idf = MIN_IDF;
            if (idf > MAX_IDF) idf = MAX_IDF;

            userTfidf.put(e.getKey(), e.getValue() * idf);
        }
        double userNorm = Math.sqrt(userTfidf.values().stream().mapToDouble(x -> x * x).sum());
        if (userNorm == 0) userNorm = 1;

        var userTags = userTfidf.keySet().stream().limit(100).toList();
        String placeholders = userTags.stream().map(t -> "?").collect(Collectors.joining(","));
        String sql =
                "SELECT s.id, s.track_name, s.artist_name, s.playcount, " +
                        "       GROUP_CONCAT(DISTINCT t.tag_name) AS tags " +
                        "FROM songs s JOIN tags t ON t.song_id = s.id " +
                        "WHERE t.tag_name IN (" + placeholders + ") " +
                        "AND s.id NOT IN (SELECT song_id FROM user_likes WHERE user_id = ?) " +
                        "AND s.id NOT IN (SELECT song_id FROM user_exclusions WHERE user_id = ?) " +
                        "GROUP BY s.id, s.track_name, s.artist_name, s.playcount";

        if (userTags.isEmpty()) return topFromSongsByPlaycount(conn, limit);

        List<Rec> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (String tag : userTags)ps.setString(i++, tag);
            ps.setInt(i++, userId);
            ps.setInt(i, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String track = rs.getString("track_name");
                    String artist = rs.getString("artist_name");
                    long plays = rs.getLong("playcount");
                    String tagCsv = rs.getString("tags");

                    Set<String> songTags = new HashSet<>();
                    if(tagCsv != null) {
                        for(String t : tagCsv.split(",")) {
                            String tt = t.trim().toLowerCase();
                            if(!tt.isEmpty()) songTags.add(tt);
                        }
                    }

                    double dot = 0.0, songNorm = Math.sqrt(Math.max(1, songTags.size()));
                    for (String t : songTags) dot += userTfidf.getOrDefault(t, 0.0);
                    double cos = (userNorm == 0 || songNorm == 0) ? 0.0 : (dot/ (userNorm * songNorm));

                    double pop = 0.0;
                    if (maxLogPlaycount > 0.0) {
                        double raw = Math.log(1.0 + Math.max(0L, plays));
                        pop = raw / maxLogPlaycount;
                    }

                    double fresh = fetchFreshness(conn, id);

                    double alpha = 0.75, beta = 0.18, gamma = 0.00; // gamma set to 0.00 because I don't need freshness right now, will have to parametrize later idk
                    double score = alpha * cos + beta * pop + gamma * fresh;

                    out.add(new Rec(id, track, artist, score, tagCsv, plays));
                }
            }
        }
        Map<String, Integer> seen = new HashMap<>();
        out.sort((a, b) -> Double.compare(b.score, a.score));
        double dupPenalty = 0.05;
        List<Rec> finalList = new ArrayList<>();
        for (Rec r : out) {
            int k = seen.getOrDefault(r.artist.toLowerCase(), 0);
            finalList.add(new Rec(r.songId, r.track, r.artist, r.score - dupPenalty * k, r.tags, r.playcount));
            seen.put(r.artist.toLowerCase(), k + 1);
        }
        finalList.sort((a, b) -> Double.compare(b.score, a.score));
        return finalList.stream().limit(limit).toList();
    }

    private static int count(Connection conn, String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next(); return rs.getInt(1);
        }
    }

    private static List<Rec> topFromSongsByPlaycount(Connection conn, int limit) throws SQLException {
        List<Rec> list = new ArrayList<>();
        String sql =
                "SELECT s.id, s.track_name, s.artist_name, s.playcount, " +
                        "       GROUP_CONCAT(DISTINCT t.tag_name) AS tags " +
                        "FROM songs s " +
                        "LEFT JOIN tags t ON t.song_id = s.id " +
                        "GROUP BY s.id, s.track_name, s.artist_name, s.playcount " +
                        "ORDER BY s.playcount DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Rec(
                            rs.getInt("id"),
                            rs.getString("track_name"),
                            rs.getString("artist_name"),
                            0.0,                               // score is N/A for cold start
                            rs.getString("tags"),
                            rs.getLong("playcount")
                    ));
                }
            }
        }
        return list;
    }

    private static double fetchFreshness(Connection conn, int songId) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT created_at FROM songs WHERE id = ?")) {
            ps.setInt(1, songId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) {
                        long days = Math.max(0, Duration.between(ts.toInstant(), Instant.now()).toDays());
                        // 0 days -> 1.0; ~2-week half-life
                        return Math.exp(-days / 14.0);
                    }
                }
            }
        } catch (SQLException ignoreIfNoColumn) {}
        return 0.0;
    }

    private static double computeMaxLogPlaycount(Connection conn) throws SQLException {
        String sql = "SELECT MAX(playcount) FROM songs";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                long maxPlays = rs.getLong(1);
                if (maxPlays <= 0L) return 0.0;
                return Math.log(1.0 + maxPlays);
            }
        }
        return 0.0;
    }

    private static double computeTimeDecay(Timestamp ts) {
        if(ts == null) return 1.0;
        long days = Math.max(0, Duration.between(ts.toInstant(), Instant.now()).toDays());
        if(FEEDBACK_HALFLIFE_DAYS <= 0) return 1.0;
        return Math.pow(0.5, days / FEEDBACK_HALFLIFE_DAYS);
    }

}
