package com.example.cherry.message

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.cherry.MainActivity
import com.example.cherry.R
import com.example.cherry.auth.UserDataModel
import com.example.cherry.setting.MyPageActivity
import com.example.cherry.setting.SettingActivity
import com.example.cherry.utils.FirebaseRef
import com.example.cherry.utils.FirebaseUtils
import com.example.cherry.utils.MyInfo
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

//matching list
class MyLikeListActivity : AppCompatActivity() {
    private val uid=FirebaseUtils.getUid()

    //user's like to other user's uid
    private val likeUserListUid= mutableListOf<String>()
    //user's like to other user
    private val likeUserList= mutableListOf<UserDataModel>()

    lateinit var listViewAdapter : ListViewAdapter

    lateinit var mDbRef: DatabaseReference
    private lateinit var senderRoom: String
    private lateinit var receiverRoom: String
    private lateinit var receiverUid: String

    //reciever's uid
    lateinit var getterUid : String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_like_list)

        val userListView=findViewById<ListView>(R.id.userListView)

        //connect adapter to listview
        listViewAdapter=ListViewAdapter(this,likeUserList)
        userListView.adapter=listViewAdapter

        //person who i like
        getMyLikeList()
        /*
        //if click textview, check matching
        userListView.setOnItemClickListener { parent,view,position,id ->
            checkMatching(likeUserList[position].uid.toString())
        }

        */

        //listview longclick
        userListView.setOnItemClickListener { parent,view,position,id ->
            checkMatching(likeUserList[position].uid.toString())
            getterUid = likeUserList[position].uid.toString()
        }

        /*
        //sort by name
        val sortBtnByName = findViewById<ImageView>(R.id.sortbyname)
        sortBtnByName.setOnClickListener{
            sortbynamedown()
        }

        //sort by age
        val sortBtnByAge = findViewById<ImageView>(R.id.sortbyage)
        sortBtnByAge.setOnClickListener{
            sortbyagedown()
        }

        //sort by name down
        val sortBtnByNameDown = findViewById<ImageView>(R.id.sortbynameDown)
        sortBtnByNameDown.setOnClickListener{
            sortbyname()
        }

        //sort by age down
        val sortBtnByAgeDown = findViewById<ImageView>(R.id.sortbyageDown)
        sortBtnByAgeDown.setOnClickListener{
            sortbyage()
        }

        //back
        val backBtn = findViewById<ImageView>(R.id.back)
        backBtn.setOnClickListener{
            val intent_main = Intent(this, MainActivity::class.java)
            startActivity(intent_main)
        }
        */

        //mypage option
        val mypage=findViewById<ImageView>(R.id.my_like_mypage)
        mypage.setOnClickListener{
            val intent_mypage=Intent(this, MyPageActivity::class.java)
            startActivity(intent_mypage)
        }

        //chatting option
        val chatting=findViewById<ImageView>(R.id.my_like_chatting)
        chatting.setOnClickListener{
            val intent_chatting=Intent(this, MyMsgActivity::class.java)
            startActivity(intent_chatting)
        }

        //main option
        val main=findViewById<ImageView>(R.id.my_like_main_btn)
        main.setOnClickListener{
            val intent_main=Intent(this, MainActivity::class.java)
            startActivity(intent_main)
        }
    }

    //sort by name down
    private fun sortbynamedown(){
        likeUserList.sortByDescending{it.name}
        listViewAdapter.notifyDataSetChanged()
    }

    //sort by age down
    private fun sortbyagedown(){
        likeUserList.sortByDescending{it.age}
        listViewAdapter.notifyDataSetChanged()
    }


    //sort by name
    private fun sortbyname(){
        likeUserList.sortBy{it.name}
        listViewAdapter.notifyDataSetChanged()
    }

    //sort by age
    private fun sortbyage(){
        likeUserList.sortBy{it.age}
        listViewAdapter.notifyDataSetChanged()
    }

    private fun checkMatching(otherUid : String){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //key -> uid, value -> other data

                //if liker is empty
                if(dataSnapshot.children.count() == 0){
                    Toast.makeText(this@MyLikeListActivity, "매칭된 상대가 아닙니다!", Toast.LENGTH_SHORT).show()
                }
                //if liker hava data
                else {
                    var check_matching = false
                    for (dataModel in dataSnapshot.children) {
                        //check matching
                        if(dataModel.key.toString().equals(uid))
                            check_matching=true
                    }
                    if(check_matching){
                        //dialog
                        receiverUid = otherUid
                        showDialog()
                    }
                    else{
                        Toast.makeText(
                            this@MyLikeListActivity,
                            "매칭된 상대가 아닙니다!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        FirebaseRef.userLikeRef.child(otherUid).addValueEventListener(postListener)
    }

    //person who i like
    private fun getMyLikeList(){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataModel in dataSnapshot.children){
                    likeUserListUid.add(dataModel.key.toString())
                }

                //get all user's data
                getUserDataList()
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        }
        FirebaseRef.userLikeRef.child(uid).addValueEventListener(postListener)
    }

    //get all user's data
    private fun getUserDataList(){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dataModel in dataSnapshot.children){
                    val user = dataModel.getValue(UserDataModel::class.java)

                    //we can know person who user like
                    if(likeUserListUid.contains(user?.uid)){
                        likeUserList.add(user!!)
                    }
                    //notify to listview that info is changed
                    listViewAdapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        }

        //get resource from
        FirebaseRef.userInfoRef.addValueEventListener(postListener)
    }

    //Dialog
    private fun showDialog(){
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog,null)
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("메시지 보내기")

        val mAlertDialog = mBuilder.show()

        val btn = mAlertDialog.findViewById<ImageView>(R.id.sendBtnArea)
        val textArea = mAlertDialog.findViewById<EditText>(R.id.sendTextArea)
        btn?.setOnClickListener {
            var messageUid = UUID.randomUUID().toString()
            if (messageUid != null) {
                val message = textArea!!.text.toString()
                val messageObject = Message(message, uid)

                mDbRef = FirebaseDatabase.getInstance().reference

                // 보낸이방
                senderRoom = receiverUid + uid
                receiverRoom = uid + receiverUid

                // 데이터 저장
                mDbRef.child("chats").child(senderRoom).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        // 저장 성공하면
                        mDbRef.child("chats").child(receiverRoom).child("messages").push()
                            .setValue(messageObject)
                    }
            }
            //off dialog
            mAlertDialog.dismiss()
        }
    }
}