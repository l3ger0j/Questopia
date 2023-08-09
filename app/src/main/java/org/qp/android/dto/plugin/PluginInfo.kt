package org.qp.android.dto.plugin

class PluginInfo {
    @JvmField
    var version: String? = null
    @JvmField
    var title: String? = null
    @JvmField
    var author: String? = null
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as PluginInfo
        return version == that.version && title == that.title && author == that.author
    }

    override fun hashCode(): Int {
        var result = version?.hashCode() ?: 0
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (author?.hashCode() ?: 0)
        return result
    }
}