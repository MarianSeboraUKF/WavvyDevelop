package sk.ukf.wavvy;

public class SongEditingHolder {
    public interface Callback {
        void onSelected(String uri);
    }
    public static Callback callback;
}