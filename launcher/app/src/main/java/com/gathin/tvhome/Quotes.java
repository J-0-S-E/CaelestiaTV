package com.gathin.tvhome;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Random;

/**
 * A random quote under the clock, as on Caelestia's lock screen: the theme's quotes.json, a list of
 * {@code {"text": ..., "author": ...}}. No file, or one that cannot be read, and the quote stays hidden.
 */
final class Quotes {
    static final String FILE = "quotes.json";

    private static final Random RANDOM = new Random();
    /** The parsed file and the modification time it was read at, so a pushed theme is picked up without a restart. */
    private static JSONArray cached;
    private static long cachedModified;

    private Quotes() {}

    /** Puts a new quote in text and "~ author" in author, or hides both. Either view may be null (a layout without them). */
    static void show(Context ctx, TextView text, TextView author) {
        if (text == null || author == null) return;
        JSONObject q = pick(ctx);
        String body = q == null ? "" : q.optString("text", "").trim();
        if (body.isEmpty()) {
            text.setVisibility(View.GONE);
            author.setVisibility(View.GONE);
            return;
        }
        String by = q.optString("author", "").trim();
        text.setText(body);
        text.setVisibility(View.VISIBLE);
        author.setText(by.isEmpty() ? "" : "~ " + by);
        author.setVisibility(by.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private static synchronized JSONObject pick(Context ctx) {
        File dir = Config.dir(ctx);
        if (dir == null) return null;
        File file = new File(dir, FILE);
        if (!file.isFile()) return null;
        if (cached == null || file.lastModified() != cachedModified) {
            try {
                cached = new JSONArray(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
                cachedModified = file.lastModified();
            } catch (IOException | JSONException e) {
                cached = null;
                return null;
            }
        }
        if (cached.length() == 0) return null;
        return cached.optJSONObject(RANDOM.nextInt(cached.length()));
    }
}
