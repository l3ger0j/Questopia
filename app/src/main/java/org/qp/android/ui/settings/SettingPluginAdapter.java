package org.qp.android.ui.settings;

import static org.qp.android.helpers.utils.StringUtil.isNotEmptyOrBlank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.qp.android.R;
import org.qp.android.databinding.ListItemPluginBinding;
import org.qp.android.dto.plugin.PluginInfo;

import java.util.List;
import java.util.Objects;

public class SettingPluginAdapter extends RecyclerView.Adapter<SettingPluginAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<PluginInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<>() {
                @Override
                public boolean areItemsTheSame(@NonNull PluginInfo oldItem, @NonNull PluginInfo newItem) {
                    return Objects.equals(oldItem.version(), newItem.version());
                }

                @Override
                public boolean areContentsTheSame(@NonNull PluginInfo oldItem, @NonNull PluginInfo newItem) {
                    return oldItem.equals(newItem);
                }
            };
    private Context context;
    private final AsyncListDiffer<PluginInfo> differ = new AsyncListDiffer<>(this, DIFF_CALLBACK);

    public PluginInfo getItem(int position) {
        return differ.getCurrentList().get(position);
    }

    @Override
    public int getItemCount() {
        return differ.getCurrentList().size();
    }

    public void submitList(List<PluginInfo> pluginInfo) {
        differ.submitList(pluginInfo);
    }

    @NonNull
    @Override
    public SettingPluginAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        var inflater = LayoutInflater.from(context);
        ListItemPluginBinding listItemPluginBinding =
                DataBindingUtil.inflate(inflater, R.layout.list_item_plugin, parent, false);
        return new SettingPluginAdapter.ViewHolder(listItemPluginBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingPluginAdapter.ViewHolder holder, int position) {
        holder.listItemPluginBinding(getItem(position));
        var pluginInfo = getItem(position);

        if (isNotEmptyOrBlank(pluginInfo.author())) {
            var authorText = context.getString(R.string.author)
                    .replace("-AUTHOR-", pluginInfo.author());
            holder.listItemPluginBinding.pluginAuthor.setText(authorText);
        } else {
            holder.listItemPluginBinding.pluginAuthor.setText("");
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ListItemPluginBinding listItemPluginBinding;

        ViewHolder(@NonNull ListItemPluginBinding listItemPluginBinding) {
            super(listItemPluginBinding.getRoot());
            this.listItemPluginBinding = listItemPluginBinding;
        }

        public void listItemPluginBinding(PluginInfo pluginInfo) {
            listItemPluginBinding.setPluginInfo(pluginInfo);
            listItemPluginBinding.executePendingBindings();
        }
    }
}
