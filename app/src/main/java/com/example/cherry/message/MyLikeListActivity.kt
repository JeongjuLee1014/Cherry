package com.example.cherry.message

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import com.example.cherry.R
import com.example.cherry.auth.UserDataModel
import com.example.cherry.utils.FirebaseRef
import com.example.cherry.utils.FirebaseUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

//matching list
class MyLikeListActivity : AppCompatActivity() {
    private val uid=FirebaseUtils.getUid()

    //user's like to other user's uid
    private val likeUserListUid= mutableListOf<String>()
    //user's like to other user
    private val likeUserList= mutableListOf<UserDataModel>()

    lateinit var listViewAdapter : ListViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_like_list)

        val userListView=findViewById<ListView>(R.id.userListView)

        //connect adapter to listview
        listViewAdapter=ListViewAdapter(this,likeUserList)
        userListView.adapter=listViewAdapter

        //person who i like
        getMyLikeList()

        //if click textview, check matching
        userListView.setOnItemClickListener { parent,view,position,id ->
            checkMatching(likeUserList[position].uid.toString())
        }
    }

    private fun checkMatching(otherUid : String){
        val postListener = object : ValueEventListener {
            //dataSnapshot : firebase instore data
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                //key -> uid, value -> other data

                //if liker is empty
                if(dataSnapshot.children.count() == 0){
                    Toast.makeText(this@MyLikeListActivity, "매칭된 상대가 아닙니다!", Toast.LENGTH_LONG).show()
                }
                //if liker hava data
                else {
                    for (dataModel in dataSnapshot.children) {
                        //check matching
                        if (dataModel.key.toString().equals(uid)) {
                            Toast.makeText(this@MyLikeListActivity, "매칭된 상대입니다!", Toast.LENGTH_LONG)
                                .show()
                        } else {
                            Toast.makeText(
                                this@MyLikeListActivity,
                                "매칭된 상대가 아닙니다!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
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
}