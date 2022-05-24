package com.qsp.player.view.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.qsp.player.R;
import com.qsp.player.dto.stock.GameData;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GamesRecyclerAdapter extends RecyclerView.Adapter<GamesRecyclerAdapter.ViewHolder>{
    private final LayoutInflater inflater;
    private final ArrayList<GameData> gameData;
    private final Context context;

    public GameData getItem(int position) {
        return gameData.get(position);
    }

    public GamesRecyclerAdapter(Context context, ArrayList<GameData> gameData) {
        this.gameData = gameData;
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
        GameData gameData = this.gameData.get(position);

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

    @Override
    public int getItemCount() {
        return gameData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView gameIcon;
        TextView gameTitle, gameAuthor;

        ViewHolder(View view){
            super(view);
            gameIcon = view.findViewById(R.id.game_icon);
            gameTitle = view.findViewById(R.id.game_title);
            gameAuthor = view.findViewById(R.id.game_author);
        }
    }
}

