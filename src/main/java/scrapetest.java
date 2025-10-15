import com.google.gson.*;
import okhttp3.*;
import java.io.IOException;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

public class scrapetest {
    private static final String API_KEY = "f57b66b50c5683a01d2f2ee6d09d9d12";

    public static void main(String[] args) throws IOException {
        OkHttpClient client = new OkHttpClient();

        String url = "http://ws.audioscrobbler.com/2.0/?method=tag.gettoptracks&tag=rock&api_key=" +
                API_KEY + "&format=json";

        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()){
            if (!response.isSuccessful()){
                throw new RuntimeException("Unexpected code " + response);
            }

            String json = response.body().string();

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            JsonObject tracksObj = root.getAsJsonObject("tracks");
            JsonArray tracks = tracksObj.getAsJsonArray("track");

            System.out.println("Top Rock Tracks on Last.fm:");
            for (int i = 0; i < Math.min(10, tracks.size()); i++){
                JsonObject track = tracks.get(i).getAsJsonObject();
                String name = track.get("name").getAsString();
                String artist = track.getAsJsonObject("artist").get("name").getAsString();
                String playcount = track.has("playcount") ? track.get("playcount").getAsString() : "N/A";
                System.out.println("\t" + name + "\t" + artist + "\t" + playcount);

            }
        }
    }
}
