package com.example.musicplayerapp.ui.home


import android.Manifest
import android.app.Activity
import android.app.SearchManager
import android.content.*
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.musicplayerapp.R
import com.example.musicplayerapp.adapter.SongAdapter
import com.example.musicplayerapp.databinding.ActivityHomeBinding
import com.example.musicplayerapp.login.LoginActivity
import com.example.musicplayerapp.model.CurrentSong
import com.example.musicplayerapp.model.SongModel
import com.example.musicplayerapp.service.SongService
import com.example.musicplayerapp.ui.gallery.favorite.FavoriteFragment
import com.example.musicplayerapp.ui.slideshow.SlideshowActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration


class HomeActivity : AppCompatActivity(){
    companion object {
        private const val REQUEST_CODE = 1
        var listLoadDataFavorite = mutableListOf<SongModel>()
        private const val MY_SHARED_PREFERENCES = "My_Shared_Preferences"
        private const val PREF_LIST_USER = "Pref_List_User"
    }

    private lateinit var songAdapter: SongAdapter
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHomeBinding
    private lateinit var userAvatar: ImageView
    private lateinit var userInfoReference: DatabaseReference
    private lateinit var songInfoReference: DatabaseReference
    private lateinit var user: FirebaseUser
    private lateinit var imageUri: Uri
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var jsonArray: JSONArray
    private lateinit var typePosition: String
    private var isPause: String = ""
    private var isFirstTime = true


    //Handle action on miniPlayer
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.getIntExtra("action", 0)
            isPause = intent.getBooleanExtra("isPause", false).toString()
            handleLayoutMusic(action, isPause.toBoolean())
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_home)

        //firebase reference songInfo
        songInfoReference = FirebaseDatabase.getInstance().getReference("songInfo")

        songAdapter = SongAdapter(this)

        //create sharePreferences to storage listFavoriteSong
        sharedPreferences = getSharedPreferences(MY_SHARED_PREFERENCES, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        //navController
        val navController = this.findNavController(R.id.myNavHostFragment)

        //navDrawer
        drawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(navController.graph, drawerLayout)
        NavigationUI.setupActionBarWithNavController(this, navController, drawerLayout)
        NavigationUI.setupWithNavController(binding.navView, navController)

        //navDrawer with user info
        setupUserInfo()

        //log out from navDrawer
        setupNavDrawer()

        //set up permission to access external storage
        checkPermission()
        requestPermission()

        //get data(listFavorite from sharePreferences
        getListSongData()

        //currentSong state
        getValueCurrentSong()


        onStartService()

        sendActionToService()


        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, IntentFilter("send_data_to_activity"))

        // intent to full screen player
        binding.cardView.setOnClickListener() {
            val intent = Intent(this, SlideshowActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStop() {
        editor.clear().apply()
        setListLoadDataFavorite(FavoriteFragment.listFavoriteSong)
        super.onStop()
    }

    override fun onDestroy() {
        onStopService()
        FirebaseDatabase.getInstance().getReference("status").child("pause").setValue(true)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search_menu, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView.maxWidth = Integer.MAX_VALUE
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                songAdapter.filter.filter(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                songAdapter.filter.filter(newText)
                return true
            }

        })
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return NavigationUI.navigateUp(navController, drawerLayout)
    }

    private fun setupUserInfo() {

        val headerView = binding.navView.getHeaderView(0)
        val userName = headerView.findViewById<TextView>(R.id.driver_name)
        val userPhone = headerView.findViewById<TextView>(R.id.driver_phone)
        val userEmail = headerView.findViewById<TextView>(R.id.driver_email)
        userAvatar = headerView.findViewById(R.id.img_avatar)
        user = FirebaseAuth.getInstance().currentUser!!
        val listInfo = ArrayList<String>()
        userInfoReference =
            FirebaseDatabase.getInstance().getReference("userInfo").child(user!!.uid)
        userInfoReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //set drive info to nav header
                Glide.with(applicationContext)
                    .load(dataSnapshot.child("avatar").value.toString())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(userAvatar)
                userName.text =
                    StringBuilder("Welcome, ").append(dataSnapshot.child("displayName").value.toString())
                userPhone.text =
                    StringBuilder("Phone: ").append(dataSnapshot.child("phoneNumber").value.toString())
                userEmail.text =
                    StringBuilder("Email: ").append(dataSnapshot.child("email").value.toString())

                //change avatar
                userAvatar.setOnClickListener {
                    startActivityForResult(Intent.createChooser(Intent().apply {
                        type = "image/*"
                        action = Intent.ACTION_GET_CONTENT
                    }, "Select Picture"), REQUEST_CODE)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("data check cancelled", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        })
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            if (data !=null && data.data !=null){
                imageUri = data.data!!
                FirebaseDatabase.getInstance().getReference("userInfo").child(
                    user.uid
                ).child("avatar").setValue(imageUri.toString())
                userAvatar.setImageURI(imageUri)
            }
        }
    }




    private fun setupNavDrawer() {
        binding.navView.setNavigationItemSelectedListener {
            if (it.itemId == R.id.sign_out) {
                val alertDialog = AlertDialog.Builder(this@HomeActivity)
                alertDialog.setTitle("Sign out")
                    .setMessage("Do you want to sign out?")
                    .setPositiveButton("SIGN OUT") { dialog, id ->
                        FirebaseAuth.getInstance().signOut()
                        startActivity(
                            Intent(
                                this,
                                LoginActivity::class.java
                            ).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        )
                        finish()
                    }
                    .setNegativeButton("CANCEL") { dialog, id -> dialog.dismiss() }
                    .setCancelable(false).show()
            }
            true
        }
    }



    private fun checkPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }



    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            Toast.makeText(
                this,
                "XXX",
                Toast.LENGTH_LONG
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE
            )
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.e("value", "Permission granted");
        } else {
            Log.e("value", "Permission deny");
        }
    }



    private fun getListSongData() {
        if (!listLoadDataFavorite.contains(getListLoadDataFavorite())) {
            listLoadDataFavorite.addAll(getListLoadDataFavorite())
        }
    }


    private fun setListLoadDataFavorite(listFavouriteSong: MutableList<SongModel>) {
        val gson = Gson()
        val jsonArray = gson.toJsonTree(listFavouriteSong).asJsonArray
        val strJsonArray = jsonArray.toString()
        editor.putString(PREF_LIST_USER, strJsonArray)
        editor.apply()
    }

    fun getListLoadDataFavorite(): MutableList<SongModel> {
        val strJsonArray = sharedPreferences.getString(PREF_LIST_USER, "0")
        val listLoadDataFavorite: MutableList<SongModel> = mutableListOf()
        if (strJsonArray != "0") {
            jsonArray = JSONArray(strJsonArray)
            val gson: Gson = Gson()
            for (i in 0 until jsonArray.length()) {
                val jsonObjects: JSONObject = jsonArray.getJSONObject(i)
                val songModel: SongModel =
                    gson.fromJson(jsonObjects.toString(), SongModel::class.java)
                if (!listLoadDataFavorite.contains(songModel)) {
                    listLoadDataFavorite.add(songModel)
                }
            }
        }
        return listLoadDataFavorite

    }

    private fun onStartService() {
        val intent = Intent(this, SongService::class.java)
        startService(intent)
    }



    private fun onStopService() {
        val intent = Intent(this, SongService::class.java)
        stopService(intent)
    }



    private fun getValueCurrentSong() {
        FirebaseDatabase.getInstance().getReference("status").child("pause").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                isPause =
                    snapshot.value.toString()
                statusActionPauseOrResume(isPause.toBoolean())
            }


            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
        songInfoReference.child("currentSong").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    typePosition = snapshot.child("type").value.toString()
                    val currentSongName: String =
                        snapshot.child("songName").value.toString()
                    val currentSingerName: String =
                        snapshot.child("artistName").value.toString()
                    val imageUrl: String =
                        snapshot.child("imgUrl").value.toString()
                    val isFavorite =
                        snapshot.child("favorite").value.toString().toBoolean()
