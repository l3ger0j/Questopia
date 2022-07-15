package com.qsp.player.view.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.qsp.player.R;
import com.qsp.player.dto.stock.GameData;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class GamesRecyclerAdapter extends RecyclerView.Adapter<GamesRecyclerAdapter.ViewHolder>{
    private final LayoutInflater inflater;
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

    public GamesRecyclerAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public GamesRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                              int viewType) {
        View view = inflater.inflate(R.layout.list_item_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GamesRecyclerAdapter.ViewHolder holder, int position) {
        GameData gameData = differ.getCurrentList().get(position);

        // gameIcon
        if (gameData.icon.isEmpty()) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.broken_image_24, null
            );
            holder.gameIcon.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(gameData.icon)
                    .fit()
                    .into(holder.gameIcon);
        }

        // gameSize
        if (gameData.fileSize != null) {
            holder.gameSize.setText(context.getString(R.string.fileSize).replace("-SIZE-",
                    Integer.toString(gameData.getFileSize() / 1024)));
        }


        // gameTitle
        if (holder.gameTitle != null) {
            holder.gameTitle.setText(gameData.title);
            if (gameData.isInstalled()) {
                holder.gameTitle.setTextColor(0xFFE0E0E0);
            } else {
                holder.gameTitle.setTextColor(0xFFFFD700);
            }
        }

        // gameAuthor
        if (gameData.author.length() > 0) {
            String text = context.getString(R.string.author)
                    .replace("-AUTHOR-", gameData.author);
            holder.gameAuthor.setText(text);
        } else {
            holder.gameAuthor.setText("");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView gameIcon;
        TextView gameTitle, gameAuthor, gameSize, gameRate;

        ViewHolder(View view){
            super(view);
            gameSize = view.findViewById(R.id.game_size);
            gameRate = view.findViewById(R.id.game_rate);
            gameIcon = view.findViewById(R.id.game_icon);
            gameTitle = view.findViewById(R.id.game_title);
            gameAuthor = view.findViewById(R.id.game_author);
        }
    }
}

