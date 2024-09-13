package org.qp.android.ui.stock;

import static org.qp.android.ui.stock.StockViewModel.DISABLE_CALCULATE_DIR;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListItemGameBinding;
import org.qp.android.dto.stock.GameData;

import java.util.List;

public class GamesListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int DIVIDER = 1;
    private static final int ITEM = 2;

    private final Context context;
    private final AsyncListDiffer<GameData> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    public GameData getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<GameData> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    @Override
    public int getItemViewType(int position) {
        var currGameData = getItem(position);

        if (getItemCount() > position + 1) {
            var nextGameData = getItem(position + 1);

            if (currGameData.listId.equals("0")
                    && nextGameData.listId.equals("1")) {
                return DIVIDER;
            }
        }

        return ITEM;
    }

    private static final DiffUtil.ItemCallback<GameData> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull GameData oldItem , @NonNull GameData newItem) {
                    return oldItem.id.equals(newItem.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull GameData oldItem , @NonNull GameData newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public GamesListAdapter submitList(List<GameData> gameData){
        differ.submitList(gameData);
        return this;
    }

    public GamesListAdapter(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemGameBinding listItemGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_game, parent, false);
        return new GameHolder(listItemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof GameHolder gameHolder) {
            gameHolder.listItemGameBinding(getGameData().get(position));
            var gameData = getItem(position);

            if (gameData.icon != null) {
                Picasso.get()
                        .load(Uri.parse(gameData.icon))
                        .fit()
                        .error(R.drawable.baseline_broken_image_24)
                        .into(gameHolder.listItemGameBinding.gameIcon);
            }

            var fileSize = gameData.fileSize;
            if (fileSize == null || fileSize.isEmpty() || fileSize.isBlank()) return;
            if (fileSize.equals(DISABLE_CALCULATE_DIR)) return;

            var elementSize = gameHolder.listItemGameBinding.gameSize;
            var fileSizeString = context.getString(R.string.fileSize);

            elementSize.setText(fileSizeString.replace("-SIZE-", fileSize));
        }
    }

    public static class GameHolder extends RecyclerView.ViewHolder {
        ListItemGameBinding listItemGameBinding;

        GameHolder(ListItemGameBinding listItemGameBinding){
            super(listItemGameBinding.getRoot());
            this.listItemGameBinding = listItemGameBinding;
            //Если у родительской ноды одна дочерняя нода,то эта нода игнорируется и родительской нодой становится дочерняя нода. Так происходит до тех пор,пока у родительской ноды нее появятся дочерние ноды. В этом случае фреймворк отлавливает нажатие на эту ноду,или на дочерние ноды. Примерно так,на мой взгляд,это работает.
            this.listItemGameBinding.relativeLayout.setAccessibilityDelegate(new View.AccessibilityDelegate() {
    @Override
    public boolean performAccessibilityAction(@NonNull View host, int action, @Nullable Bundle args) {
        if(action== AccessibilityNodeInfo.ACTION_CLICK) return host.performClick(); else if(action==AccessibilityNodeInfo.ACTION_LONG_CLICK) return host.performLongClick();
        return super.performAccessibilityAction(host, action, args);
    }
});
                    }

        public void listItemGameBinding(GameData gameData) {
            listItemGameBinding.setGameData(gameData);
            listItemGameBinding.executePendingBindings();
        }
    }
}

