package org.qp.android.helpers.repository

import android.content.Context
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.qp.android.dto.stock.InnerGameData
import org.qp.android.helpers.utils.FileUtil
import org.qp.android.helpers.utils.XmlUtil
import java.util.*

class LocalGame {
    private val TAG = this.javaClass.simpleName
    private var context: Context? = null

    fun getGames(context: Context?, gamesDir: DocumentFile?): List<InnerGameData> {
        this.context = context
        if (gamesDir == null) {
            return emptyList()
        }
        val gameDirs = getGameDirectories(gamesDir)
        return if (gameDirs != null) {
            if (gameDirs.isEmpty()) {
                return emptyList()
            }
            val items = ArrayList<InnerGameData>()
            for (folder in getGameFolders(gameDirs)) {
                var item = null as InnerGameData?
                val info = getGameInfo(folder)
                if (info != null) {
                    item = parseGameInfo(info)
                }
                if (item == null) {
                    val name = folder.dir.name
                    item = InnerGameData()
                    item.id = name
                    item.title = name
                }
                item.gameDir = folder.dir
                item.gameFiles = folder.gameFiles
                items.add(item)
            }
            items
        } else {
            Log.d(TAG, "game dir is null")
            emptyList()
        }
    }

    private fun getGameDirectories(gamesDir: DocumentFile): ArrayList<DocumentFile>? {
        return try {
            val dirs = ArrayList<DocumentFile>()
            for (f in gamesDir.listFiles()) {
                if (f.isDirectory) {
                    dirs.add(f)
                }
            }
            dirs
        } catch (e: NullPointerException) {
            Log.d(TAG, "Error: ", e)
            null
        }
    }

    private fun getGameFolders(dirs: List<DocumentFile>): List<GameFolder> {
        val folders = ArrayList<GameFolder>()
        sortFilesByName(dirs)
        for (dir in dirs) {
            val gameFiles = ArrayList<DocumentFile>()
            val files = listOf(*dir.listFiles())
            sortFilesByName(files)
            for (file in files) {
                val lcName = file.name!!.lowercase()
                if (lcName.endsWith(".qsp") || lcName.endsWith(".gam")) {
                    gameFiles.add(file)
                }
            }
            folders.add(GameFolder(dir, gameFiles))
        }
        return folders
    }

    private fun sortFilesByName(files: List<DocumentFile>) {
        if (files.size < 2) return
        files.sortedWith(Comparator.comparing { o: DocumentFile ->
            o.name!!.lowercase()
        })
    }

    private fun getGameInfo(game: GameFolder): String? {
        val gameInfoFiles = FileUtil.findFileOrDirectory(game.dir, GAME_INFO_FILENAME)
        if (gameInfoFiles == null || gameInfoFiles.length() == 0L) {
            Log.w(TAG, "InnerGameData info file not found in " + game.dir.name)
            return null
        }
        return FileUtil.readFileAsString(context, gameInfoFiles)
    }

    private fun parseGameInfo(xml: String): InnerGameData? {
        return try {
            XmlUtil.xmlToObject(xml, InnerGameData::class.java)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to parse game info file", ex)
            null
        }
    }

    private class GameFolder(val dir: DocumentFile, val gameFiles: List<DocumentFile>)
    companion object {
        private const val GAME_INFO_FILENAME = "gameStockInfo"
    }
}