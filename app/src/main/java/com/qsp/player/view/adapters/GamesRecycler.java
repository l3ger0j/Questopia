package com.qsp.player.view.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.qsp.player.R;
import com.qsp.player.databinding.ListItemGameBinding;
import com.qsp.player.dto.stock.GameData;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class GamesRecycler extends RecyclerView.Adapter<GamesRecycler.ViewHolder>{
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
            new DiffUtil.ItemCallback<GameData>() {
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

    public GamesRecycler(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public GamesRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                       int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ListItemGameBinding listItemGameBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_game, parent, false);
        return new ViewHolder(listItemGameBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GamesRecycler.ViewHolder holder, int position) {
        holder.listItemGameBinding(differ.getCurrentList().get(position));
        GameData gameData = getItem(position);

        // gameIcon
        if (gameData.icon.isEmpty()) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.broken_image , null
            );
            holder.listItemGameBinding.gameIcon.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(gameData.icon)
                    .fit()
                    .into(holder.listItemGameBinding.gameIcon);
        }

        // gameSize
        if (gameData.fileSize != null) {
            holder.listItemGameBinding.gameSize
                    .setText(context.getString(R.string.fileSize)
                            .replace("-SIZE-", Integer.toString(gameData.getFileSize())));
        }

        // gameAuthor
        if (gameData.author.length() > 0) {
            String text = context.getString(R.string.author)
                    .replace("-AUTHOR-", gameData.author);
            holder.listItemGameBinding.gameAuthor.setText(text);
        } else {

            holder.listItemGameBinding.gameAuthor.setText("");
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

