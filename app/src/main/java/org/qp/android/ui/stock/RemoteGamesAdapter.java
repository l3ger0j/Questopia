package org.qp.android.ui.stock;

import static org.qp.android.helpers.utils.AccessibilityUtil.customAccessibilityDelegate;
import static org.qp.android.helpers.utils.FileUtil.formatFileSize;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.data.db.Game;
import org.qp.android.databinding.ListItemRemoteGameBinding;
import org.qp.android.ui.settings.SettingsController;

import java.util.Objects;

public class RemoteGamesAdapter extends PagingDataAdapter<Game, RemoteGamesAdapter.RemoteGameHolder> {

    public static final int LOADING_ITEM = 0;
    public static final int GAME_ITEM = 1;
    private final static boolean DEFAULT_VALUE = false;
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
    private Context context;

    public Game getItemByPosition(int position) {
        return getItem(position);
    }

    public RemoteGamesAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public RemoteGameHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemRemoteGameBinding listItemRemoteGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_remote_game, parent, false);
        listItemRemoteGameBinding.relativeLayout.setAccessibilityDelegate(customAccessibilityDelegate());
        return new RemoteGameHolder(listItemRemoteGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RemoteGameHolder holder, int position) {
        var item = getItem(position);
        if (item == null) return;

        holder.listItemGameBinding(item);

        var fileSize = item.fileSize;
        if (fileSize == 0) return;

        var currBinPref = SettingsController.getInstance(context).binaryPrefixes;
        var sizeWithPref = formatFileSize(fileSize, currBinPref);

        var elementSize = holder.listItemRemoteGameBinding.gameSize;
        var fileSizeString = context.getString(R.string.fileSize);
        elementSize.setText(fileSizeString.replace("-SIZE-", sizeWithPref));
    }

    @Override
    public int getItemViewType(int position) {
        return position == getItemCount() ? GAME_ITEM : LOADING_ITEM;
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
            gameDataObserver.isGameInstalled.set(DEFAULT_VALUE);
            gameDataObserver.iconUriObserver.set(gameEntry.gameIconUri);
            listItemRemoteGameBinding.setData(gameDataObserver);
            listItemRemoteGameBinding.executePendingBindings();
        }
    }

}
