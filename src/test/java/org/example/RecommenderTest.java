package org.example;

import org.junit.jupiter.api.Test;

import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RecommenderTest {

    private Connection createConnection(String dbName) throws SQLException {
        String url = "jdbc:h2:mem:" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
        return DriverManager.getConnection(url, "sa", "");
    }

    private void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE songs (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    track_name   VARCHAR(255),
                    artist_name  VARCHAR(255),
                    album_name   VARCHAR(255),
                    playcount    BIGINT,
                    tags         VARCHAR(1000),
                    album_image  VARCHAR(500),
                    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
            st.execute("""
                CREATE TABLE tags (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    song_id INT,
                    tag_name VARCHAR(255)
                );
            """);
            st.execute("""
                CREATE TABLE user_likes (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT,
                    song_id INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
            st.execute("""
                CREATE TABLE user_exclusions (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT,
                    song_id INT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);
        }
    }

    private int insertSong(Connection conn, String track, String artist, String album,
                           long playcount, String tagsCsv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO songs (track_name, artist_name, album_name, playcount, tags) " +
                        "VALUES (?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, track);
            ps.setString(2, artist);
            ps.setString(3, album);
            ps.setLong(4, playcount);
            ps.setString(5, tagsCsv);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert song");
    }

    private void insertTag(Connection conn, int songId, String tag) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tags (song_id, tag_name) VALUES (?, ?)")) {
            ps.setInt(1, songId);
            ps.setString(2, tag);
            ps.executeUpdate();
        }
    }

    private void insertLike(Connection conn, int userId, int songId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO user_likes (user_id, song_id) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        }
    }

    private void insertExclusion(Connection conn, int userId, int songId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO user_exclusions (user_id, song_id) VALUES (?, ?)")) {
            ps.setInt(1, userId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        }
    }

    @Test
    void coldStart_usesTopPlaycountWhenNoLikes() throws Exception {
        try (Connection conn = createConnection("coldStart")) {
            initSchema(conn);

            int idA = insertSong(conn, "Song A", "Artist 1", "Album", 1000, "rock");
            int idB = insertSong(conn, "Song B", "Artist 2", "Album", 5000, "pop");
            int idC = insertSong(conn, "Song C", "Artist 3", "Album", 3000, "jazz");

            insertTag(conn, idA, "rock");
            insertTag(conn, idB, "pop");
            insertTag(conn, idC, "jazz");

            List<Recommender.Rec> recs = Recommender.recommendHybrid(conn, 1, 3);

            assertEquals(3, recs.size(), "Should return 3 recommendations in cold start");
            assertEquals("Song B", recs.get(0).track, "Highest playcount first");
            assertEquals("Song C", recs.get(1).track);
            assertEquals("Song A", recs.get(2).track);
        }
    }

    @Test
    void likedSongsAreExcluded_butSimilarUnlikedSongsAreRecommended() throws Exception {
        try (Connection conn = createConnection("similarUnliked")) {
            initSchema(conn);

            int rockLiked = insertSong(conn, "Rock Liked", "Band A", "Alb", 2000, "rock");
            int rockNew   = insertSong(conn, "Rock New",   "Band B", "Alb", 1800, "rock");
            int pop1      = insertSong(conn, "Pop 1",      "Artist P", "Alb", 2200, "pop");

            insertTag(conn, rockLiked, "rock");
            insertTag(conn, rockNew,   "rock");
            insertTag(conn, pop1,      "pop");

            insertLike(conn, 1, rockLiked);

            List<Recommender.Rec> recs = Recommender.recommendHybrid(conn, 1, 5);

            boolean containsLiked = recs.stream().anyMatch(r -> "Rock Liked".equals(r.track));
            assertFalse(containsLiked, "Already liked song should not appear in recommendations");

            boolean containsNewRock = recs.stream().anyMatch(r -> "Rock New".equals(r.track));
            assertTrue(containsNewRock, "Similar unliked rock song should be recommended");
        }
    }

    @Test
    void exclusions_removeSongsFromResults() throws Exception {
        try (Connection conn = createConnection("exclusions")) {
            initSchema(conn);

            int rockFav   = insertSong(conn, "Rock Fav",      "Band A", "Alb", 2000, "rock");
            int popAnnoy  = insertSong(conn, "Pop Annoying",  "Artist P", "Alb", 2100, "pop");
            int rockNew   = insertSong(conn, "Rock New",      "Band B", "Alb", 1900, "rock");

            insertTag(conn, rockFav,  "rock");
            insertTag(conn, popAnnoy, "pop");
            insertTag(conn, rockNew,  "rock");

            insertLike(conn, 1, rockFav);
            insertLike(conn, 1, popAnnoy);

            insertExclusion(conn, 1, popAnnoy);

            List<Recommender.Rec> recs = Recommender.recommendHybrid(conn, 1, 10);

            boolean containsExcluded = recs.stream().anyMatch(r -> "Pop Annoying".equals(r.track));
            assertFalse(containsExcluded, "Excluded songs must not appear in recommendations");

            for (Recommender.Rec r : recs) {
                assertNotEquals("Pop Annoying", r.track, "Excluded track should never be recommended");
            }
        }
    }
}
