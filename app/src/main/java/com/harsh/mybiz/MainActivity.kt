package com.harsh.mybiz

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.harsh.mybiz.fragments.AnalysisFragment
import com.harsh.mybiz.fragments.ProductsFragment
import com.harsh.mybiz.fragments.ProfileFragment
import com.harsh.mybiz.fragments.SaleFragment
import com.harsh.mybiz.fragments.StockFragment
import com.pnpbook.adapters.ExpandableSalesAdapter

class MainActivity : AppCompatActivity() {
    lateinit var bNavMain : BottomNavigationView
    var lastNavigatedMenuItemId : Int = -1
    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = resources.getColor(R.color.bg)
        window.navigationBarColor = resources.getColor(R.color.nav_bg)

        bNavMain = findViewById(R.id.bottom_nav_main)
        bNavMain.setOnNavigationItemSelectedListener { menuItem ->
            if(menuItem.itemId == R.id.mi_sale && lastNavigatedMenuItemId!=menuItem.itemId){
                setFragment(0)
            }else if(menuItem.itemId == R.id.mi_products && lastNavigatedMenuItemId!=menuItem.itemId){
                setFragment(1)
            }else if(menuItem.itemId == R.id.mi_stock && lastNavigatedMenuItemId!=menuItem.itemId){
                setFragment(2)
            }else if(menuItem.itemId == R.id.mi_analysis && lastNavigatedMenuItemId!=menuItem.itemId){
                setFragment(3)
            }else if(menuItem.itemId == R.id.mi_profile && lastNavigatedMenuItemId!=menuItem.itemId){
                setFragment(4)
            }
            lastNavigatedMenuItemId = menuItem.itemId
            return@setOnNavigationItemSelectedListener true
        }
    }

    override fun onResume() {
        super.onResume()
        if(bNavMain.selectedItemId==R.id.mi_sale){
            setFragment(0)
        }else if(bNavMain.selectedItemId==R.id.mi_products){
            setFragment(1)
        }else if(bNavMain.selectedItemId==R.id.mi_stock){
            setFragment(2)
        }else if(bNavMain.selectedItemId==R.id.mi_analysis){
            setFragment(3)
        }
        else if(bNavMain.selectedItemId==R.id.mi_analysis){
            setFragment(4)
        }
    }
    fun setFragment(fragIndex : Int){
        val fm : FragmentManager = supportFragmentManager
        val ft : FragmentTransaction = fm.beginTransaction()
        if(fragIndex==0){
            ft.replace(R.id.fl_main, SaleFragment())
        }else if(fragIndex==1){
            ft.replace(R.id.fl_main, ProductsFragment())
        }else if(fragIndex==2){
            ft.replace(R.id.fl_main, StockFragment())
        }else if(fragIndex==3){
            ft.replace(R.id.fl_main, AnalysisFragment())
        }else if(fragIndex==4){
            ft.replace(R.id.fl_main, ProfileFragment())
        }
        ft.commit()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(!hasFocus && ExpandableSalesAdapter.isShared){
            Handler().postDelayed(Runnable {
                try{
                    startActivity(Intent(this, MainActivity::class.java))
                }catch (ex : Exception){
                    Log.d("dalle", ex.toString())
                }
            }, 30000)
        }
    }
}