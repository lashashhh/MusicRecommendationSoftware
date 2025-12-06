package org.example;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.sql.*;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;
import javafx.scene.layout.Region;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;


public class HomeScreen {

    private final String username;

    private enum ViewMode {
        LIST,
        GRID
    }

    public HomeScreen (String username) {
        this.username = username;
    }

    public void start(Stage stage){
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        VBox sidebar = new VBox(20);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(30, 15, 30, 15));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: #181818");

        Label title = new Label("Music Recommender");
        title.setStyle("-fx-font-size: 16px; -fx-font-fill: #1db954");

        Button libraryBtn = new Button("Your Library");
        Button discoverBtn = new Button("Discover New Music");
        Button statsBtn = new Button("Your Statistics");
        Button settingsBtn = new Button("Settings / Logout");

        libraryBtn.getStyleClass().add("button-secondary");
        discoverBtn.getStyleClass().add("button-secondary");
        statsBtn.getStyleClass().add("button-secondary");
        settingsBtn.getStyleClass().add("button-secondary");

        sidebar.getChildren().addAll(title, libraryBtn, discoverBtn, statsBtn, settingsBtn);

        VBox contentArea = new VBox(15);
        contentArea.setAlignment(Pos.TOP_CENTER);
        contentArea.setPadding(new Insets(40));
        contentArea.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        Label welcomeLabel = new Label("Welcome " + username + "!");
        welcomeLabel.getStyleClass().add("title-label");
        contentArea.getChildren().add(welcomeLabel);

        root.setLeft(sidebar);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 550);

        var css = getClass().getResource("/org/example/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
            System.out.println("[GUI_STYLE] Dashboard CSS loaded.");
        } else {
            System.err.println("[GUI_STYLE] Dashboard CSS not found.");
        }

        stage.setScene(scene);
        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2.2), welcomeLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
