package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.ui.stock.StockViewModel.DISABLE_CALCULATE_DIR;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.databinding.ListItemLocalGameBinding;
import org.qp.android.databinding.ListItemRemoteGameBinding;

import java.util.List;
import java.util.Objects;

public class GamesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final DiffUtil.ItemCallback<Game> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Game oldItem, @NonNull Game newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
    private final Context context;
    private final AsyncListDiffer<Game> differ =
            new AsyncListDiffer<>(this, DIFF_CALLBACK);
    private final int currentPage;

    public GamesListAdapter(Context context, Integer currentPage) {
        this.context = context;
        this.currentPage = currentPage;
    }

    public Game getGameEntry(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<Game> getGameEntries() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public GamesListAdapter submitList(List<Game> gameEntriesList) {
        differ.submitList(gameEntriesList);
        return this;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        if (currentPage == 0) {
            ListItemLocalGameBinding listItemLocalGameBinding =
                    DataBindingUtil.inflate(inflater, R.layout.list_item_local_game, parent, false);
            listItemLocalGameBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
            return new LocalGameHolder(listItemLocalGameBinding);
        } else {
            ListItemRemoteGameBinding listItemRemoteGameBinding =
                    DataBindingUtil.inflate(inflater, R.layout.list_item_remote_game, parent, false);
            listItemRemoteGameBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
            return new RemoteGameHolder(listItemRemoteGameBinding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RemoteGameHolder remoteGameHolder) {
            remoteGameHolder.listItemGameBinding(getGameEntries().get(position));
            var gameData = getGameEntry(position);

            var fileSize = gameData.fileSize;
            if (fileSize == null || fileSize.isEmpty() || fileSize.isBlank()) return;
            if (fileSize.equals(DISABLE_CALCULATE_DIR)) return;

            var elementSize = remoteGameHolder.listItemRemoteGameBinding.gameSize;
            var fileSizeString = context.getString(R.string.fileSize);

            elementSize.setText(fileSizeString.replace("-SIZE-", fileSize));
        } else if (holder instanceof LocalGameHolder localGameHolder) {
            localGameHolder.listItemGameBinding(getGameEntries().get(position));
            var gameData = getGameEntry(position);

            var fileSize = gameData.fileSize;
            if (fileSize == null || fileSize.isEmpty() || fileSize.isBlank()) return;
            if (fileSize.equals(DISABLE_CALCULATE_DIR)) return;

            var elementSize = localGameHolder.listItemLocalGameBinding.gameSize;
            var fileSizeString = context.getString(R.string.fileSize);

            elementSize.setText(fileSizeString.replace("-SIZE-", fileSize));
        }
    }

    public static class RemoteGameHolder extends RecyclerView.ViewHolder {
        ListItemRemoteGameBinding listItemRemoteGameBinding;

        RemoteGameHolder(ListItemRemoteGameBinding listItemRemoteGameBinding) {
            super(listItemRemoteGameBinding.getRoot());
            this.listItemRemoteGameBinding = listItemRemoteGameBinding;
        }

        public void listItemGameBinding(Game gameEntry) {
            var gameDataObserver = new GameDataObserver();
            gameDataObserver.titleObserver.set(gameEntry.title);
            gameDataObserver.isGameInstalled.set(gameEntry.listId == 1);
            gameDataObserver.iconUriObserver.set(gameEntry.gameIconUri);
            listItemRemoteGameBinding.setData(gameDataObserver);
            listItemRemoteGameBinding.executePendingBindings();
        }
    }

    public static class LocalGameHolder extends RecyclerView.ViewHolder {
        ListItemLocalGameBinding listItemLocalGameBinding;

        LocalGameHolder(ListItemLocalGameBinding listItemLocalGameBinding) {
            super(listItemLocalGameBinding.getRoot());
            this.listItemLocalGameBinding = listItemLocalGameBinding;
        }

        public void listItemGameBinding(Game gameEntry) {
            var gameDataObserver = new GameDataObserver();
            gameDataObserver.titleObserver.set(gameEntry.title);
            gameDataObserver.isGameInstalled.set(gameEntry.listId == 0);
            gameDataObserver.iconUriObserver.set(gameEntry.gameIconUri);
            listItemLocalGameBinding.setData(gameDataObserver);
            listItemLocalGameBinding.executePendingBindings();
        }
    }
}

