package com.harsh.mybiz

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.harsh.mybiz.fragments.LoginFragment
import com.harsh.mybiz.fragments.RegistrationFragment

class AuthenticationActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        flAuth = findViewById(R.id.fl_authentication)
        fm = supportFragmentManager
        setFragment(0)
    }

    @SuppressLint("CommitTransaction")
    companion object{
        fun setFragment(fragIndex : Int){
            val ft : FragmentTransaction = fm.beginTransaction()
            val animLeftToRight = TranslateAnimation(-500.0F, 0.0F, 0.0F, 0.0F)
            val animRightToLeft = TranslateAnimation(500.0F, 0.0F, 0.0F, 0.0F)
            animLeftToRight.duration = 500
            animRightToLeft.duration = 500
            if(fragIndex==0){
                flAuth.startAnimation(animLeftToRight)
                ft.replace(R.id.fl_authentication, LoginFragment())
            }else if(fragIndex==1){
                flAuth.startAnimation(animRightToLeft)
                ft.replace(R.id.fl_authentication, RegistrationFragment())
            }
            ft.commit()
        }
        lateinit var fm : FragmentManager
        @SuppressLint("StaticFieldLeak")
        lateinit var flAuth : FrameLayout
    }

}