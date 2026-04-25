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
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.models.SaleEntryModel
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        if (isConnectedToInternet()) {
            postInternetConnection()
        } else {
            val adb: AlertDialog.Builder = AlertDialog.Builder(this, R.style.myAlertDialogTheme)
            adb.setCancelable(false)
            adb.setTitle("No network?")
            adb.setMessage("Please connect to internet to continue...")
            adb.setIcon(resources.getDrawable(R.drawable.ic_no_network))
            var ad: AlertDialog = adb.create()
            adb.setPositiveButton("RETRY") { dialog, which ->
                if (isConnectedToInternet()) {
                    postInternetConnection()
                } else {
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

    fun postInternetConnection() {
        Handler().postDelayed(Runnable {
            try {
                Constants.initialize()
                if (Constants.fbAuth.currentUser == null) {
                    startActivity(Intent(this, AuthenticationActivity::class.java))
                    finish()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            } catch (ex: Exception) {
                Log.d("dalle", ex.toString())
            }
        }, 1000)
    }

    companion object {
        fun preloadSalesAndProducts(onComplete: (() -> Unit)? = null) {
            // Load both products and sales once
            val businessRef = Constants.fbStore.collection("businesses").document(Constants.uID)
//            Getting cached business name
            businessRef.get().addOnSuccessListener { res ->
                Constants.cachedBusinessName = res.getString("name").toString()
            }

            val taskProducts = businessRef.collection("products").get()
            val taskSales = businessRef.collection("sales").get()
            val taskStocks = businessRef.collection("stocks").get()

            Tasks.whenAllSuccess<QuerySnapshot>(taskProducts, taskSales, taskStocks)
                .addOnSuccessListener { results ->
                    val productsSnap = results[0]
                    val salesSnap = results[1]
                    val stocksSnap = results[2]

                    // Cache products
                    Constants.alProductsOptimized.clear()
                    for (doc in productsSnap) {
                        Constants.logThis(doc.toString())
                        Constants.alProductsOptimized.add(
                            ProductModel(
                                doc.getString("id").toString(),
                                doc.getString("name").toString(),
                                doc.getString("price")!!.toDouble(),
                                doc.id,
                                (Constants.isDeleted(doc))
                            )
                        )
                    }
                    // Cache products
                    Constants.alStocksCached.clear()
                    for (stock in stocksSnap) {
                        Constants.alStocksCached.add(
                            StockModel(
                                stock.getString("id").toString(),
                                stock.getString("name").toString(),
                                stock.getString("price")!!.toDouble(),
                                stock.getString("date").toString(),
                                stock.id
                            )
                        )
                    }
                    Constants.alStocksCached.sortWith(
                        Comparator.comparing(StockModel::date).reversed()
                    )

                    // Load all subcollections under sales/{date}/sales
                    Constants.alSalesCached.clear()
                    val subTasks = mutableListOf<Task<QuerySnapshot>>()
                    for (saleDoc in salesSnap) {
                        val dateId = saleDoc.id
                        subTasks.add(
                            businessRef.collection("sales").document(dateId).collection("sales")
                                .get()
                        )
                    }



                    Tasks.whenAllSuccess<QuerySnapshot>(subTasks).addOnSuccessListener { allSales ->
                        for ((i, saleSet) in allSales.withIndex()) {
                            val date = salesSnap.documents[i].id
                            for (sale in saleSet) {
                                val rawQuantity = sale.get("quantity")
                                val productId = sale.getString("id")

                                val quantity = when (rawQuantity) {
                                    is Long -> rawQuantity.toInt()
                                    is Double -> rawQuantity.toInt()
                                    is String -> rawQuantity.toIntOrNull()
                                    else -> null
                                }

                                if (quantity == null || productId == null) {
                                    Log.w("FIRESTORE_SKIP", "Skipping invalid sale: ${sale.id} → ${sale.data}")
                                    continue
                                }

                                Constants.alSalesCached.add(
                                    SaleEntryModel(
                                        date = date,
                                        productId = productId,
                                        quantity = quantity
                                    )
                                )
                            }
                        }
                        onComplete?.invoke()
                    }
                }
        }
    }
}