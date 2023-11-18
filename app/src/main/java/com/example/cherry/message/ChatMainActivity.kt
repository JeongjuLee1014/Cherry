package com.example.cherry.message

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cherry.R
import com.example.cherry.auth.UserDataModel
import com.example.cherry.databinding.ActivityChatMainBinding
import com.example.cherry.utils.FirebaseRef
import com.google.android.play.integrity.internal.t
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlin.concurrent.thread

class ChatMainActivity : AppCompatActivity() {
    lateinit var binding: ActivityChatMainBinding
    lateinit var adapter: UserAdapter

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    private lateinit var userList: ArrayList<UserDataModel>

    val nameMap = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 인증 초기화
        mAuth = Firebase.auth

        // db 초기화
        mDbRef = Firebase.database.reference

        // 리스트 초기화
        userList = ArrayList()

        adapter = UserAdapter(this, userList)

        binding.userRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.userRecyclerView.adapter = adapter

        mDbRef.child("userInfo").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                GlobalScope.launch(Dispatchers.Main) {
                    for (postSnapshot in snapshot.children) {
                        val currentUser = postSnapshot.getValue(UserDataModel::class.java)
                        if (mAuth.currentUser?.uid != currentUser?.uid) {
                            // 비동기 작업을 코루틴으로 감싸고, 작업이 완료될 때까지 대기
                            val likeList = getLikeList(currentUser!!.uid!!)
                            if(isMatched(likeList)) {
                                userList.add(currentUser!!)
                                nameMap.put(currentUser!!.name!!, currentUser!!.uid!!)
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onCancelled(error: DatabaseError) { }
        })
    }

    suspend fun getLikeList(userId: String): List<String> {
        return mDbRef.child("userLike").child(userId).get().await().children.mapNotNull {
            it.getValue(String::class.java)
        }
    }

    private fun isMatched(likeList: List<String>): Boolean {
        // uid가 uid인 사람의 좋아요 목록을 돌다가 접속한 사람의 uid가 있으면 true를 반환
        for(like in likeList) {
            Log.v(mAuth.currentUser?.uid, like)
            if(mAuth.currentUser?.uid != like) {
                return true
            }
        }
        return false
    }

    private fun searchUser() {
        //input
        val searchEditText = findViewById<EditText>(R.id.search_EditText)

        val msgBuilder = AlertDialog.Builder(this)
            .setPositiveButton("확인") { dialogInterface, i -> }
        val targetName : String = searchEditText.text.toString()

        if(nameMap.containsKey(targetName)){
            val getterUid = nameMap[targetName]!!

            msgBuilder.setTitle("검색 성공")
            msgBuilder.setMessage(targetName + "님을 찾았습니다")
        }
        else{
            msgBuilder.setTitle("검색 오류")
            msgBuilder.setMessage("이름을 찾을 수 없습니다")
        }
    }
}

