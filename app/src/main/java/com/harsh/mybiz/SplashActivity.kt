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
import java.util.Calendar


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

        /** Number of months to load per page */
        private const val PAGE_MONTHS = 3

        /**
         * Initial load: fetches products + stocks once, then loads the most recent
         * [PAGE_MONTHS] months of sales. Call this from SplashActivity on first launch.
         */
        fun preloadSalesAndProducts(onComplete: (() -> Unit)? = null) {
            val businessRef = Constants.fbStore.collection("businesses").document(Constants.uID)

            // Cache business name
            businessRef.get().addOnSuccessListener { res ->
                Constants.cachedBusinessName = res.getString("name").toString()
            }

            val taskProducts = businessRef.collection("products").get()
            val taskStocks   = businessRef.collection("stocks").get()

            Tasks.whenAllSuccess<QuerySnapshot>(taskProducts, taskStocks)
                .addOnSuccessListener { results ->
                    val productsSnap = results[0]
                    val stocksSnap   = results[1]

                    // Cache products
                    Constants.alProductsOptimized.clear()
                    for (doc in productsSnap) {
                        Constants.alProductsOptimized.add(
                            ProductModel(
                                doc.getString("id").toString(),
                                doc.getString("name").toString(),
                                doc.getString("price")!!.toDouble(),
                                doc.id,
                                Constants.isDeleted(doc)
                            )
                        )
                    }

                    // Cache stocks
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

                    // Reset pagination state and load first page of sales
                    Constants.alSalesCached.clear()
                    Constants.allSalesLoaded = false

                    val now = Calendar.getInstance()
                    val windowEnd   = Constants.calendarToSalesKey(now)
                    now.add(Calendar.MONTH, -PAGE_MONTHS)
                    val windowStart = Constants.calendarToSalesKey(now)

                    // Store cursor so next "Load More" knows where to continue from
                    Constants.salesPaginationCursor = windowStart

                    loadSalesWindow(windowStart, windowEnd, onComplete)
                }
        }

        /**
         * Loads the next [PAGE_MONTHS] months of sales going further back in time.
         * Safe to call repeatedly; sets [Constants.allSalesLoaded] when nothing more exists.
         */
        fun loadMoreSales(onComplete: (() -> Unit)? = null) {            if (Constants.allSalesLoaded) {
                onComplete?.invoke()
                return
            }

            val windowEnd = Constants.salesPaginationCursor  // exclusive upper bound of next fetch
            val cal = Calendar.getInstance()

            // Parse cursor back to a Calendar so we can subtract another PAGE_MONTHS
            // Cursor format: "sales_YYYY_MM_DD"
            try {
                val parts = windowEnd.removePrefix("sales_").split("_")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (e: Exception) {
                Log.w("PAGINATION", "Could not parse cursor: $windowEnd", e)
                Constants.allSalesLoaded = true
                onComplete?.invoke()
                return
            }

            val newEnd = Constants.calendarToSalesKey(cal)   // same as old start (exclusive)
            cal.add(Calendar.MONTH, -PAGE_MONTHS)
            val newStart = Constants.calendarToSalesKey(cal)

            Constants.salesPaginationCursor = newStart

            loadSalesWindow(newStart, newEnd, onComplete)
        }

        /**
         * Fetches all sale date-documents whose ID falls in [startKey, endKey) and
         * appends their sub-collection entries to [Constants.alSalesCached].
         */
        private fun loadSalesWindow(
            startKey: String,
            endKey: String,
            onComplete: (() -> Unit)?
        ) {
            val businessRef = Constants.fbStore.collection("businesses").document(Constants.uID)

            // Firestore document IDs sort lexicographically — "sales_YYYY_MM_DD" works perfectly
            val taskSales = businessRef.collection("sales")
                .whereGreaterThanOrEqualTo("__name__", startKey)
                .whereLessThanOrEqualTo("__name__", endKey)
                .get()

            taskSales.addOnSuccessListener { salesSnap ->
                if (salesSnap.isEmpty) {
                    // No documents in this window — mark as fully loaded
                    Constants.allSalesLoaded = true
                    onComplete?.invoke()
                    return@addOnSuccessListener
                }

                val subTasks = mutableListOf<Task<QuerySnapshot>>()
                for (saleDoc in salesSnap) {
                    subTasks.add(
                        businessRef.collection("sales")
                            .document(saleDoc.id)
                            .collection("sales")
                            .get()
                    )
                }

                Tasks.whenAllSuccess<QuerySnapshot>(subTasks).addOnSuccessListener { allSales ->
                    for ((i, saleSet) in allSales.withIndex()) {
                        val date = salesSnap.documents[i].id
                        for (sale in saleSet) {
                            val rawQuantity = sale.get("quantity")
                            val productId   = sale.getString("id")

                            val quantity = when (rawQuantity) {
                                is Long   -> rawQuantity.toInt()
                                is Double -> rawQuantity.toInt()
                                is String -> rawQuantity.toIntOrNull()
                                else      -> null
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
                }.addOnFailureListener { e ->
                    Log.e("FIRESTORE", "Failed loading sub-sales", e)
                    onComplete?.invoke()
                }
            }.addOnFailureListener { e ->
                Log.e("FIRESTORE", "Failed loading sales window", e)
                onComplete?.invoke()
            }
        }

        // ─────────────────────────── STOCKS PAGINATION ───────────────────────────

        /**
         * Initial stock load: fetches the most recent [PAGE_MONTHS] months.
         * Called from [preloadSalesAndProducts] after products/stocks are first cached,
         * and also directly when StockFragment needs a fresh first page.
         */
        fun preloadStocks(onComplete: (() -> Unit)? = null) {
            Constants.alStocksCached.clear()
            Constants.allStocksLoaded = false

            val now = Calendar.getInstance()
            val windowEnd   = Constants.calendarToDateString(now)
            now.add(Calendar.MONTH, -PAGE_MONTHS)
            val windowStart = Constants.calendarToDateString(now)

            Constants.stocksPaginationCursor = windowStart
            loadStocksWindow(windowStart, windowEnd, onComplete)
        }

        /**
         * Loads the next [PAGE_MONTHS] months of stocks going further back in time.
         */
        fun loadMoreStocks(onComplete: (() -> Unit)? = null) {
            if (Constants.allStocksLoaded) {
                onComplete?.invoke()
                return
            }

            val windowEnd = Constants.stocksPaginationCursor
            val cal = Calendar.getInstance()
            try {
                val parts = windowEnd.split("-")
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (e: Exception) {
                Log.w("PAGINATION", "Could not parse stock cursor: $windowEnd", e)
                Constants.allStocksLoaded = true
                onComplete?.invoke()
                return
            }

            val newEnd   = Constants.calendarToDateString(cal)
            cal.add(Calendar.MONTH, -PAGE_MONTHS)
            val newStart = Constants.calendarToDateString(cal)

            Constants.stocksPaginationCursor = newStart
            loadStocksWindow(newStart, newEnd, onComplete)
        }

        /**
         * Fetches stocks whose "date" field falls in [startDate, endDate] and
         * appends them to [Constants.alStocksCached].
         * Stock dates are stored as plain "YYYY-MM-DD" strings.
         */
        private fun loadStocksWindow(
            startDate: String,
            endDate: String,
            onComplete: (() -> Unit)?
        ) {
            val businessRef = Constants.fbStore.collection("businesses").document(Constants.uID)

            businessRef.collection("stocks")
                .whereGreaterThanOrEqualTo("date", startDate)
                .whereLessThanOrEqualTo("date", endDate)
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) {
                        Constants.allStocksLoaded = true
                        onComplete?.invoke()
                        return@addOnSuccessListener
                    }
                    // Avoid duplicates if called multiple times
                    val existingDocIds = Constants.alStocksCached.map { it.docId }.toSet()
                    for (stock in snap) {
                        if (stock.id in existingDocIds) continue
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
                    Constants.alStocksCached.sortWith(Comparator.comparing(StockModel::date).reversed())
                    onComplete?.invoke()
                }
                .addOnFailureListener { e ->
                    Log.e("FIRESTORE", "Failed loading stocks window", e)
                    onComplete?.invoke()
                }
        }

        /**
         * Fetches stocks for an arbitrary date range without touching pagination state.
         * Used by AnalysisFragment to fill gaps in the cache for a user-selected range.
         */
        fun fetchStocksForRange(
            startDate: String,
            endDate: String,
            onComplete: (() -> Unit)?
        ) {
            loadStocksWindow(startDate, endDate, onComplete)
        }

        /**
         * Fetches sales for an arbitrary date range without touching pagination state.
         * Used by AnalysisFragment to fill gaps in the cache for a user-selected range.
         */
        fun fetchSalesForRange(
            startKey: String,
            endKey: String,
            onComplete: (() -> Unit)?
        ) {
            loadSalesWindow(startKey, endKey, onComplete)
        }
    }
}