//        fadeIn.setOnFinished(e -> showDiscover(contentArea));
        fadeIn.play();

        discoverBtn.setOnAction(e -> {
            highlightBtn(discoverBtn, libraryBtn, statsBtn, settingsBtn);
            showDiscover(contentArea);
        });
        libraryBtn.setOnAction(e -> {
            highlightBtn(libraryBtn, discoverBtn, statsBtn, settingsBtn);
            showLibrary(contentArea);
        });
        statsBtn.setOnAction(e -> {
            highlightBtn(statsBtn, libraryBtn, discoverBtn, settingsBtn);
            showStats(contentArea);
        });
        settingsBtn.setOnAction(e -> {
            highlightBtn(settingsBtn, libraryBtn, statsBtn, discoverBtn);
            showSettings(contentArea, stage);
        });
    }

    private void showDiscover(VBox content) {
        content.getChildren().clear();

        Label title = new Label("Discover New Music");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab forYouTab = new Tab("For You");
        forYouTab.setClosable(false);
        BorderPane forYouRoot = new BorderPane();
        forYouTab.setContent(forYouRoot);

        ToggleGroup fyViewGroup = new ToggleGroup();
        ToggleButton fyListBtn = new ToggleButton("List");
        ToggleButton fyGridBtn = new ToggleButton("Grid");
        fyListBtn.setToggleGroup(fyViewGroup);
        fyGridBtn.setToggleGroup(fyViewGroup);
        fyListBtn.setSelected(true);
        fyListBtn.getStyleClass().add("button-secondary");
        fyGridBtn.getStyleClass().add("button-secondary");

        Button fyRefreshBtn = new Button("Refresh");
        fyRefreshBtn.getStyleClass().add("button-secondary");

        Label viewLabel = new Label("View:");
        viewLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Region fySpacer = new Region();
        HBox.setHgrow(fySpacer, Priority.ALWAYS);

        HBox viewRow = new HBox(10, viewLabel, fyListBtn, fyGridBtn, fySpacer, fyRefreshBtn);
        viewRow.setAlignment(Pos.CENTER_LEFT);
        viewRow.setPadding(new Insets(10, 0, 6, 0));

        Label tagsLabel = new Label("Tags:");
        tagsLabel.setStyle("-fx-text-fill: #b3b3b3;");
        Label includeLabel = new Label("Include:");
        includeLabel.setStyle("-fx-text-fill: #b3b3b3;");
        TextField includeField = new TextField();
        includeField.setPromptText("Include tags (e.g. rock, indie)");
        includeField.setPrefWidth(260);

        Label excludeLabel = new Label("Exclude:");
        excludeLabel.setStyle("-fx-text-fill: #b3b3b3;");
        TextField excludeField = new TextField();
        excludeField.setPromptText("Exclude tags (e.g. metal, rap)");
        excludeField.setPrefWidth(260);

        Button applyFilterBtn = new Button("Apply");
        applyFilterBtn.getStyleClass().add("button-secondary");
        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.getStyleClass().add("button-secondary");

        HBox filterRow = new HBox(
                6,
                tagsLabel,
                includeLabel, includeField,
                excludeLabel, excludeField,
                applyFilterBtn, clearFilterBtn
        );
        filterRow.setAlignment(Pos.CENTER_LEFT);
        filterRow.setPadding(new Insets(0, 0, 10, 0));

        VBox fyTopBox = new VBox(4, viewRow, filterRow);
        forYouRoot.setTop(fyTopBox);

        ScrollPane fyScroll = new ScrollPane();
        fyScroll.setFitToWidth(true);
        fyScroll.setFitToHeight(true);
        fyScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        fyScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        fyScroll.setStyle("-fx-background-color: transparent;");
        forYouRoot.setCenter(fyScroll);

        List<DiscoverEntry> baseForYouEntries = new ArrayList<>();
        List<DiscoverEntry> filteredForYouEntries = new ArrayList<>();
        Set<Integer> forYouLiked = new HashSet<>();
        Set<Integer> forYouExcluded = new HashSet<>();
        final ViewMode[] forYouViewMode = { ViewMode.LIST };

        final Runnable[] renderForYouRef = new Runnable[1];
        final Runnable[] loadForYouRef = new Runnable[1];

        Runnable applyFilters = () -> {
            filteredForYouEntries.clear();

            String incRaw = includeField.getText() == null ? "" : includeField.getText().toLowerCase();
            String excRaw = excludeField.getText() == null ? "" : excludeField.getText().toLowerCase();

            Set<String> incTags = new HashSet<>();
            Set<String> excTags = new HashSet<>();

            for (String token : incRaw.split(",")) {
                String t = token.trim();
                if (!t.isEmpty()) incTags.add(t);
            }
            for (String token : excRaw.split(",")) {
                String t = token.trim();
                if (!t.isEmpty()) excTags.add(t);
            }

            for (DiscoverEntry e : baseForYouEntries) {
                if (e.tagsCsv == null || e.tagsCsv.isBlank()) {
                    if (incTags.isEmpty() && excTags.isEmpty()) {
                        filteredForYouEntries.add(e);
                    }
                    continue;
                }

                Set<String> songTags = new HashSet<>();
                for (String t : e.tagsCsv.split(",")) {
                    String tt = t.trim().toLowerCase();
                    if (!tt.isEmpty()) songTags.add(tt);
                }

                if (!incTags.isEmpty()) {
                    boolean anyMatch = false;
                    for (String want : incTags) {
                        for (String st : songTags) {
                            if (st.contains(want)) {
                                anyMatch = true;
                                break;
                            }
                        }
                        if (anyMatch) break;
                    }
                    if (!anyMatch) continue;
                }

                if (!excTags.isEmpty()) {
                    boolean blocked = false;
                    for (String ban : excTags) {
                        for (String st : songTags) {
                            if (st.contains(ban)) {
                                blocked = true;
                                break;
                            }
                        }
                        if (blocked) break;
                    }
                    if (blocked) continue;
                }

                filteredForYouEntries.add(e);
            }

            if (renderForYouRef[0] != null) {
                renderForYouRef[0].run();
            }
        };

        renderForYouRef[0] = () -> {
            if (filteredForYouEntries.isEmpty()) {
                Label empty = new Label("Like a few songs so we can recommend music for you.");
                empty.setStyle("-fx-text-fill: #b3b3b3");
                fyScroll.setContent(empty);
                return;
            }
            Node view = (forYouViewMode[0] == ViewMode.LIST)
                    ? buildDiscoverListView(filteredForYouEntries, forYouLiked, forYouExcluded)
                    : buildDiscoverGridView(filteredForYouEntries, forYouLiked, forYouExcluded);
            fyScroll.setContent(view);
        };

        fyViewGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == fyGridBtn) {
                forYouViewMode[0] = ViewMode.GRID;
            } else {
                forYouViewMode[0] = ViewMode.LIST;
            }
            renderForYouRef[0].run();
        });

        applyFilterBtn.setOnAction(e -> {
            if (loadForYouRef[0] != null) {
                loadForYouRef[0].run();
            }
        });
        clearFilterBtn.setOnAction(e -> {
            includeField.clear();
            excludeField.clear();
            if (loadForYouRef[0] != null) {
                loadForYouRef[0].run();
            }
        });

        loadForYouRef[0] = () -> {
            fyScroll.setContent(new Label("Loading personalized recommendations..."));

            new Thread(() -> {
                try (Connection conn = Database.connect()) {
                    int userId = loginSystem.getCurrentUserId();

                    // populate liked/excluded states
                    forYouLiked.clear();
                    forYouExcluded.clear();
                    loadUserLibrary(conn, userId, forYouLiked, forYouExcluded);

                    // --- parse include tags from the text field ---
                    String incTextRaw = includeField.getText() == null ? "" : includeField.getText().toLowerCase();
                    Set<String> incTags = new LinkedHashSet<>();
                    for (String token : incTextRaw.split(",")) {
                        String t = token.trim();
                        if (!t.isEmpty()) incTags.add(t);
                    }

                    // we don't need exclude tags here; applyFilters will enforce them later

                    // Ask recommender for more when include tags are present
                    int recLimit = incTags.isEmpty() ? 40 : 80;

                    List<Recommender.Rec> recs = Recommender.recommendHybrid(conn, userId, recLimit);

                    List<DiscoverEntry> loaded = new ArrayList<>();
                    Set<Integer> usedSongIds = new HashSet<>();

                    // Map recommender results to DiscoverEntry
                    try (PreparedStatement songPs = conn.prepareStatement(
                            "SELECT album_name, album_image, tags, playcount FROM songs WHERE id = ?")) {

                        for (Recommender.Rec r : recs) {
                            songPs.setInt(1, r.songId);

                            String album = "(N/A)";
                            String albumImg = "(N/A)";
                            String tagsCsv = r.tags;
                            long plays = r.playcount;

                            try (ResultSet rs = songPs.executeQuery()) {
                                if (rs.next()) {
                                    String a = rs.getString("album_name");
                                    if (a != null && !a.isBlank()) album = a;
                                    String img = rs.getString("album_image");
                                    if (img != null && !img.isBlank()) albumImg = img;

                                    if (tagsCsv == null || tagsCsv.isBlank()) {
                                        String dbTags = rs.getString("tags");
                                        if (dbTags != null && !dbTags.isBlank()) {
                                            tagsCsv = dbTags;
                                        }
                                    }
                                    long p = rs.getLong("playcount");
                                    if (plays <= 0 && p > 0) plays = p;
                                }
                            }

                            loaded.add(new DiscoverEntry(
                                    r.songId,
                                    r.track,
                                    r.artist,
                                    album,
                                    plays,
                                    tagsCsv,
                                    albumImg
                            ));
                            usedSongIds.add(r.songId);
                        }
                    }

                    // --- TAG-BASED FALLBACK: pull extra songs that match include tags ---
                    if (!incTags.isEmpty()) {
                        // Build WHERE clause: (LOWER(t.tag_name) LIKE ? OR ...)
                        StringBuilder where = new StringBuilder();
                        boolean first = true;
                        for (int i = 0; i < incTags.size(); i++) {
                            if (!first) where.append(" OR ");
                            where.append("LOWER(t.tag_name) LIKE ?");
                            first = false;
                        }

                        String sql =
                                "SELECT s.id, s.track_name, s.artist_name, s.album_name, s.playcount, s.tags, s.album_image " +
                                        "FROM songs s " +
                                        "JOIN tags t ON t.song_id = s.id " +
                                        "WHERE " + where + " " +
                                        "GROUP BY s.id, s.track_name, s.artist_name, s.album_name, s.playcount, s.tags, s.album_image " +
                                        "ORDER BY s.playcount DESC " +
                                        "LIMIT 200";

                        try (PreparedStatement ps = conn.prepareStatement(sql)) {
                            int idx = 1;
                            for (String tag : incTags) {
                                ps.setString(idx++, "%" + tag + "%"); // contains-match, like applyFilters
                            }

                            try (ResultSet rs = ps.executeQuery()) {
                                while (rs.next()) {
                                    int id = rs.getInt("id");

                                    // Skip if already used or user has liked/excluded it
                                    if (usedSongIds.contains(id)) continue;
                                    if (forYouLiked.contains(id) || forYouExcluded.contains(id)) continue;

                                    String trackName = rs.getString("track_name");
                                    String artistName = rs.getString("artist_name");
                                    String albumName = rs.getString("album_name");
                                    long playcount = rs.getLong("playcount");
                                    String tagsCsv = rs.getString("tags");
                                    String albumImg = rs.getString("album_image");

                                    if (albumName == null || albumName.isBlank()) albumName = "(N/A)";
                                    if (albumImg == null || albumImg.isBlank()) albumImg = "(N/A)";

                                    loaded.add(new DiscoverEntry(
                                            id,
                                            trackName,
                                            artistName,
                                            albumName,
                                            playcount,
                                            tagsCsv,
                                            albumImg
                                    ));
                                    usedSongIds.add(id);

                                    // Keep list from exploding; 80 personalized + up to ~120 tag-based
                                    if (loaded.size() >= 200) break;
                                }
                            }
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        baseForYouEntries.clear();
                        baseForYouEntries.addAll(loaded);
                        // Apply include/exclude filters on this enlarged set
                        applyFilters.run();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        Label error = new Label("Error loading recommendations: " + ex.getMessage());
                        error.setStyle("-fx-text-fill: red;");
                        fyScroll.setContent(error);
                    });
                }
            }, "for-you-thread").start();
        };


        fyRefreshBtn.setOnAction(e -> {
            if (loadForYouRef[0] != null) loadForYouRef[0].run();
        });
        if (loadForYouRef[0] != null) loadForYouRef[0].run();

        Tab weeklyTab = new Tab("Top 20 (This Week)");
        weeklyTab.setClosable(false);
        BorderPane weeklyRoot = new BorderPane();
        weeklyTab.setContent(weeklyRoot);

        ToggleGroup wkViewGroup = new ToggleGroup();
        ToggleButton wkListBtn = new ToggleButton("List");
        ToggleButton wkGridBtn = new ToggleButton("Grid");
        wkListBtn.setToggleGroup(wkViewGroup);
        wkGridBtn.setToggleGroup(wkViewGroup);
        wkListBtn.setSelected(true);
        wkListBtn.getStyleClass().add("button-secondary");
        wkGridBtn.getStyleClass().add("button-secondary");

        Button refreshTopBtn = new Button("Refresh Top 20");
        refreshTopBtn.getStyleClass().add("button-secondary");

        Label wkViewLabel = new Label("View:");
        wkViewLabel.setStyle("-fx-text-fill: #b3b3b3;");

        Region wkSpacer = new Region();
        HBox.setHgrow(wkSpacer, Priority.ALWAYS);

        HBox wkControls = new HBox(10, wkViewLabel, wkListBtn, wkGridBtn, wkSpacer, refreshTopBtn);
        wkControls.setAlignment(Pos.CENTER_LEFT);
        wkControls.setPadding(new Insets(10, 0, 10, 0));
        weeklyRoot.setTop(wkControls);

        ScrollPane wkScroll = new ScrollPane();
        wkScroll.setFitToWidth(true);
        wkScroll.setFitToHeight(true);
        wkScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        wkScroll.setStyle("-fx-background-color: transparent;");
        weeklyRoot.setCenter(wkScroll);

        List<DiscoverEntry> weeklyEntries = new ArrayList<>();
        Set<Integer> weeklyLiked = new HashSet<>();
        Set<Integer> weeklyExcluded = new HashSet<>();
        final ViewMode[] weeklyViewMode = { ViewMode.LIST };

        Runnable renderWeekly = () -> {
            if (weeklyEntries.isEmpty()) {
                Label empty = new Label("Top tracks are not available yet.");
                empty.setStyle("-fx-text-fill: #b3b3b3");
                wkScroll.setContent(empty);
                return;
            }
            Node view = (weeklyViewMode[0] == ViewMode.LIST)
                    ? buildDiscoverListView(weeklyEntries, weeklyLiked, weeklyExcluded)
                    : buildDiscoverGridView(weeklyEntries, weeklyLiked, weeklyExcluded);
            wkScroll.setContent(view);
        };
        wkViewGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == wkGridBtn) {
                weeklyViewMode[0] = ViewMode.GRID;
            } else {
                weeklyViewMode[0] = ViewMode.LIST;
            }
            renderWeekly.run();
        });
        Runnable loadWeekly = () -> {
            wkScroll.setContent(new Label("Loading Top 20 tracks..."));

            new Thread(() -> {
                try (Connection conn = Database.connect()) {
                    int userId = loginSystem.getCurrentUserId();
                    weeklyLiked.clear();
                    weeklyExcluded.clear();
                    loadUserLibrary(conn, userId, weeklyLiked, weeklyExcluded);

                    List<DiscoverEntry> loaded = new ArrayList<>();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT id, track_name, artist_name, album_name, playcount, tags, album_image " +
                                    "FROM weekly_top_cache ORDER BY playcount DESC LIMIT 20")) {

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int cacheId = rs.getInt("id");
                                String wTrack = rs.getString("track_name");
                                String wArtist = rs.getString("artist_name");
                                String wAlbum = rs.getString("album_name");
                                long wPlays = rs.getLong("playcount");
                                String wTags = rs.getString("tags");
                                String wAlbumImg = rs.getString("album_image");

                                int songId;
                                try (PreparedStatement psSong = conn.prepareStatement(
                                        "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1")) {
                                    psSong.setString(1, wTrack);
                                    psSong.setString(2, wArtist);
                                    try (ResultSet rsSong = psSong.executeQuery()) {
                                        if (rsSong.next()) {
                                            songId = rsSong.getInt("id");
                                        } else {
                                            try (PreparedStatement ins = conn.prepareStatement(
                                                    "INSERT IGNORE INTO songs (track_name, artist_name, album_name, playcount, tags, album_image) " +
                                                            "VALUES (?, ?, ?, ?, ?, ?)",
                                                    Statement.RETURN_GENERATED_KEYS)) {
                                                ins.setString(1, wTrack);
                                                ins.setString(2, wArtist);
                                                ins.setString(3, wAlbum);
                                                ins.setLong(4, wPlays);
                                                ins.setString(5, wTags);
                                                ins.setString(6, wAlbumImg);
                                                ins.executeUpdate();

                                                try (ResultSet gen = ins.getGeneratedKeys()) {
                                                    if (gen.next()) {
                                                        songId = gen.getInt(1);
                                                    } else {
                                                        try (PreparedStatement ps2 = conn.prepareStatement(
                                                                "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1")) {
                                                            ps2.setString(1, wTrack);
                                                            ps2.setString(2, wArtist);
                                                            try (ResultSet rs2 = ps2.executeQuery()) {
                                                                if (rs2.next()) {
                                                                    songId = rs2.getInt("id");
                                                                } else {
                                                                    songId = 0;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                loaded.add(new DiscoverEntry(
                                        songId,
                                        wTrack,
                                        wArtist,
                                        wAlbum,
                                        wPlays,
                                        wTags,
                                        wAlbumImg
                                ));
                            }
                        }
                    }
                    javafx.application.Platform.runLater(() -> {
                        weeklyEntries.clear();
                        weeklyEntries.addAll(loaded);
                        renderWeekly.run();
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        Label error = new Label("Error loading Top 20: " + ex.getMessage());
                        error.setStyle("-fx-text-fill: red;");
                        wkScroll.setContent(error);
                    });
                }
            }, "weekly-top-thread").start();
        };

        refreshTopBtn.setOnAction(e -> loadWeekly.run());
        loadWeekly.run();

        tabPane.getTabs().addAll(forYouTab, weeklyTab);

        content.getChildren().addAll(title, tabPane);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        animateContent(content);
    }


    private void showSongPopup(String artist, String track, String album, String tags, long plays, String albumImg) {
        Stage popup = new Stage();
        popup.setTitle("Track Details");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #181818; -fx-background-radius: 15;");

        final int currentUserId = loginSystem.getCurrentUserId();
        final int[] songIdHolder = { -1 };
        final boolean[] likedState = { false };
        final boolean[] excludedState = { false };

        try (Connection conn = Database.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1"
            )) {
                ps.setString(1, track);
                ps.setString(2, artist);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        songIdHolder[0] = rs.getInt("id");
                    }
                }
            }
            int sId = songIdHolder[0];
            if (sId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM user_likes WHERE user_id = ? AND song_id = ? LIMIT 1")) {
                    ps.setInt(1, currentUserId);
                    ps.setInt(2, sId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            likedState[0] = true;
                        }
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT 1 FROM user_exclusions WHERE user_id = ? AND song_id = ? LIMIT 1")) {
                    ps.setInt(1, currentUserId);
                    ps.setInt(2, sId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            excludedState[0] = true;
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        final int songId = songIdHolder[0];


        ImageView imgView = new ImageView();
        if (albumImg != null && !albumImg.isBlank() && !albumImg.equals("(N/A)") && albumImg.startsWith("http")) {
            try {
                String safeUrl = albumImg.replace("http://", "https://");
                Image img = new Image(safeUrl, 200, 200, true, true);
                img.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) System.err.println("[IMG_LOAD_ERROR] " + safeUrl);
                });
                imgView.setImage(img);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        imgView.setFitWidth(200);
        imgView.setFitHeight(200);
        imgView.setStyle("-fx-background-color: #333; -fx-background-radius: 15;");

        Tooltip genreTooltip = new Tooltip(
                (tags != null && !tags.isBlank()) ? "Genres: " + tags : "No tags available"
        );
        genreTooltip.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #1db954; -fx-font-size: 12px;");
        Tooltip.install(imgView, genreTooltip);

        Label trackLabel = new Label(track);
        trackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold");
        Label artistLabel = new Label("Artist: " + artist);
        artistLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label albumLabel = new Label("Album: " + album);
        albumLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label playLabel = new Label("Playcount: " + plays);
        playLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label tagLabel = new Label("Tags: " + (tags == null || tags.isBlank() ? "N/A" : tags));
        tagLabel.setStyle("-fx-text-fill: #1db954; -fx-font-size: 13px");

        Button likeBtn = new Button("♥ Like");
        likeBtn.getStyleClass().add("button-secondary");
        Button excludeBtn = new Button("✖ Exclude");
        excludeBtn.getStyleClass().add("button-secondary");

        if (likedState[0]) {
            likeBtn.setText("✔ Liked");
            excludeBtn.setText("✖ Exclude");
        } else if (excludedState[0]) {
            likeBtn.setText("♥ Like");
            excludeBtn.setText("✔ Excluded");
        }
        likeBtn.setOnAction(e -> {
            if (songId <= 0) {
                likeBtn.setText("Error");
                return;
            }

            try (Connection conn = Database.connect()) {
                if (!likedState[0]) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT IGNORE INTO user_likes (user_id, song_id) VALUES (?, ?)")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM user_exclusions WHERE user_id = ? AND song_id = ?")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    likedState[0] = true;
                    excludedState[0] = false;

                    likeBtn.setText("✔ Liked");
                    excludeBtn.setText("✖ Exclude");
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM user_likes WHERE user_id = ? AND song_id = ?")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    likedState[0] = false;
                    likeBtn.setText("♥ Like");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                likeBtn.setText("Error");
            }
        });
        excludeBtn.setOnAction(e -> {
            if (songId <= 0) {
                excludeBtn.setText("Error");
                return;
            }
            try (Connection conn = Database.connect()) {
                if (!excludedState[0]) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT IGNORE INTO user_exclusions (user_id, song_id) VALUES (?, ?)")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM user_likes WHERE user_id = ? AND song_id = ?")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    excludedState[0] = true;
                    likedState[0] = false;

                    excludeBtn.setText("✔ Excluded");
                    likeBtn.setText("♥ Like");
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM user_exclusions WHERE user_id = ? AND song_id = ?")) {
                        ps.setInt(1, currentUserId);
                        ps.setInt(2, songId);
                        ps.executeUpdate();
                    }
                    excludedState[0] = false;
                    excludeBtn.setText("✖ Exclude");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                excludeBtn.setText("Error");
            }
        });


        Label commentsTitle = new Label("Comments");
        commentsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold");

        VBox commentsList = new VBox(8);
        commentsList.setFillWidth(true);

        ScrollPane commentsScroll = new ScrollPane(commentsList);
        commentsScroll.setFitToWidth(true);
        commentsScroll.setPrefViewportHeight(230);
        commentsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        commentsScroll.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");

        TextArea commentInput = new TextArea();
        commentInput.setPromptText("Write a comment...");
        commentInput.setWrapText(true);
        commentInput.setPrefRowCount(2);

        Button postCommentBtn = new Button("Post");
        postCommentBtn.getStyleClass().add("button-primary");
        postCommentBtn.setMinWidth(100);
        postCommentBtn.setPrefWidth(100);
        postCommentBtn.setMaxWidth(100);

        Label commentFeedback = new Label();
        commentFeedback.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11px;");

        Label replyContextLabel = new Label();
        replyContextLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11px;");

        final int[] replyingToCommentId = { -1 };

        HBox commentInputRow = new HBox(10, commentInput, postCommentBtn);
        commentInputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(commentInput, Priority.ALWAYS);

        class CommentRow {
            final int id;
            final int userId;
            final String username;
            final String text;
            final Timestamp createdAt;
            final Integer parentId;

            CommentRow(int id, int userId, String username, String text, Timestamp createdAt, Integer parentId) {
                this.id = id;
                this.userId = userId;
                this.username = username;
                this.text = text;
                this.createdAt = createdAt;
                this.parentId = parentId;
            }
        }

        final Runnable[] refreshComments = new Runnable[1];

        refreshComments[0] = () -> {
            if (songId <= 0) {
                javafx.application.Platform.runLater(() -> {
                    commentsList.getChildren().clear();
                    Label noSong = new Label("Cannot load comments for this track.");
                    noSong.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 12px;");
                    commentsList.getChildren().add(noSong);
                });
                return;
            }
            new Thread(() -> {
                java.util.List<CommentRow> rows = new java.util.ArrayList<>();
                boolean hadError = false;

                try (Connection conn = Database.connect()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT c.id, c.user_id, u.username, c.comment_text, c.created_at, c.parent_comment_id " +
                                    "FROM song_comments c JOIN users u ON u.id = c.user_id " +
                                    "WHERE c.song_id = ? " +
                                    "ORDER BY c.created_at ASC")) {
                        ps.setInt(1, songId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int id = rs.getInt("id");
                                int uId = rs.getInt("user_id");
                                String uName = rs.getString("username");
                                String text = rs.getString("comment_text");
                                Timestamp ts = rs.getTimestamp("created_at");
                                int parent = rs.getInt("parent_comment_id");
                                Integer parentId = rs.wasNull() ? null : parent;

                                rows.add(new CommentRow(id, uId, uName, text, ts, parentId));
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    hadError = true;
                }

                final boolean hadErrorFinal = hadError;
                final java.util.List<CommentRow> rowsFinal = new java.util.ArrayList<>(rows);

                javafx.application.Platform.runLater(() -> {
                    commentsList.getChildren().clear();

                    if (hadErrorFinal) {
                        Label err = new Label("Failed to load comments.");
                        err.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
                        commentsList.getChildren().add(err);
                        return;
                    }

                    if (rowsFinal.isEmpty()) {
                        Label none = new Label("No comments yet. Be the first!");
                        none.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 12px;");
                        commentsList.getChildren().add(none);
                        return;
                    }

                    Map<Integer, java.util.List<CommentRow>> children = new HashMap<>();
                    java.util.List<CommentRow> roots = new java.util.ArrayList<>();
                    for (CommentRow row : rowsFinal) {
                        if (row.parentId == null) {
                            roots.add(row);
                        } else {
                            children.computeIfAbsent(row.parentId, k -> new java.util.ArrayList<>()).add(row);
                        }
                    }

                    class Renderer {
                        void add(CommentRow row, int depth) {
                            String when = "";
                            if (row.createdAt != null) {
                                when = row.createdAt.toString();
                                if (when.length() > 16) when = when.substring(0, 16);
                            }

                            Label header = new Label(row.username + "  \u2022  " + when);
                            header.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11px;");

                            Label body = new Label(row.text);
                            body.setWrapText(true);
                            body.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12px;");

                            Button replyBtn = new Button("Reply");
                            replyBtn.getStyleClass().add("button-secondary");
                            replyBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 8 2 8;");
                            replyBtn.setOnAction(ev -> {
                                replyingToCommentId[0] = row.id;
                                replyContextLabel.setText("Replying to " + row.username + "...");
                                commentInput.requestFocus();
                                commentInput.positionCaret(commentInput.getText().length());
                            });

                            HBox actions = new HBox(6, replyBtn);
                            actions.setAlignment(Pos.CENTER_LEFT);

                            if (row.userId == currentUserId) {
                                Button deleteBtn = new Button("Delete");
                                deleteBtn.getStyleClass().add("button-secondary");
                                deleteBtn.setStyle("-fx-font-size: 10px; -fx-padding: 2 8 2 8;");
                                deleteBtn.setOnAction(ev -> {
                                    new Thread(() -> {
                                        try (Connection conn = Database.connect()) {
                                            try (PreparedStatement ps = conn.prepareStatement(
                                                    "DELETE FROM song_comments WHERE id = ? AND user_id = ?")) {
                                                ps.setInt(1, row.id);
                                                ps.setInt(2, currentUserId);
                                                ps.executeUpdate();
                                            }
                                            javafx.application.Platform.runLater(() -> {
                                                if (replyingToCommentId[0] == row.id) {
                                                    replyingToCommentId[0] = -1;
                                                    replyContextLabel.setText("");
                                                }
                                                commentFeedback.setText("");
                                            });
                                            refreshComments[0].run();
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                            javafx.application.Platform.runLater(() -> {
                                                commentFeedback.setText("Failed to delete comment.");
                                                commentFeedback.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
                                            });
                                        }
                                    }, "delete-comment").start();
                                });
                                actions.getChildren().add(deleteBtn);
                            }

                            VBox cb = new VBox(2, header, body, actions);
                            cb.setStyle("-fx-background-color: #202020; -fx-background-radius: 8; -fx-padding: 6;");
                            cb.setPadding(new Insets(4, 0, 0, 0));

                            HBox wrapper = new HBox();
                            wrapper.setAlignment(Pos.TOP_LEFT);

                            Region spacer = new Region();
                            double indent = depth * 20.0;
                            spacer.setMinWidth(indent);
                            spacer.setPrefWidth(indent);
                            spacer.setMaxWidth(indent);

                            wrapper.getChildren().addAll(spacer, cb);

                            commentsList.getChildren().add(wrapper);

                            java.util.List<CommentRow> kids = children.get(row.id);
                            if (kids != null) {
                                for (CommentRow child : kids) {
                                    add(child, depth + 1);
                                }
                            }
                        }
                    }

                    Renderer renderer = new Renderer();
                    for (CommentRow root : roots) {
                        renderer.add(root, 0);
                    }
                });
            }, "load-comments-popup").start();
        };

        postCommentBtn.setOnAction(ev -> {
            String text = commentInput.getText().trim();
            if (text.isEmpty()) {
                commentFeedback.setText("Comment cannot be empty.");
                commentFeedback.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
                return;
            }
            if (songId <= 0) {
                commentFeedback.setText("Song is not available for commenting.");
                commentFeedback.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
                return;
            }

            commentFeedback.setText("Posting...");
            commentFeedback.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 11px;");

            new Thread(() -> {
                try (Connection conn = Database.connect()) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO song_comments (song_id, user_id, comment_text, parent_comment_id) " +
                                    "VALUES (?, ?, ?, ?)")) {
                        ps.setInt(1, songId);
                        ps.setInt(2, currentUserId);
                        ps.setString(3, text);
                        if (replyingToCommentId[0] > 0) {
                            ps.setInt(4, replyingToCommentId[0]);
                        } else {
                            ps.setNull(4, java.sql.Types.INTEGER);
                        }
                        ps.executeUpdate();
                    }

                    javafx.application.Platform.runLater(() -> {
                        commentInput.clear();
                        commentFeedback.setText("Comment posted.");
                        commentFeedback.setStyle("-fx-text-fill: #1db954; -fx-font-size: 11px;");
                        replyingToCommentId[0] = -1;
                        replyContextLabel.setText("");
                    });

                    refreshComments[0].run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        commentFeedback.setText("Failed to post comment.");
                        commentFeedback.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 11px;");
                    });
                }
            }, "post-comment-popup").start();
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("button-secondary");
        closeBtn.setOnAction(e -> popup.close());

        HBox actionRow = new HBox(10, likeBtn, excludeBtn, closeBtn);
        actionRow.setAlignment(Pos.CENTER);

        box.getChildren().addAll(
                imgView,
                trackLabel,
                artistLabel,
                albumLabel,
                playLabel,
                tagLabel,
                actionRow,
                commentsTitle,
                commentsScroll,
                replyContextLabel,
                commentInputRow,
                commentFeedback
        );

        refreshComments[0].run();

        Scene scene = new Scene(box, 380, 700);
        var css = getClass().getResource("/org/example/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        popup.setScene(scene);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.show();

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), box);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private int ensureSongExists(Connection conn, String track, String artist, String album, String tags, String albumImg, long plays) throws SQLException {
        try (PreparedStatement getSong = conn.prepareStatement(
                "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1"
        )) {
            getSong.setString(1, track);
            getSong.setString(2, artist);
            ResultSet rs = getSong.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }

        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO songs (track_name, artist_name, album_name, playcount, tags, album_image) VALUES (?, ?, ?, ?, ?, ?)"
        )) {
            insert.setString(1, track);
            insert.setString(2, artist);
            insert.setString(3, album);
            insert.setLong(4, plays);
            insert.setString(5, tags);
            insert.setString(6, albumImg);
            insert.executeUpdate();
        }

        try(PreparedStatement getSong = conn.prepareStatement(
                "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1"
        )){
            getSong.setString(1, track);
            getSong.setString(2, artist);
            ResultSet rs = getSong.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        throw new java.sql.SQLException("Failed to find or insert the song " + artist + " - " + track);
    }

    private void handleLike (String track, String artist, String album, String tags, long plays, String albumImg, Button likeBtn, Button excludeBtn) {
        try (Connection conn = Database.connect()) {
            int songId = ensureSongExists(conn, track, artist, album, tags, albumImg, plays);
            int userId = loginSystem.getCurrentUserId();

            boolean alreadyLiked = false;
            try(PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM user_likes WHERE user_id = ? AND song_id = ? LIMIT 1"
            )) {
                check.setInt(1, userId);
                check.setInt(2, songId);
                try(ResultSet rs = check.executeQuery()){
                    alreadyLiked = rs.next();
                }
            }

            if (alreadyLiked) {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM user_likes WHERE user_id = ? AND song_id = ?"
                )) {
                    del.setInt(1, userId);
                    del.setInt(2, songId);
                    del.executeUpdate();
                }

                likeBtn.setText("♥");
                if (excludeBtn != null && !"✖ Excluded".equals(excludeBtn.getText())){
                    excludeBtn.setText("✖");
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO user_likes (user_id, song_id) VALUES (?, ?)"
                )){
                    stmt.setInt(1, userId);
                    stmt.setInt(2, songId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement delEX = conn.prepareStatement(
                        "DELETE FROM user_exclusions WHERE user_id = ? AND song_id = ?"
                )){
                    delEX.setInt(1, userId);
                    delEX.setInt(2, songId);
                    delEX.executeUpdate();
                }

                likeBtn.setText("✔ Liked");
                if(excludeBtn != null){
                    excludeBtn.setText("✖");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            likeBtn.setText("Error");
        }
    }

    private void handleExclude (String track, String artist, String album, String tags, long plays, String albumImg, Button likeBtn, Button excludeBtn) {
        try (Connection conn = Database.connect()) {
            int songId = ensureSongExists(conn, track, artist, album, tags, albumImg, plays);
            int userId = loginSystem.getCurrentUserId();

            boolean alreadyExcluded = false;
            try(PreparedStatement check = conn.prepareStatement(
                    "SELECT 1 FROM user_exclusions WHERE user_id = ? AND song_id = ?"
            )){
                check.setInt(1, userId);
                check.setInt(2, songId);
                try(ResultSet rs = check.executeQuery()){
                    alreadyExcluded = rs.next();
                }
            }

            if (alreadyExcluded) {
                try (PreparedStatement del = conn.prepareStatement(
                        "DELETE FROM user_exclusions WHERE user_id = ? AND song_id = ?"
                )){
                    del.setInt(1, userId);
                    del.setInt(2, songId);
                    del.executeUpdate();
                }

                excludeBtn.setText("✖");
                if(likeBtn != null && !"✔ Liked".equals(likeBtn.getText())){
                    likeBtn.setText("♥");
                }
            } else {
                try (PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO user_exclusions (user_id, song_id) VALUES (?, ?)"
                )){
                    stmt.setInt(1, userId);
                    stmt.setInt(2, songId);
                    stmt.executeUpdate();
                }

                try (PreparedStatement delLiked = conn.prepareStatement(
                        "DELETE FROM user_likes WHERE user_id = ? AND song_id = ?"
                )){
                    delLiked.setInt(1, userId);
                    delLiked.setInt(2, songId);
                    delLiked.executeUpdate();
                }

                excludeBtn.setText("✔ Excluded");
                if(likeBtn != null){
                    likeBtn.setText("♥");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            excludeBtn.setText("Error");
        }
    }

    private void loadUserLibrary(Connection conn,
                                 int userId,
                                 Set<Integer> likeIds,
                                 Set<Integer> excludeIds) throws SQLException {
        likeIds.clear();
        excludeIds.clear();

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT song_id FROM user_likes WHERE user_id = ?"
        )) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    likeIds.add(rs.getInt("song_id"));
                }
            }
        }
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT song_id FROM user_exclusions WHERE user_id = ?"
        )) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    excludeIds.add(rs.getInt("song_id"));
                }
            }
        }
    }

    private Node buildDiscoverListView(List<DiscoverEntry> entries,
                                       Set<Integer> likedIds,
                                       Set<Integer> excludedIds) {
        VBox box = new VBox(10);
        box.setFillWidth(true);

        if (entries.isEmpty()) {
            Label empty = new Label("No tracks to show yet.");
            empty.setStyle("-fx-text-fill: #b3b3b3");
            box.getChildren().add(empty);
            return box;
        }

        for (DiscoverEntry e : entries) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 4, 6, 4));
            row.setStyle("-fx-cursor: hand;");

            ImageView albumView = new ImageView();
            if (e.albumImg != null && !e.albumImg.isBlank()
                    && !"(N/A)".equals(e.albumImg)
                    && e.albumImg.startsWith("http")) {
                try {
                    String safeUrl = e.albumImg;
                    if (safeUrl.startsWith("http://")) {
                        safeUrl = "https://" + safeUrl.substring("http://".length());
                    }
                    Image img = new Image(safeUrl, 50, 50, true, true, true);
                    albumView.setImage(img);
                } catch (Exception ignored) {}
            }
            albumView.setFitWidth(50);
            albumView.setFitHeight(50);
            albumView.setStyle("-fx-background-color: #333; -fx-background-radius: 8;");

            VBox textBox = new VBox(2);
            Label title = new Label(e.artist + " - " + e.track);
            title.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            String albumSuffix = (e.album != null && !e.album.isBlank()) ? " • " + e.album : "";
            Label sub = new Label(e.playcount + " plays" + albumSuffix);
            sub.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            textBox.getChildren().addAll(title, sub);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button likeBtn = new Button();
            likeBtn.getStyleClass().add("button-secondary");
            Button excludeBtn = new Button();
            excludeBtn.getStyleClass().add("button-secondary");

            if (likedIds != null && likedIds.contains(e.songId)) {
                likeBtn.setText("✔ Liked");
                excludeBtn.setText("✖");
            } else if (excludedIds != null && excludedIds.contains(e.songId)) {
                likeBtn.setText("♥");
                excludeBtn.setText("✖ Excluded");
            } else {
                likeBtn.setText("♥");
                excludeBtn.setText("✖");
            }

            likeBtn.setOnAction(ev -> {
                ev.consume();
                handleLike(e.track, e.artist, e.album,
                        e.tagsCsv, e.playcount, e.albumImg,
                        likeBtn, excludeBtn);
            });

            excludeBtn.setOnAction(ev -> {
                ev.consume();
                handleExclude(e.track, e.artist, e.album,
                        e.tagsCsv, e.playcount, e.albumImg,
                        likeBtn, excludeBtn);
            });

            row.setOnMouseClicked(ev -> {
                if (ev.getTarget() instanceof Button) return;
                showSongPopup(e.artist, e.track, e.album,
                        e.tagsCsv, e.playcount, e.albumImg);
            });

            row.getChildren().addAll(albumView, textBox, spacer, likeBtn, excludeBtn);
            box.getChildren().add(row);
        }

        return box;
    }

    private Node buildDiscoverGridView(List<DiscoverEntry> entries,
                                       Set<Integer> likedIds,
                                       Set<Integer> excludedIds) {
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.setPrefColumns(4);

        if (entries.isEmpty()) {
            Label empty = new Label("No tracks to show yet.");
            empty.setStyle("-fx-text-fill: #b3b3b3");
            return new VBox(empty);
        }

        for (DiscoverEntry e : entries) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(8));
            card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8;");
            card.setAlignment(Pos.TOP_CENTER);

            double cardWidth  = 210;
            double cardHeight = 260;

            card.setPrefWidth(cardWidth);
            card.setMinWidth(cardWidth);
            card.setMaxWidth(cardWidth);

            card.setPrefHeight(cardHeight);
            card.setMinHeight(cardHeight);
            card.setMaxHeight(cardHeight);

            ImageView albumView = new ImageView();
            if (e.albumImg != null && !e.albumImg.isBlank()
                    && !"(N/A)".equals(e.albumImg)
                    && e.albumImg.startsWith("http")) {
                try {
                    String safeUrl = e.albumImg;
                    if (safeUrl.startsWith("http://")) {
                        safeUrl = "https://" + safeUrl.substring("http://".length());
                    }
                    Image img = new Image(safeUrl, 120, 120, true, true, true);
                    albumView.setImage(img);
                } catch (Exception ignored) {}
            }
            albumView.setFitWidth(120);
            albumView.setFitHeight(120);
            albumView.setStyle("-fx-background-color: #333; -fx-background-radius: 8;");

            Label title = new Label(e.track);
            title.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            title.setWrapText(true);

            Label artist = new Label(e.artist);
            artist.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
            artist.setWrapText(true);

            HBox btnRow = new HBox(5);
            btnRow.setAlignment(Pos.CENTER);

            Button likeBtn = new Button();
            likeBtn.getStyleClass().add("button-secondary");
            Button excludeBtn = new Button();
            excludeBtn.getStyleClass().add("button-secondary");

            if (likedIds != null && likedIds.contains(e.songId)) {
                likeBtn.setText("✔ Liked");
                excludeBtn.setText("✖");
            } else if (excludedIds != null && excludedIds.contains(e.songId)) {
                likeBtn.setText("♥");
                excludeBtn.setText("✖ Excluded");
            } else {
                likeBtn.setText("♥");
                excludeBtn.setText("✖");
            }

            likeBtn.setOnAction(ev -> {
                ev.consume();
                handleLike(e.track, e.artist, e.album,
                        e.tagsCsv, e.playcount, e.albumImg,
                        likeBtn, excludeBtn);
            });

            excludeBtn.setOnAction(ev -> {
                ev.consume();
                handleExclude(e.track, e.artist, e.album,
                        e.tagsCsv, e.playcount, e.albumImg,
                        likeBtn, excludeBtn);
            });

            card.setOnMouseClicked(ev -> {
                if (ev.getTarget() instanceof Button) return;
                showSongPopup(e.artist, e.track, e.album,
                        e.tagsCsv, e.playcount, e.albumImg);
            });

            btnRow.getChildren().addAll(likeBtn, excludeBtn);
            card.getChildren().addAll(albumView, title, artist, btnRow);
            grid.getChildren().add(card);
        }
        return grid;
    }

    private void showLibrary(VBox content) {
        content.getChildren().clear();

        Label title = new Label("Your Library");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        Label loading = new Label("Loading your library...");
        loading.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 13px;");

        content.getChildren().addAll(title, loading);

        new Thread(() -> {
            List<LibraryEntry> likedEntries = new ArrayList<>();
            List<LibraryEntry> excludedEntries = new ArrayList<>();
            String errorMsg = null;

            try (Connection conn = Database.connect()) {
                int userId = loginSystem.getCurrentUserId();
                likedEntries = loadLibraryEntries(conn, userId, true);
                excludedEntries = loadLibraryEntries(conn, userId, false);
            } catch (SQLException ex) {
                ex.printStackTrace();
                errorMsg = ex.getMessage();
            }

            final List<LibraryEntry> likedFinal = likedEntries;
            final List<LibraryEntry> excludedFinal = excludedEntries;
            final String errorFinal = errorMsg;

            javafx.application.Platform.runLater(() -> {
                content.getChildren().clear();
                content.getChildren().add(title);

                if (errorFinal != null) {
                    Label error = new Label("Failed to load library: " + errorFinal);
                    error.setStyle("-fx-text-fill: red;");
                    content.getChildren().add(error);
                    return;
                }

                TabPane tabPane = new TabPane();
                tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

                Tab likedTab = createLibraryTab("Liked", likedFinal, true, () -> showLibrary(content));
                Tab excludedTab = createLibraryTab("Excluded", excludedFinal, false, () -> showLibrary(content));

                tabPane.getTabs().addAll(likedTab, excludedTab);
                tabPane.setMaxHeight(Double.MAX_VALUE);

                content.getChildren().add(tabPane);
                VBox.setVgrow(tabPane, Priority.ALWAYS);
                animateContent(content);
            });
        }, "load-library-thread").start();
    }

    private static class LibraryEntry{
        final int songId;
        final String track;
        final String artist;
        final String album;
        final long playcount;
        final String tagsCsv;
        final String albumImg;
        final Timestamp feedbackTime;
        final List<String> tagsNormalized;

        LibraryEntry(int songId, String track, String artist, String album, long playcount, String tagsCsv, String albumImg, Timestamp feedbackTime){
            this.songId = songId;
            this.track = track;
            this.artist = artist;
            this.album = album;
            this.playcount = playcount;
            this.tagsCsv = tagsCsv;
            this.albumImg = albumImg;
            this.feedbackTime = feedbackTime;

            List<String> list = new ArrayList<>();
            if(tagsCsv != null){
                String[] parts = tagsCsv.split(",");
                for (String p : parts){
                    String t = p.trim().toLowerCase();
                    if(!t.isEmpty()) list.add(t);
                }
            }
            this.tagsNormalized = list;
        }
    }

    private static class DiscoverEntry {
        final int songId;
        final String track;
        final String artist;
        final String album;
        final long playcount;
        final String tagsCsv;
        final String albumImg;

        DiscoverEntry(int songId,
                      String track,
                      String artist,
                      String album,
                      long playcount,
                      String tagsCsv,
                      String albumImg) {
            this.songId = songId;
            this.track = track;
            this.artist = artist;
            this.album = album;
            this.playcount = playcount;
            this.tagsCsv = tagsCsv;
            this.albumImg = albumImg;
        }
    }

    private boolean libraryMatchesTagFilter(LibraryEntry entry, String filter){
        if (filter == null || filter.isBlank()) return true;
        String f = filter.toLowerCase().trim();
        if(f.isEmpty()) return true;

        for (String tag : entry.tagsNormalized){
            if (tag.contains(f)) return true;
        }
        return false;
    }

    private void sortLibraryEntries(List<LibraryEntry> list, String sortMode){
        if (sortMode == null) sortMode = "Recent";

        switch (sortMode){
            case "Oldest":
                list.sort(Comparator.comparing(e -> e.feedbackTime));
                break;
            case "Most Played":
                list.sort(Comparator.comparing((LibraryEntry e) -> e.playcount).reversed());
                break;
            case "Least Played":
                list.sort(Comparator.comparing( e -> e.playcount));
                break;
            case "Recent":
            default:
                list.sort(Comparator.comparing((LibraryEntry e ) -> e.feedbackTime).reversed());
        }
    }

    private Node buildLibraryListView(List<LibraryEntry> entries,
                                      List<LibraryEntry> allEntries,
                                      boolean likedTab,
                                      Runnable refresh, Runnable reloadLibrary) {
        VBox box = new VBox(10);
        box.setFillWidth(true);

        if (entries.isEmpty()) {
            Label empty = new Label(likedTab
                    ? "You haven't liked any songs yet."
                    : "You haven't excluded any songs yet.");
            empty.setStyle("-fx-text-fill: #b3b3b3");
            box.getChildren().add(empty);
            return box;
        }

        for (LibraryEntry entry : entries) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(6, 4, 6, 4));
            row.setStyle("-fx-cursor: hand;");

            ImageView albumView = new ImageView();
            if (entry.albumImg != null && !entry.albumImg.isBlank()
                    && !"(N/A)".equals(entry.albumImg)
                    && entry.albumImg.startsWith("http")) {
                try {
                    String safeUrl = entry.albumImg;
                    if (safeUrl.startsWith("http://")) {
                        safeUrl = "https://" + safeUrl.substring("http://".length());
                    }
                    Image img = new Image(safeUrl, 50, 50, true, true, true);
                    albumView.setImage(img);
                } catch (Exception ignored) {}
            }
            albumView.setFitWidth(50);
            albumView.setFitHeight(50);
            albumView.setStyle("-fx-background-color: #333; -fx-background-radius: 8;");

            VBox textBox = new VBox(2);
            Label title = new Label(entry.artist + " - " + entry.track);
            title.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
            Label sub = new Label(entry.playcount + " plays" +
                    (entry.album != null && !entry.album.isBlank() ? " • " + entry.album : ""));
            sub.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");
            textBox.getChildren().addAll(title, sub);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button likeBtn = new Button();
            likeBtn.getStyleClass().add("button-secondary");
            Button excludeBtn = new Button();
            excludeBtn.getStyleClass().add("button-secondary");

            if (likedTab) {
                likeBtn.setText("✔ Liked");
                excludeBtn.setText("✖");
            } else {
                likeBtn.setText("♥");
                excludeBtn.setText("✖ Excluded");
            }

            if (likedTab) {
                likeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleLike(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    if (allEntries.remove(entry)) {
                        refresh.run();
                    }
                });

                excludeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleExclude(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    reloadLibrary.run();
                });

            } else {
                excludeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleExclude(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    if (allEntries.remove(entry)) {
                        refresh.run();
                    }
                });

                likeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleLike(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    reloadLibrary.run();
                });
            }
            row.setOnMouseClicked(ev -> {
                if (ev.getTarget() instanceof Button) return;
                showSongPopup(entry.artist, entry.track, entry.album,
                        entry.tagsCsv, entry.playcount, entry.albumImg);
            });
            row.getChildren().addAll(albumView, textBox, spacer, likeBtn, excludeBtn);
            box.getChildren().add(row);
        }
        return box;
    }


    private Node buildLibraryGridView(List<LibraryEntry> entries,
                                      List<LibraryEntry> allEntries,
                                      boolean likedTab,
                                      Runnable refresh, Runnable reloadLibrary) {
        TilePane grid = new TilePane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10));
        grid.setPrefColumns(4);

        if (entries.isEmpty()) {
            Label empty = new Label(likedTab
                    ? "You haven't liked any songs yet."
                    : "You haven't excluded any songs yet.");
            empty.setStyle("-fx-text-fill: #b3b3b3");
            return new VBox(empty);
        }

        for (LibraryEntry entry : entries) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(8));
            card.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8;");
            card.setAlignment(Pos.TOP_CENTER);

            // 🔸 force same size for all cards (Liked & Excluded)
            double cardWidth  = 210;
            double cardHeight = 260;

            card.setPrefWidth(cardWidth);
            card.setMinWidth(cardWidth);
            card.setMaxWidth(cardWidth);

            card.setPrefHeight(cardHeight);
            card.setMinHeight(cardHeight);
            card.setMaxHeight(cardHeight);

            ImageView albumView = new ImageView();
            if (entry.albumImg != null && !entry.albumImg.isBlank()
                    && !"(N/A)".equals(entry.albumImg)
                    && entry.albumImg.startsWith("http")) {
                try {
                    String safeUrl = entry.albumImg;
                    if (safeUrl.startsWith("http://")) {
                        safeUrl = "https://" + safeUrl.substring("http://".length());
                    }
                    Image img = new Image(safeUrl, 120, 120, true, true, true);
                    albumView.setImage(img);
                } catch (Exception ignored) {}
            }
            albumView.setFitWidth(120);
            albumView.setFitHeight(120);
            albumView.setStyle("-fx-background-color: #333; -fx-background-radius: 8;");

            Label title = new Label(entry.track);
            title.setStyle("-fx-text-fill: white; -fx-font-size: 13px; -fx-font-weight: bold;");
            title.setWrapText(true);

            Label artist = new Label(entry.artist);
            artist.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
            artist.setWrapText(true);

            HBox btnRow = new HBox(5);
            btnRow.setAlignment(Pos.CENTER);

            Button likeBtn = new Button();
            likeBtn.getStyleClass().add("button-secondary");
            Button excludeBtn = new Button();
            excludeBtn.getStyleClass().add("button-secondary");

            if (likedTab) {
                likeBtn.setText("✔ Liked");
                excludeBtn.setText("✖");
            } else {
                likeBtn.setText("♥");
                excludeBtn.setText("✖ Excluded");
            }
            if (likedTab) {
                likeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleLike(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    if (allEntries.remove(entry)) {
                        refresh.run();
                    }
                });
                excludeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleExclude(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    reloadLibrary.run();
                });
            } else {
                excludeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleExclude(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    if (allEntries.remove(entry)) {
                        refresh.run();
                    }
                });
                likeBtn.setOnAction(ev -> {
                    ev.consume();
                    handleLike(entry.track, entry.artist, entry.album,
                            entry.tagsCsv, entry.playcount, entry.albumImg,
                            likeBtn, excludeBtn);
                    reloadLibrary.run();
                });
            }
            btnRow.getChildren().addAll(likeBtn, excludeBtn);

            card.setOnMouseClicked(ev -> {
                if (ev.getTarget() instanceof Button) return;
                showSongPopup(entry.artist, entry.track, entry.album,
                        entry.tagsCsv, entry.playcount, entry.albumImg);
            });

            card.getChildren().addAll(albumView, title, artist, btnRow);
            grid.getChildren().add(card);
        }
        return grid;
    }

    private List<LibraryEntry> loadLibraryEntries(Connection conn, int userId, boolean likedTab) throws SQLException {
        List<LibraryEntry> result = new ArrayList<>();

        String feedbackSql = likedTab
                ? "SELECT song_id, COALESCE(liked_at, created_at) AS fb_time " +
                "FROM user_likes WHERE user_id = ? ORDER BY fb_time DESC"
                : "SELECT song_id, created_at AS fb_time " +
                "FROM user_exclusions WHERE user_id = ? ORDER BY created_at DESC";

        try (PreparedStatement ps = conn.prepareStatement(feedbackSql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {

                try (PreparedStatement songPs = conn.prepareStatement(
                        "SELECT track_name, artist_name, album_name, playcount, tags, album_image " +
                                "FROM songs WHERE id = ?")) {

                    while (rs.next()) {
                        int songId = rs.getInt("song_id");
                        Timestamp fbTime = rs.getTimestamp("fb_time");

                        songPs.setInt(1, songId);
                        try (ResultSet rsSong = songPs.executeQuery()) {
                            if (!rsSong.next()) {
                                continue;
                            }

                            result.add(new LibraryEntry(
                                    songId,
                                    rsSong.getString("track_name"),
                                    rsSong.getString("artist_name"),
                                    rsSong.getString("album_name"),
                                    rsSong.getLong("playcount"),
                                    rsSong.getString("tags"),
                                    rsSong.getString("album_image"),
                                    fbTime
                            ));
                        }
                    }
                }
            }
        }
        System.out.println((likedTab ? "Liked" : "Excluded") + " entries loaded: " + result.size());
        return result;
    }

    private Tab createLibraryTab(String tabTitle,
                                 List<LibraryEntry> allEntries,
                                 boolean likedTab,  Runnable reloadLibrary) {

        Tab tab = new Tab(tabTitle);
        tab.setClosable(false);

        BorderPane root = new BorderPane();

        TextField filterField = new TextField();
        filterField.setPromptText("Filter by tag.");
        filterField.setPrefWidth(160);
        filterField.setStyle("-fx-background-color: #121212; -fx-text-fill: white;");

        ComboBox<String> sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Recent", "Oldest", "Most played", "Least played");
        sortCombo.getSelectionModel().selectFirst();
        sortCombo.setPromptText("Sort by");
        sortCombo.setStyle("-fx-background-color: #282828; -fx-text-fill: white;");

        ToggleGroup viewGroup = new ToggleGroup();
        ToggleButton listBtn = new ToggleButton("List");
        ToggleButton gridBtn = new ToggleButton("Grid");
        listBtn.setToggleGroup(viewGroup);
        gridBtn.setToggleGroup(viewGroup);
        listBtn.setSelected(true);
        listBtn.getStyleClass().add("button-secondary");
        gridBtn.getStyleClass().add("button-secondary");

        HBox controls = new HBox(10,
                new Label("Tag:"), filterField,
                new Label("Sort:"), sortCombo,
                listBtn, gridBtn
        );
        controls.setPadding(new Insets(10, 0, 10, 0));
        controls.setAlignment(Pos.CENTER_LEFT);

        root.setTop(controls);

        final ViewMode[] viewMode = { ViewMode.LIST };

        final Runnable[] renderRef = new Runnable[1];

        renderRef[0] = () -> {
            String filter = filterField.getText();
            String sortMode = sortCombo.getValue();

            List<LibraryEntry> filtered = new ArrayList<>();
            for (LibraryEntry e : allEntries) {
                if (libraryMatchesTagFilter(e, filter)) {
                    filtered.add(e);
                }
            }
            sortLibraryEntries(filtered, sortMode);
            Node view = (viewMode[0] == ViewMode.LIST)
                    ? buildLibraryListView(filtered, allEntries, likedTab, renderRef[0], reloadLibrary)
                    : buildLibraryGridView(filtered, allEntries, likedTab, renderRef[0], reloadLibrary);

            ScrollPane scroll = new ScrollPane(view);
            scroll.setFitToWidth(true);
            scroll.setFitToHeight(true);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scroll.setStyle("-fx-background-color: transparent;");

            root.setCenter(scroll);
        };

        filterField.textProperty().addListener((obs, ov, nv) -> renderRef[0].run());
        sortCombo.valueProperty().addListener((obs, ov, nv) -> renderRef[0].run());
        viewGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == gridBtn) {
                viewMode[0] = ViewMode.GRID;
            } else {
                viewMode[0] = ViewMode.LIST;
            }
            renderRef[0].run();
        });

        renderRef[0].run();
        tab.setContent(root);
        return tab;
    }

    private void showStats(VBox content) {
        content.getChildren().clear();

        Label title = new Label("Your Statistics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");

        Label loading = new Label("Loading your statistics...");
        loading.setStyle("-fx-text-fill: #b3b3b3");

        content.getChildren().addAll(title, loading);
        animateContent(content);

        new Thread(() -> {
            int userId = loginSystem.getCurrentUserId();

            int likedCount = 0;
            int excludedCount = 0;
            long totalPlaycount = 0L;
            List<String> topArtists = new ArrayList<>();
            List<String> topTags = new ArrayList<>();

            try (Connection conn = Database.connect()) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM user_likes WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) likedCount = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM user_exclusions WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) excludedCount = rs.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT SUM(s.playcount) " +
                                "FROM user_likes ul JOIN songs s ON s.id = ul.song_id " +
                                "WHERE ul.user_id = ?")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) totalPlaycount = rs.getLong(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT s.artist_name, COUNT(*) AS c " +
                                "FROM user_likes ul JOIN songs s ON s.id = ul.song_id " +
                                "WHERE ul.user_id = ? " +
                                "GROUP BY s.artist_name " +
                                "ORDER BY c DESC " +
                                "LIMIT 5")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String artist = rs.getString("artist_name");
                            int c = rs.getInt("c");
                            topArtists.add(artist + " (" + c + " liked tracks)");
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT t.tag_name, COUNT(*) AS c " +
                                "FROM user_likes ul JOIN tags t ON t.song_id = ul.song_id " +
                                "WHERE ul.user_id = ? " +
                                "GROUP BY t.tag_name " +
                                "ORDER BY c DESC " +
                                "LIMIT 10")) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String tag = rs.getString("tag_name");
                            int c = rs.getInt("c");
                            topTags.add(tag + " (" + c + ")");
                        }
                    }
                }

                final int likedCountFinal = likedCount;
                final int excludedCountFinal = excludedCount;
                final long totalPlaycountFinal = totalPlaycount;
                final List<String> topArtistsFinal = new ArrayList<>(topArtists);
                final List<String> topTagsFinal = new ArrayList<>(topTags);

                javafx.application.Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().add(title);

                    Label summary = new Label(
                            "Liked tracks: " + likedCountFinal +
                                    "   |   Excluded tracks: " + excludedCountFinal +
                                    "   |   Total Last.fm playcount of liked tracks: " + totalPlaycountFinal
                    );
                    summary.setStyle("-fx-text-fill: #ffffff;");
                    Label artistsHeader = new Label("Top artists from your likes");
                    artistsHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

                    VBox artistsBox = new VBox(3);
                    if (topArtistsFinal.isEmpty()) {
                        Label none = new Label("No liked tracks yet – start liking songs in Discover or Library.");
                        none.setStyle("-fx-text-fill: #b3b3b3");
                        artistsBox.getChildren().add(none);
                    } else {
                        int idx = 1;
                        for (String row : topArtistsFinal) {
                            Label lbl = new Label(idx + ". " + row);
                            lbl.setStyle("-fx-text-fill: #b3b3b3");
                            artistsBox.getChildren().add(lbl);
                            idx++;
                        }
                    }
                    Label tagsHeader = new Label("Top tags from your likes");
                    tagsHeader.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

                    VBox tagsBox = new VBox(3);
                    if (topTagsFinal.isEmpty()) {
                        Label none = new Label("Not enough data yet.");
                        none.setStyle("-fx-text-fill: #b3b3b3");
                        tagsBox.getChildren().add(none);
                    } else {
                        int idx = 1;
                        for (String row : topTagsFinal) {
                            Label lbl = new Label(idx + ". " + row);
                            lbl.setStyle("-fx-text-fill: #b3b3b3");
                            tagsBox.getChildren().add(lbl);
                            idx++;
                        }
                    }
                    content.getChildren().addAll(
                            summary,
                            artistsHeader,
                            artistsBox,
                            tagsHeader,
                            tagsBox
                    );
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    content.getChildren().clear();
                    content.getChildren().add(title);
                    Label error = new Label("Failed to load statistics.");
                    error.setStyle("-fx-text-fill: #ff6b6b");
                    content.getChildren().add(error);
                });
            }
        }, "stats-thread").start();
    }

    private void showSettings(VBox content, Stage stage) {
        content.getChildren().clear();

        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");

        Label subtitle = new Label("Manage your account or log out");
        subtitle.setStyle("-fx-text-fill: #b3b3b3");

        String currentUsername = "";
        try (Connection conn = Database.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT username FROM users WHERE id = ?")) {
                ps.setInt(1, loginSystem.getCurrentUserId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentUsername = rs.getString(1);
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        Label usernameLabel = new Label("Username:");
        usernameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        TextField usernameField = new TextField(currentUsername);
        usernameField.setPromptText("New username");

        Button saveUsernameBtn = new Button("Save username");
        saveUsernameBtn.getStyleClass().add("button-primary");

        Label usernameFeedbackLabel = new Label();
        usernameFeedbackLabel.setStyle("-fx-text-fill: #b3b3b3");

        saveUsernameBtn.setOnAction(ev -> {
            String newName = usernameField.getText().trim();
            if (newName.isEmpty()) {
                usernameFeedbackLabel.setText("Username cannot be empty.");
                usernameFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                return;
            }

            new Thread(() -> {
                try (Connection conn = Database.connect()) {
                    int userId = loginSystem.getCurrentUserId();

                    try (PreparedStatement check = conn.prepareStatement(
                            "SELECT id FROM users WHERE username = ? AND id <> ?")) {
                        check.setString(1, newName);
                        check.setInt(2, userId);
                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next()) {
                                javafx.application.Platform.runLater(() -> {
                                    usernameFeedbackLabel.setText("That username is already taken.");
                                    usernameFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                                });
                                return;
                            }
                        }
                    }

                    try (PreparedStatement upd = conn.prepareStatement(
                            "UPDATE users SET username = ? WHERE id = ?")) {
                        upd.setString(1, newName);
                        upd.setInt(2, userId);
                        upd.executeUpdate();
                    }

                    javafx.application.Platform.runLater(() -> {
                        usernameFeedbackLabel.setText("Username updated successfully.");
                        usernameFeedbackLabel.setStyle("-fx-text-fill: #1db954");
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        usernameFeedbackLabel.setText("Error updating username. Please try again.");
                        usernameFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                    });
                }
            }, "update-username-thread").start();
        });

        HBox usernameRow = new HBox(10, usernameLabel, usernameField, saveUsernameBtn);
        usernameRow.setAlignment(Pos.CENTER_LEFT);

        Label pwTitle = new Label("Change password");
        pwTitle.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

        PasswordField currentPwField = new PasswordField();
        currentPwField.setPromptText("Current password");

        PasswordField newPwField = new PasswordField();
        newPwField.setPromptText("New password");

        PasswordField confirmPwField = new PasswordField();
        confirmPwField.setPromptText("Confirm new password");

        Button changePwBtn = new Button("Change password");
        changePwBtn.getStyleClass().add("button-primary");

        Label pwFeedbackLabel = new Label();
        pwFeedbackLabel.setStyle("-fx-text-fill: #b3b3b3");

        changePwBtn.setOnAction(ev -> {
            String currentPw = currentPwField.getText();
            String newPw = newPwField.getText();
            String confirmPw = confirmPwField.getText();

            if (currentPw.isEmpty() || newPw.isEmpty() || confirmPw.isEmpty()) {
                pwFeedbackLabel.setText("All password fields are required.");
                pwFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                return;
            }
            if (!newPw.equals(confirmPw)) {
                pwFeedbackLabel.setText("New passwords do not match.");
                pwFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                return;
            }
            if (newPw.length() < 6) {
                pwFeedbackLabel.setText("New password should be at least 6 characters.");
                pwFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                return;
            }

            new Thread(() -> {
                try (Connection conn = Database.connect()) {
                    int userId = loginSystem.getCurrentUserId();

                    String existingHash = null;
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT password_hash FROM users WHERE id = ?")) {
                        ps.setInt(1, userId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                existingHash = rs.getString(1);
                            }
                        }
                    }

                    if (existingHash == null || !BCrypt.checkpw(currentPw, existingHash)) {
                        javafx.application.Platform.runLater(() -> {
                            pwFeedbackLabel.setText("Current password is incorrect.");
                            pwFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                        });
                        return;
                    }

                    String newHash = BCrypt.hashpw(newPw, BCrypt.gensalt());
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE users SET password_hash = ? WHERE id = ?")) {
                        ps.setString(1, newHash);
                        ps.setInt(2, userId);
                        ps.executeUpdate();
                    }

                    javafx.application.Platform.runLater(() -> {
                        pwFeedbackLabel.setText("Password updated successfully.");
                        pwFeedbackLabel.setStyle("-fx-text-fill: #1db954");
                        currentPwField.clear();
                        newPwField.clear();
                        confirmPwField.clear();
                    });

                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        pwFeedbackLabel.setText("Error updating password. Please try again.");
                        pwFeedbackLabel.setStyle("-fx-text-fill: #ff6b6b");
                    });
                }
            }, "change-password-thread").start();
        });
        VBox pwBox = new VBox(5,
                pwTitle,
                currentPwField,
                newPwField,
                confirmPwField,
                changePwBtn,
                pwFeedbackLabel
        );
        pwBox.setAlignment(Pos.CENTER_LEFT);
        pwBox.setPadding(new Insets(10, 0, 0, 0));
        Button logout = new Button("Log Out");
        logout.getStyleClass().add("button-primary");
        logout.setOnAction(e -> {
            try {
                new LoginGUI().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        content.getChildren().addAll(
                title,
                subtitle,
                usernameRow,
                usernameFeedbackLabel,
                pwBox,
                spacer,
                logout
        );
        animateContent(content);
    }

    private void animateContent(VBox content) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void highlightBtn(Button active, Button... others) {
        active.setStyle("-fx-background-color: #1db954; -fx-text-fill: black; -fx-font-weight: bold");
        for (Button b : others) {
            b.setStyle("");
            b.getStyleClass().add("button-secondary");
        }
    }
}
