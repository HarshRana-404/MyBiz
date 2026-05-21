package com.harsh.mybiz.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.models.SaleEntryModel
import com.harsh.mybiz.models.StockModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.HashMap
import java.util.Locale
import java.util.UUID


class Constants {
    companion object{
//        Optimized & Sales
        var alProductsOptimized: ArrayList<ProductModel> = ArrayList()
        val alSalesCached = mutableListOf<SaleEntryModel>()  // Flattened sales data
//        val alExpandableSalesCached = mutableListOf<ExpandableSalesModel>() // Aggregated per day
        var alStocksCached = ArrayList<StockModel>()
        lateinit var cachedBusinessName : String

        // Pagination state for sales
        // Tracks the start of the oldest window already loaded (e.g. "sales_2025_01_01")
        var salesPaginationCursor: String = ""
        // True once we've loaded all the way back to the very first sale doc
        var allSalesLoaded: Boolean = false

        // Pagination state for stocks
        // Stocks use a plain "YYYY-MM-DD" date field, cursor tracks oldest date loaded
        var stocksPaginationCursor: String = ""
        var allStocksLoaded: Boolean = false

        @SuppressLint("StaticFieldLeak")
        lateinit var fbStore : FirebaseFirestore
        lateinit var fbAuth : FirebaseAuth
        var uID : String = ""
        var pId : String = ""
        fun initialize(){
            fbStore = FirebaseFirestore.getInstance()
            fbAuth = FirebaseAuth.getInstance()
            if(fbAuth.currentUser!=null){
                uID = fbAuth.currentUser!!.uid
            }
        }
        fun toastThis(context: Context, title: String){
            Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
        }
        fun logThis(msg: String){
            Log.d("dalle", msg)
        }
        fun isDeleted(product: DocumentSnapshot): Boolean {
            val value = product.get("deleted")

            return when (value) {
                is Boolean -> value
                is String -> value.equals("true", ignoreCase = true)
                else -> false
            }
        }
        fun getDateTime(): String{
            val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd@HH:mm:ss", Locale.getDefault())
            val dateTime = sdf.format(Calendar.getInstance().getTime())
            return dateTime
        }
        fun getDateToShow(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[0].split("-")
            val date : String = d[2] + "-" + d[1] + "-" + d[0]
            return date
        }
        fun getYearMonthForSales(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[0].split("-")
            val date : String = d[0] + "_" + d[1]
            return date
        }
        fun getYearMonth(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[0].split("-")
            val date : String = d[0] + "-" + d[1]
            return date
        }
        fun getDocDateForExpandedSale(date: String): String{
            val dt = date.split("-")
            val date : String = dt[2] + "_" + dt[1] + "_" + dt[0]
            return date
        }
        fun getDocDateForQuantity(date: String): String{
            val dt = date.split("-")
            val date : String = dt[0] + "_" + dt[1] + "_" + dt[2]
            return "sales_$date"
        }
        fun getDocDateToShow(dateTime: String): String{
            val dt = dateTime.split("sales_")
            val d = dt[1].split("_")
            val date : String = d[2] + "-" + d[1] + "-" + d[0]
            return date
        }
        fun getDateForYYYYMMDD(dateTime: String): String{
            val dt = dateTime.split("sales_")
            val d = dt[1].split("_")
            val date : String = d[0] + "-" + d[1] + "-" + d[2]
            return date
        }
        fun getDateForYearMonth(dateTime: String): String{
            val dt = dateTime.split("sales_")
            val d = dt[1].split("_")
            val date : String = d[0] + "_" + d[1]
            return date
        }
        fun getDateForDB(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[0].split("-")
            val date : String = d[0] + "-" + d[1] + "-" + d[2]
            return date
        }
        fun getYearMonthForDatesTotal(dateTime: String): String{
            val dt = dateTime.split("sales_")
            val d = dt[1].split("_")
            val date : String = d[0]+ "-" + d[1]
            return date
        }
        fun getDateForSaleDocDB(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[0].split("-")
            val date : String = d[0] + "_" + d[1] + "_" + d[2]
            return date
        }
        fun getDateForExpandedDocDB(dateTime: String): String{
            val dt = dateTime.split("@")
            val d = dt[1].split("-")
            val date : String = d[2] + "_" + d[1] + "_" + d[0]
            return date
        }
        /** Returns a sales doc-ID key like "sales_2025_01_15" from a Calendar instance */
        fun calendarToSalesKey(cal: Calendar): String {
            val y = cal.get(Calendar.YEAR)
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            return "sales_${y}_${m}_${d}"
        }

        /** Returns a plain date string like "2025-01-15" from a Calendar instance (used for stocks) */
        fun calendarToDateString(cal: Calendar): String {
            val y = cal.get(Calendar.YEAR)
            val m = String.format("%02d", cal.get(Calendar.MONTH) + 1)
            val d = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
            return "$y-$m-$d"
        }

        fun getUUIDForText(str: String):String{
            var uuid: UUID? = null
            try {
                uuid = UUID.nameUUIDFromBytes(str.toLowerCase().toByteArray())
            } catch (e: Exception) {

            }
            return uuid.toString()
        }
        fun getUUIDForPassword(str: String):String{
            var uuid: UUID? = null
            try {
                uuid = UUID.nameUUIDFromBytes(str.toByteArray())
            } catch (e: Exception) {

            }
            return uuid.toString()
        }
    }
}
