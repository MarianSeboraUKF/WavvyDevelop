package sk.ukf.wavvy;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import sk.ukf.wavvy.adapter.SongAdapter;
import sk.ukf.wavvy.model.Song;

public class QueueBottomSheet extends BottomSheetDialogFragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_queue, container, false);
        RecyclerView rvQueue = view.findViewById(R.id.rvQueue);
        PlaybackManager pm = PlaybackManager.get(requireContext());

        int[] queueIds = pm.getQueueIds();

        if (queueIds == null || queueIds.length == 0) {
            return view;
        }
        ArrayList<Song> songs = new ArrayList<>();

        for (int id : queueIds) {
            Song s = SongRepository.findByAudioResId(id);
            if (s != null) songs.add(s);
        }

        SongAdapter adapter = new SongAdapter(songs, true, false, song -> {
                    int position = songs.indexOf(song);
                    if (position != -1) {
                        pm.playFromQueue(position);
                    }
                    dismiss();
                }
        );

        rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        rvQueue.setAdapter(adapter);

        int currentId = pm.getCurrentAudioResId();

        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).getAudioResId() == currentId) {
                rvQueue.scrollToPosition(i);
                break;
            }
        }

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                        int fromPos = viewHolder.getAdapterPosition();
                        int toPos = target.getAdapterPosition();

                        Collections.swap(songs, fromPos, toPos);
                        adapter.notifyItemMoved(fromPos, toPos);

                        pm.moveQueueItem(fromPos, toPos);
                        return true;
                    }
                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                        int position = viewHolder.getAdapterPosition();

                        pm.removeFromQueue(position);
                        songs.remove(position);
                        adapter.notifyItemRemoved(position);
                        adapter.notifyItemRangeChanged(position, songs.size());
                    }
                    @Override
                    public boolean isLongPressDragEnabled() {
                        return true;
                    }
                };
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(rvQueue);
        return view;
    }
    @Override
    public int getTheme() {
        return R.style.BottomSheetTheme;
    }

    @Override
    public void onStart() {
        super.onStart();
        View view = getView();
        if (view != null) {
            View parent = (View) view.getParent();

            parent.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            int screenHeight = getResources().getDisplayMetrics().heightPixels;
            parent.getLayoutParams().height = (int) (screenHeight * 0.65);
        }
    }
}