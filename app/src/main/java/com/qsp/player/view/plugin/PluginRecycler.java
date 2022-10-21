package com.qsp.player.view.plugin;

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
import com.qsp.player.databinding.ListItemPluginBinding;
import com.qsp.player.dto.PluginList;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class PluginRecycler extends RecyclerView.Adapter<PluginRecycler.ViewHolder> {
    private final Context context;
    private final AsyncListDiffer<PluginList> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    public PluginList getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<PluginList> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    private static final DiffUtil.ItemCallback<PluginList> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PluginList>() {
        @Override
        public boolean areItemsTheSame(@NonNull PluginList oldItem , @NonNull PluginList newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PluginList oldItem , @NonNull PluginList newItem) {
            return oldItem.equals(newItem);
        }
    };

    public void submitList(ArrayList<PluginList> pluginLists){
        differ.submitList(pluginLists);
    }

    public PluginRecycler(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PluginRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent , int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ListItemPluginBinding listItemPluginBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_plugin, parent, false);
        return new PluginRecycler.ViewHolder(listItemPluginBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PluginRecycler.ViewHolder holder , int position) {
        holder.listItemPluginBinding(differ.getCurrentList().get(position));
        PluginList pluginList = getItem(position);

        // gameIcon
        if (pluginList.image.isEmpty()) {
            Drawable drawable = ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.broken_image , null
            );
            holder.listItemPluginBinding.pluginIcon.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(pluginList.image)
                    .fit()
                    .into(holder.listItemPluginBinding.pluginIcon);
        }

        // gameSize
        if (pluginList.fileSize != null) {
            holder.listItemPluginBinding.pluginSize
                    .setText(context.getString(R.string.fileSize)
                            .replace("-SIZE-", Integer.toString(pluginList.getFileSize())));
        }

        // gameAuthor
        if (pluginList.author.length() > 0) {
            String text = context.getString(R.string.author)
                    .replace("-AUTHOR-", pluginList.author);
            holder.listItemPluginBinding.pluginAuthor.setText(text);
        } else {
            holder.listItemPluginBinding.pluginAuthor.setText("");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListItemPluginBinding listItemPluginBinding;

        ViewHolder(ListItemPluginBinding listItemPluginBinding){
            super(listItemPluginBinding.getRoot());
            this.listItemPluginBinding = listItemPluginBinding;
        }

        public void listItemPluginBinding(PluginList pluginList) {
            listItemPluginBinding.setPluginList(pluginList);
            listItemPluginBinding.executePendingBindings();
        }
    }
}
