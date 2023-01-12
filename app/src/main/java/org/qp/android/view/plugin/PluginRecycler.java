package org.qp.android.view.plugin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.qp.android.R;
import org.qp.android.databinding.ListItemPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;

import java.util.ArrayList;
import java.util.List;

public class PluginRecycler extends RecyclerView.Adapter<PluginRecycler.ViewHolder> {
    private final Context context;
    private final AsyncListDiffer<PluginInfo> differ =
            new AsyncListDiffer<>(this , DIFF_CALLBACK);

    public PluginInfo getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    public List<PluginInfo> getGameData() {
        return differ.getCurrentList();
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    private static final DiffUtil.ItemCallback<PluginInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull PluginInfo oldItem , @NonNull PluginInfo newItem) {
                    return oldItem.id == newItem.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull PluginInfo oldItem , @NonNull PluginInfo newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public void submitList(ArrayList<PluginInfo> pluginInfo){
        differ.submitList(pluginInfo);
    }

    public PluginRecycler(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public PluginRecycler.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent , int viewType) {
        var inflater = LayoutInflater.from(parent.getContext());
        ListItemPluginBinding listItemPluginBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_plugin, parent, false);
        return new PluginRecycler.ViewHolder(listItemPluginBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull PluginRecycler.ViewHolder holder , int position) {
        holder.listItemPluginBinding(differ.getCurrentList().get(position));
        var pluginInfo = getItem(position);

        // gameIcon
        if (pluginInfo.image != null && pluginInfo.image.isEmpty()) {
            var drawable = ResourcesCompat.getDrawable(
                    context.getResources(),
                    R.drawable.broken_image , null
            );
            holder.listItemPluginBinding.pluginIcon.setImageDrawable(drawable);
        } else {
            Picasso.get()
                    .load(pluginInfo.image)
                    .fit()
                    .into(holder.listItemPluginBinding.pluginIcon);
        }

        // gameSize
        if (pluginInfo.fileSize != null) {
            holder.listItemPluginBinding.pluginSize
                    .setText(context.getString(R.string.fileSize)
                            .replace("-SIZE-", Integer.toString(pluginInfo.getFileSize())));
        }

        // gameAuthor
        if (pluginInfo.author != null && pluginInfo.author.length() > 0) {
            holder.listItemPluginBinding.pluginAuthor
                    .setText(context.getString(R.string.author)
                            .replace("-AUTHOR-", pluginInfo.author));
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

        public void listItemPluginBinding(PluginInfo pluginInfo) {
            listItemPluginBinding.setPluginInfo(pluginInfo);
            listItemPluginBinding.executePendingBindings();
        }
    }
}
