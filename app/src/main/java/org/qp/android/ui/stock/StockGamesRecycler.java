package org.qp.android.ui.stock;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListItemGameBinding;
import org.qp.android.dto.stock.GameData;

import java.util.ArrayList;
import java.util.List;

public class StockGamesRecycler extends RecyclerView.Adapter<StockGamesRecycler.ViewHolder> {

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

    public void submitList(ArrayList<GameData> gameData){
        differ.submitList(gameData);
    }

    public StockGamesRecycler(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public StockGamesRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                            int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemGameBinding listItemGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_game, parent, false);
        return new ViewHolder(listItemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull StockGamesRecycler.ViewHolder holder, int position) {
        holder.listItemGameBinding(getGameData().get(position));
        var gameData = getItem(position);

        if (gameData.icon != null) {
            Picasso.get()
                    .load(Uri.parse(gameData.icon))
                    .fit()
                    .error(R.drawable.baseline_broken_image_24)
                    .into(holder.listItemGameBinding.gameIcon);
        }

        if (gameData.fileSize != null) {
            holder.listItemGameBinding.gameSize
                    .setText(context.getString(R.string.fileSize)
                            .replace("-SIZE-", gameData.getFileSize()));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListItemGameBinding listItemGameBinding;

        ViewHolder(ListItemGameBinding listItemGameBinding){
            super(listItemGameBinding.getRoot());
            this.listItemGameBinding = listItemGameBinding;
        }

        public void listItemGameBinding(GameData gameData) {
            listItemGameBinding.setGameData(gameData);
            listItemGameBinding.executePendingBindings();
        }
    }
}

