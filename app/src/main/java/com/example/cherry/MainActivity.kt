package com.example.cherry

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.example.cherry.auth.IntroActivity
import com.example.cherry.slider.CardStackAdapter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.example.cherry.auth.UserDataModel
import com.example.cherry.setting.MyPageActivity
import com.example.cherry.setting.SettingActivity
import com.example.cherry.utils.FirebaseRef
import com.example.cherry.utils.FirebaseUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.ktx.storage

class MainActivity : AppCompatActivity() {
    lateinit var cardstackAdapter : CardStackAdapter
    lateinit var manager : CardStackLayoutManager

    //control userdata
    private val usersDataList= mutableListOf<UserDataModel>()

    //for refresh user
    private var userCount = 0

    //user's uid
    private val uid = FirebaseUtils.getUid()

    private lateinit var UserGender : String
    private lateinit var UserLocation : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //logout option
        val logout=findViewById<ImageView>(R.id.logout)
        logout.setOnClickListener{
            val auth= Firebase.auth
            auth.signOut()

            val intent_intro=Intent(this,IntroActivity::class.java)
            startActivity(intent_intro)
        }

        //setting option
        val mypage=findViewById<ImageView>(R.id.mypage)
        mypage.setOnClickListener{
            val intent_setting=Intent(this, SettingActivity::class.java)
            startActivity(intent_setting)
        }

        //set cardstackview same location
        val sameLocationBtn=findViewById<Button>(R.id.filter_samelocation)
        sameLocationBtn.setOnClickListener {
            setFilterSameLocation()
        }

        //set cardstackview all location
        val allLocationBtn=findViewById<Button>(R.id.filter_alllocation)
        allLocationBtn.setOnClickListener {
            getUserDataList(UserGender)
        }

        //cardstackview
        val cardStackView = findViewById<CardStackView>(R.id.cardStackView)

        manager = CardStackLayoutManager(baseContext, object : CardStackListener {
            override fun onCardDragging(direction: Direction?, ratio: Float) {

            }
            override fun onCardSwiped(direction: Direction?) {
                //if give like to other user
                if(direction==Direction.Right){
                    userLikeOtherUser(uid,usersDataList[userCount].uid.toString())
                }

                userCount = userCount+1

                //if user is over
                if(userCount==usersDataList.count()){
                    getUserDataList(UserGender)
                    Toast.makeText(this@MainActivity, "유저 새롭게 받아옵니다!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCardRewound() {

            }

            override fun onCardCanceled() {

            }

            override fun onCardAppeared(view: View?, position: Int) {

            }

            override fun onCardDisappeared(view: View?, position: Int) {

            }
        })

        //adapter accept
        cardstackAdapter=CardStackAdapter(baseContext,usersDataList)
        cardStackView.layoutManager=manager
        cardStackView.adapter=cardstackAdapter

        //get user's data for matching other gender
        getMyUserData()
    }

    //set cardlistview samelocation
    private fun setFilterSameLocation() {
        val filteredUsersDataList = usersDataList.filter { it.location == UserLocation }.toMutableList()

        usersDataList.clear()
        usersDataList.addAll(filteredUsersDataList)

        //sync_adapter
        cardstackAdapter.notifyDataSetChanged()
    }


    //user's like log
    private fun userLikeOtherUser(myUid : String , otherUid: String){
        //instore firebase
        FirebaseRef.userLikeRef.child(myUid).child(otherUid).setValue("true")

        getOtherUserLikeList(otherUid)
    }

    //to check other user's likelist(for matching)
    private fun getOtherUserLikeList(otherUid: String){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //show one by one(cardstackview)
                //key -> uid, value -> other data
                for (dataModel in dataSnapshot.children){
                    if(dataModel.key.toString().equals(uid)){
                        Toast.makeText(this@MainActivity, "매칭 완료", Toast.LENGTH_LONG).show()

                        //Notification
                        createNotificationChannel()
                        sendNotification()
                    }
                }

            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        FirebaseRef.userLikeRef.child(otherUid).addValueEventListener(postListener)
    }

    //get user's data for matching other gender
    private fun getMyUserData(){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val data = dataSnapshot.getValue(UserDataModel::class.java)
                UserGender = data?.gender.toString()
                UserLocation = data?.location.toString()

                //show card in cardstackview
                getUserDataList(UserGender)
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        }
        FirebaseRef.userInfoRef.child(uid).addValueEventListener(postListener)
    }

    //get all user's data
    private fun getUserDataList(CurrentUserGender : String){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //show one by one(cardstackview)
                //key -> uid, value -> other data
                for (dataModel in dataSnapshot.children){
                    val user = dataModel.getValue(UserDataModel::class.java)

                    if(user!!.gender.toString().equals(CurrentUserGender)){
                        //if same gender dont have to instore
                    }
                    else{
                        usersDataList.add(user!!)
                    }
                }
                //sync_adapter
                cardstackAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        FirebaseRef.userInfoRef.addValueEventListener(postListener)
    }

    //notification
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "name"
            val descriptionText = "descript"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("Cherry", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(){
        try {
            var builder = NotificationCompat.Builder(this, "Cherry")
                .setSmallIcon(R.drawable.cherry)
                .setContentTitle("매칭완료")
                .setContentText("매칭이 완료되었습니다 지금 바로 확인해보세요!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(this)) {
                notify(123, builder.build())
            }
        }
        catch(e:SecurityException){

        }
    }
}