//                    val isRepeat: snapshot.child("favorite").value.toString().toBoolean()
                    setUpMiniPlayer(
                        typePosition,
                        currentSongName,
                        currentSingerName,
                        imageUrl,
                        isFavorite,
                        isPause
                    )

                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }

    private fun setUpMiniPlayer(
        typePosition: String,
        currentSongName: String,
        currentSingerName: String,
        imageUrl: String,
        isFavorite: Boolean,
        isPause: String
    ) {
        if(typePosition.equals("album")) {
            val img: ByteArray = getImageUri(imageUrl!!)
            Glide.with(applicationContext)
                .load(img)
                .placeholder(R.drawable.image_default)
                .error(R.drawable.image_default)
                .into(binding.playImgSong)
        } else {
            Glide.with(applicationContext)
                .load(imageUrl)
                .placeholder(R.drawable.image_default)
                .error(R.drawable.image_default)
                .into(binding.playImgSong)
        }
        binding.playSongName.text = currentSongName
        binding.playSingerName.text = currentSingerName
        if (isFavorite) {
                            binding.playImgFavorite.setImageResource(R.drawable.ic_favorite)
                        } else {
                            binding.playImgFavorite.setImageResource(R.drawable.ic_favorite_border)
                        }
        isFirstTime = false
        statusActionPauseOrResume(isPause.toBoolean())

    }

    private fun animationImageView(duration: Long) {
        val runnable: Runnable = object : Runnable {
            override fun run() {
                binding.playImgSong.animate().rotationBy(360F).withEndAction(this).setDuration(duration)
                    .setInterpolator(LinearInterpolator()).start()
            }
        }
        binding.playImgSong.animate().rotationBy(360F).withEndAction(runnable).setDuration(duration)
            .setInterpolator(LinearInterpolator()).start()
    }



    fun getImageUri(uri: String): ByteArray {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(uri)
        val art: ByteArray? = retriever.embeddedPicture
        retriever.release()
        return art!!
    }



    private fun handleLayoutMusic(action: Int, isPause: Boolean) {
        when(action) {
            SongService.ACTION_PAUSE -> statusActionPauseOrResume(isPause)
            SongService.ACTION_RESUME -> statusActionPauseOrResume(isPause)
            SongService.ACTION_NEXT -> statusActionPauseOrResume(isPause)
        }
    }



    private fun statusActionPauseOrResume(isPause: Boolean) {
        if(isPause) {
            binding.playImgPlay.setImageResource(R.drawable.ic_play)
            animationImageView(0)
        } else {
            binding.playImgPlay.setImageResource(R.drawable.ic_pause)
            animationImageView(10000)
        }
    }





    private fun sendActionToService() {
        val intent = Intent(this, SongService::class.java)
        binding.playImgNext.setOnClickListener() {
            intent.putExtra("action_music_service", SongService.ACTION_NEXT)
            startService(intent)
        }

        binding.playImgPlay.setOnClickListener() {
            if(isPause.toBoolean()) {
                    intent.putExtra("action_music_service", SongService.ACTION_RESUME)
                    startService(intent)
            } else {
                intent.putExtra("action_music_service", SongService.ACTION_PAUSE)
                startService(intent)
            }
        }

    }




}