package org.qp.android.ui.game;

import android.graphics.Typeface;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListGameItemBinding;
import org.qp.android.model.libQP.QpListItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GameItemRecycler extends RecyclerView.Adapter<GameItemRecycler.ViewHolder> {
    private final AsyncListDiffer<QpListItem> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    private Typeface typeface;
    private int textSize;
    private int backgroundColor;
    private int textColor;
    private int linkTextColor;

    public void setTypeface(Typeface typeface) {
        this.typeface = typeface;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
    }

    public void setLinkTextColor(int linkTextColor) {
        this.linkTextColor = linkTextColor;
    }

    public QpListItem getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<QpListItem> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    private static final DiffUtil.ItemCallback<QpListItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull QpListItem oldItem , @NonNull QpListItem newItem) {
                    return Objects.equals(oldItem.pathToImage , newItem.pathToImage) && Objects.equals(oldItem.text , newItem.text);
                }

                @Override
                public boolean areContentsTheSame(@NonNull QpListItem oldItem , @NonNull QpListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public void submitList(ArrayList<QpListItem> gameData){
        differ.submitList(gameData);
    }

    @NonNull
    @Override
    public GameItemRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                          int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListGameItemBinding listGameItemBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_game_item , parent, false);
        return new GameItemRecycler.ViewHolder(listGameItemBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GameItemRecycler.ViewHolder holder, int position) {
        holder.listItemActionObjectBinding(getGameData().get(position));
        var qpListItem = getItem(position);

        if (qpListItem.pathToImage != null) {
            Picasso.get()
                    .load(new File(qpListItem.pathToImage))
                    .error(R.drawable.baseline_broken_image_24)
                    .fit()
                    .into(holder.listGameItemBinding.itemIcon);
        }

        if (qpListItem.text != null) {
            var itemText = holder.listGameItemBinding.itemText;
            itemText.setTypeface(typeface);
            itemText.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            itemText.setBackgroundColor(backgroundColor);
            itemText.setTextColor(textColor);
            itemText.setLinkTextColor(linkTextColor);
            // TODO: 27.06.2023 rewrite this!
            itemText.setText(Html.fromHtml(String.valueOf(qpListItem.text) , Html.FROM_HTML_MODE_LEGACY));
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListGameItemBinding listGameItemBinding;

        ViewHolder(ListGameItemBinding binding){
            super(binding.getRoot());
            this.listGameItemBinding = binding;
        }

        public void listItemActionObjectBinding(QpListItem qpListItem) {
            listGameItemBinding.setQpListItem(qpListItem);
            listGameItemBinding.executePendingBindings();
        }
    }
}
