package sk.ukf.wavvy;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;

public class RecentlyPlayedRepository {
    private static final String PREFS = "wavvy_prefs";
    private static final String KEY_RECENT = "recent_songs";

    public static void add(Context ctx, int audioResId) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String existing = sp.getString(KEY_RECENT, "");
        ArrayList<Integer> list = new ArrayList<>();

        if (!existing.isEmpty()) {

            String[] parts = existing.split(",");

            for (String p : parts) {
                try {
                    int id = Integer.parseInt(p);
                    list.add(id);
                } catch (Exception ignored) {}
            }
        }

        list.remove((Integer) audioResId);
        list.add(0, audioResId);

        if (list.size() > 10) {
            list = new ArrayList<>(list.subList(0, 10));
        }

        StringBuilder csv = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            if (i > 0) csv.append(",");
            csv.append(list.get(i));
        }
        sp.edit().putString(KEY_RECENT, csv.toString()).apply();
    }

    public static ArrayList<Integer> get(Context ctx) {

        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String csv = sp.getString(KEY_RECENT, "");

        ArrayList<Integer> list = new ArrayList<>();

        if (csv.isEmpty()) return list;

        String[] parts = csv.split(",");

        for (String p : parts) {
            try {
                list.add(Integer.parseInt(p));
            } catch (Exception ignored) {}
        }
        return list;
    }
}