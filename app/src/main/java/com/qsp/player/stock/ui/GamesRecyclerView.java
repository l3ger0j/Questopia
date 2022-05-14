package com.qsp.player.stock.ui;

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
import com.qsp.player.stock.dto.Game;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GamesRecyclerView extends RecyclerView.Adapter<GamesRecyclerView.ViewHolder>{

    private final LayoutInflater inflater;
    private final ArrayList<Game> games;
    private final Context context;

    public Game getItem(int position) {
        return games.get(position);
    }

    public GamesRecyclerView(Context context, ArrayList<Game> games) {
        this.games = games;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public GamesRecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item_game, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GamesRecyclerView.ViewHolder holder, int position) {
        Game game = games.get(position);

        // gameIcon
        if (game.icon.isEmpty()) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.broken_image_24, null
            );
            holder.gameIcon.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(game.icon)
                    .fit()
                    .into(holder.gameIcon);
        }

        // gameTitle
        if (holder.gameTitle != null) {
            holder.gameTitle.setText(game.title);
            if (game.isInstalled()) {
                holder.gameTitle.setTextColor(0xFFE0E0E0);
            } else {
                holder.gameTitle.setTextColor(0xFFFFD700);
            }
        }

        // gameAuthor
        if (game.author.length() > 0) {
            String text = context.getString(R.string.author)
                    .replace("-AUTHOR-", game.author);
            holder.gameAuthor.setText(text);
        } else {
            holder.gameAuthor.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return games.size();
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

