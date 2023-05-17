package hh.game.mgba_android.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anggrayudi.storage.SimpleStorageHelper
import com.anggrayudi.storage.file.getAbsolutePath
import com.anggrayudi.storage.file.getStorageId
import hh.game.mgba_android.adapter.GameListAdapter
import hh.game.mgba_android.R
import hh.game.mgba_android.database.GB.GBgameData
import hh.game.mgba_android.database.GBA.GBAgameData
import hh.game.mgba_android.utils.Gameutils
import hh.game.mgba_android.utils.GameListListener

class GameListActivity : AppCompatActivity() {
    private val storageHelper = SimpleStorageHelper(this)
    private var sharepreferences: SharedPreferences? = null
    private var storageid: String? = null
    private var FOLDER_PATH: String = "folder_path"
    private var STORAGEID: String = "storageid"
    private lateinit var gameListAdapter: GameListAdapter
    private lateinit var gamelistview: RecyclerView
    private var gamelist: ArrayList<DocumentFile>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_list)
        gamelistview = findViewById<RecyclerView>(R.id.gamelistview)
        gameListAdapter = GameListAdapter(this, ArrayList())
        sharepreferences = getSharedPreferences("mGBA", Context.MODE_PRIVATE)
        var permissionlist = contentResolver.persistedUriPermissions

        if (permissionlist.size > 0) {
            storageid = sharepreferences?.getString(STORAGEID, null)
            setupUI()
        } else {
            sharepreferences?.edit()?.putString(FOLDER_PATH, null)?.apply()
            storageHelper.openFolderPicker()
            setupStorageFolder()
        }
    }

    fun setupStorageFolder() {
        storageHelper.onFolderSelected = { requestCode, folder ->
            sharepreferences?.edit()?.putString(FOLDER_PATH, folder.uri.toString())?.apply()
            storageid = folder.getStorageId(this)
            sharepreferences?.edit()?.putString(STORAGEID, storageid)?.apply()
            setupUI()
        }
    }

    fun setupUI() {
        var uri = Uri.parse(sharepreferences?.getString(FOLDER_PATH, null))
        var documentfile = DocumentFile.fromTreeUri(this, uri)
        var coverfilefolder = documentfile?.findFile("gbacovers")
        gamelistview.layoutManager = LinearLayoutManager(this)
        gamelistview.adapter = gameListAdapter.also {
            gamelist = ArrayList(documentfile?.listFiles()?.filter {
                it.getAbsolutePath(this).contains(".gba", ignoreCase = true)
                        ||
                        it.getAbsolutePath(this).contains(".gb", ignoreCase = true)
            }?.toList())
            Gameutils.getGameList(
                this@GameListActivity,
                gamelist!!, ArrayList(coverfilefolder?.listFiles()?.toList()), ArrayList(),
                object : GameListListener {
                    override fun onGetGamelist(
                        gbagamelist: ArrayList<GBAgameData>,
                        gbgamelist: ArrayList<GBgameData>
                    ) {
                        var list = ArrayList(gbagamelist + gbgamelist)
                        it.updateList(list)
                        it.updateCoverfolder(coverfilefolder)
                        it.itemClickListener = { position, game ->
                            startActivity(
                                Intent(
                                    this@GameListActivity,
                                    GameActivity::class.java
                                ).also {
                                    var game = list.get(position)
                                    var gamepath = when (game) {
                                        is GBAgameData -> game.gbaDocumentFile.getAbsolutePath(this@GameListActivity)
                                        else -> (game as GBgameData).gbDocumentFile.getAbsolutePath(
                                            this@GameListActivity
                                        )
                                    }
                                    it.putExtra(
                                        "gamepath",
                                        gamepath
                                    )
                                    if (game is GBAgameData) {
                                        it.putExtra("cheat", game.gbaGame.GameNum)
                                    }
                                })
                        }
                    }
                })
        }
    }
}