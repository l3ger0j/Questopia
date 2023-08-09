package org.qp.android.dto.plugin

class PluginInfo {

    var version: String? = null
    var title: String? = null
    var author: String? = null

    override fun hashCode(): Int {
        var result = version?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (author?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PluginInfo

        if (version != other.version) return false
        if (title != other.title) return false
        if (author != other.author) return false

        return true
    }
}