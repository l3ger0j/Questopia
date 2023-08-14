package org.qp.android.ui.plugin

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.qp.android.R
import org.qp.android.databinding.ListItemPluginBinding
import org.qp.android.dto.plugin.PluginInfo

class PluginRecycler(private val context: Context) :
    RecyclerView.Adapter<PluginRecycler.ViewHolder>() {

    private val differ = AsyncListDiffer(this, DIFF_CALLBACK)
    private fun getItem(position: Int): PluginInfo {
        return differ.currentList[position]
    }

    private val gameData: List<PluginInfo>
        get() = differ.currentList

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun submitList(pluginInfo: ArrayList<PluginInfo>?) {
        differ.submitList(pluginInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val listItemPluginBinding = DataBindingUtil.inflate<ListItemPluginBinding>(
            inflater,
            R.layout.list_item_plugin,
            parent,
            false
        )
        return ViewHolder(listItemPluginBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.listItemPluginBinding(gameData[position])
        val pluginInfo = getItem(position)

        // pluginAuthor
        if (pluginInfo.author != null && pluginInfo.author!!.isNotEmpty()) {
            holder.listItemPluginBinding.pluginAuthor.text = context.getString(R.string.author)
                .replace("-AUTHOR-", pluginInfo.author!!)
        } else {
            holder.listItemPluginBinding.pluginAuthor.text = ""
        }
    }

    class ViewHolder internal constructor(var listItemPluginBinding: ListItemPluginBinding) :
        RecyclerView.ViewHolder(
            listItemPluginBinding.root
        ) {
        fun listItemPluginBinding(pluginInfo: PluginInfo?) {
            listItemPluginBinding.pluginInfo = pluginInfo
            listItemPluginBinding.executePendingBindings()
        }
    }

    companion object {
        private val DIFF_CALLBACK: DiffUtil.ItemCallback<PluginInfo> =
            object : DiffUtil.ItemCallback<PluginInfo>() {
                override fun areItemsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
                    return oldItem.version == newItem.version
                }

                override fun areContentsTheSame(oldItem: PluginInfo, newItem: PluginInfo): Boolean {
                    return oldItem == newItem
                }
            }
    }
}