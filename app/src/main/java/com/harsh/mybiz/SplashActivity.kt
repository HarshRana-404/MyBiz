package com.harsh.mybiz

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.harsh.mybiz.utilities.Constants


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if(isConnectedToInternet()){
            postInternetConnection()
        }else{
            val adb : AlertDialog.Builder = AlertDialog.Builder(this, R.style.myAlertDialogTheme)
            adb.setCancelable(false)
            adb.setTitle("No network?")
            adb.setMessage("Please connect to internet to continue...")
            adb.setIcon(resources.getDrawable(R.drawable.ic_no_network))
            var ad: AlertDialog = adb.create()
            adb.setPositiveButton("RETRY"){
                    dialog, which ->
                if(isConnectedToInternet()){
                    postInternetConnection()
                }else{
                    ad.dismiss()
                    adb.show()
                }
            }
            adb.show()
        }
    }
    fun isConnectedToInternet(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetworkInfo ?: return false
        return true
    }
    fun postInternetConnection(){
        Handler().postDelayed(Runnable {
            try{
                Constants.initialize()
                if(Constants.fbAuth.currentUser==null){
                    startActivity(Intent(this, AuthenticationActivity::class.java))
                    finish()
                }else{
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }catch (ex : Exception){
                Log.d("dalle", ex.toString())
            }
        }, 1000)
    }
}