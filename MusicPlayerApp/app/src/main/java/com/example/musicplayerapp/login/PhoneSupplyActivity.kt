package com.example.musicplayerapp.login

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.musicplayerapp.R
import com.example.musicplayerapp.databinding.ActivityPhoneSupplyBinding
import com.example.musicplayerapp.model.UserInfoModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


class PhoneSupplyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhoneSupplyBinding
    private lateinit var model: UserInfoModel
    private lateinit var database: FirebaseDatabase
    private lateinit var userInForReference: DatabaseReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_phone_supply)
        model = UserInfoModel()
        checkPhoneNumber()
        binding.btnSignInPhone.setOnClickListener() {
            updateDriveInfo()
        }
    }

    private fun checkPhoneNumber() {
        binding.txtPhone.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // TODO Auto-generated method stub
            }

            override fun afterTextChanged(s: Editable) {
                if (s.length > 8) {
                    binding.btnSignInPhone.visibility = View.VISIBLE
                } else {
                    binding.btnSignInPhone.visibility = View.GONE
                }
            }
        })

    }

    private fun updateDriveInfo() {
        val user = FirebaseAuth.getInstance().currentUser
        database = FirebaseDatabase.getInstance()
        userInForReference = database.getReference("userInfo")
        if (user != null) {
            model.avatar = user.photoUrl!!.toString()
            model.displayName = user.displayName!!
            model.email = user.email!!
            model.phoneNumber =
                binding.ccp.selectedCountryCodeWithPlus.toString() + binding.txtPhone.text.toString()
            userInForReference.child(user.uid).setValue(model).addOnCompleteListener { e ->
                finish()
            }
                .addOnFailureListener { e ->
                    Log.e("Error", "${e.message}")
                }
        }
    }
}