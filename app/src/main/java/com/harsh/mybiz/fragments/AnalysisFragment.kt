package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.adapters.SalesBetweenAdapter
import com.harsh.mybiz.models.ExpandedSaleModel
import com.harsh.mybiz.models.ProductQuantityModel
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.utilities.Constants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AnalysisFragment : Fragment() {
    lateinit var tvMonthlySale: TextView
    lateinit var tvMonthlyStock: TextView
    lateinit var tvMostSoldProduct: TextView
    lateinit var tvDailyAverage: TextView
    lateinit var tvSalesDates: TextView
    lateinit var tvSalesBetweenTotal: TextView
    lateinit var btnSalesBetweenDates: Button
    lateinit var rvSalesBetween: RecyclerView
    lateinit var adapSalesBetween: SalesBetweenAdapter
    var alSalesBetween = ArrayList<ExpandedSaleModel>()
    var alProductsForSales = ArrayList<ProductModel>()
    var alDatesBetween = ArrayList<String>()
    var alProductQuantity = ArrayList<ProductQuantityModel>()
    var hmProductQuantity = HashMap<String, Int>()
    var hmMostSoldProduct = HashMap<String, Int>()
    var startDate: String = ""
    var startDateInMillis: Long = 0
    var endDate: String = ""
    var endDateInMillis: Long = 0

    @SuppressLint("MissingInflatedId", "SetTextI18n", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragAnalysis : View = inflater.inflate(R.layout.fragment_analysis, container, false)
        tvMonthlySale = fragAnalysis.findViewById(R.id.tv_sale_monthly_value)
        tvMonthlyStock = fragAnalysis.findViewById(R.id.tv_stock_monthly_value)
        tvMostSoldProduct = fragAnalysis.findViewById(R.id.tv_most_selling_product)
        tvDailyAverage = fragAnalysis.findViewById(R.id.tv_daily_average)
        tvSalesDates = fragAnalysis.findViewById(R.id.tv_sale_dates)
        tvSalesBetweenTotal = fragAnalysis.findViewById(R.id.tv_sale_between_total)
        btnSalesBetweenDates = fragAnalysis.findViewById(R.id.btn_sales_between_dates)
        rvSalesBetween = fragAnalysis.findViewById(R.id.rv_sales_between_dates)
        adapSalesBetween = SalesBetweenAdapter(fragAnalysis.context, alSalesBetween)
        rvSalesBetween.layoutManager = LinearLayoutManager(fragAnalysis.context)
        rvSalesBetween.adapter = adapSalesBetween
        tvSalesDates.visibility = View.GONE
        tvSalesBetweenTotal.visibility = View.GONE

        getProductsForSales()
        loadData()

        btnSalesBetweenDates.setOnClickListener {
            rvSalesBetween.isClickable = false
            alSalesBetween.clear()
            adapSalesBetween.notifyDataSetChanged()
            hmProductQuantity.clear()
            tvSalesDates.visibility = View.GONE
            tvSalesBetweenTotal.visibility = View.GONE
            val dtp = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range to get sales")
            .setSelection(Pair(null, null))
            .setTheme(R.style.myDateTimePicker)
            .build()

            dtp.show(this.parentFragmentManager, "")
            dtp.addOnPositiveButtonClickListener {
                tvSalesDates.visibility = View.VISIBLE
                tvSalesBetweenTotal.visibility = View.VISIBLE
                startDate = convertDate(it.first)
                endDate = convertDate(it.second)
                startDateInMillis = it.first
                endDateInMillis = it.second
                alDatesBetween = getDatesBetween(startDateInMillis, endDateInMillis)
                getSalesBetween()
            }
            dtp.addOnNegativeButtonClickListener {
                dtp.dismiss()
            }
        }

        return fragAnalysis
    }

    fun getProductsForSales(){
        try{
            alProductsForSales.clear()
            val qs: Task<QuerySnapshot> =  Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").get()
            qs.addOnSuccessListener{
                    documents->
                for(product in documents){
                    alProductsForSales.add(ProductModel(product.getString("id").toString(), product.getString("name").toString(), product.getString("price")!!.toDouble(), product.id, product.getString("deleted").toBoolean()))
                }
            }
        }catch (ex: Exception){
            Constants.logThis(ex.toString())
        }
    }
    @SuppressLint("SetTextI18n")
    fun loadData(){
        var monthlySale: Double = 0.0
        var monthlyStock: Double = 0.0
        val qsSales = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").get()
        qsSales.addOnSuccessListener {
            documents->
            for(sale in documents){
                if(sale.id.contains(Constants.getYearMonthForSales(Constants.getDateTime()))){
                    val qsSalesOnDate = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(sale.id).collection("sales").get()
                    qsSalesOnDate.addOnSuccessListener {
                        saleDoc->
                        for(saleOnDate in saleDoc){
                            for(pDetails in alProductsForSales){
                                if(pDetails.id.equals(saleOnDate.getString("id"))){
                                    val price = pDetails.price.toDouble()
                                    val quantity = saleOnDate.getString("quantity")!!.toInt()
                                    monthlySale += (price * quantity)
                                    tvMonthlySale.setText("₹ ${monthlySale.toString()}")
                                    val s1 = Constants.getDateToShow(Constants.getDateTime())
                                    val days = s1.split("-")
                                    tvDailyAverage.setText("₹ ${monthlySale/Integer.parseInt(days[0])}")
                                }
                            }
                        }
                    }
                }
            }
        }
        val qsStocks = Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").get()
        qsStocks.addOnSuccessListener {
                documents->
            for(stock in documents){
                if(stock.getString("date").toString().contains(Constants.getYearMonth(Constants.getDateTime()))){
                    val price = stock.getString("price")!!.toDouble()
                    monthlyStock += price
                    tvMonthlyStock.setText("₹ ${monthlyStock.toString()}")
                }
            }
        }
        val qsMostSold = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").get()
        qsMostSold.addOnSuccessListener {
            documents->
            for(sale in documents){
                val qsSaleDoc = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(sale.id).collection("sales").get()
                qsSaleDoc.addOnSuccessListener {
                    saleOnDateDocs->
                    for(saleOnDate in saleOnDateDocs){
                        for (productDetails in alProductsForSales){
                            if(productDetails.id.equals(saleOnDate.getString("id"))){
                                try{
                                    if(hmMostSoldProduct.containsKey(productDetails.name)){
                                        hmMostSoldProduct.replace(productDetails.name, (hmMostSoldProduct.get(productDetails.name)!!.toInt()+saleOnDate.getString("quantity")!!.toInt()))
                                    }else{
                                        hmMostSoldProduct.put(productDetails.name, saleOnDate.getString("quantity")!!.toInt())
                                    }
                                    Constants.logThis(hmMostSoldProduct.size.toString())
                                    getMostSoldProduct()
                                }catch (ex: Exception){
                                    Constants.logThis(ex.toString())
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    fun getMostSoldProduct(){
        var mostSoldProduct: String = ""
        if(alProductsForSales.size>0){
            mostSoldProduct = alProductsForSales.get(0).name
        }
        for(key in hmMostSoldProduct.keys){
            if(hmMostSoldProduct.get(key)!!.toInt()>hmMostSoldProduct.get(mostSoldProduct)!!.toInt()){
                mostSoldProduct = key
            }
        }
        tvMostSoldProduct.setText(mostSoldProduct)
        //TODO mostselling product not loading
    }
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    fun replicateAlQuantity(){
        alSalesBetween.clear()
        var index = 1
        var total = 0.0
        for(key in hmProductQuantity.keys){
            for(productDetails in alProductsForSales){
                if(productDetails.name.equals(key)){
                    alSalesBetween.add(ExpandedSaleModel(index++, "", key, productDetails.price, hmProductQuantity.get(key)!!.toInt(), "", "", productDetails.deleted))
                    total += (productDetails.price * hmProductQuantity.get(key)!!.toInt())
                }
            }
        }
        tvSalesDates.setText("Sale from ${Constants.getDateToShow("${startDate}@")} to ${Constants.getDateToShow("${endDate}@")}")
        tvSalesBetweenTotal.setText("₹ ${total}")
        adapSalesBetween.notifyDataSetChanged()
    }
    fun convertDate(miliSeconds: Long): String{
        val utc = Calendar.getInstance()
        utc.timeInMillis = miliSeconds
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return format.format(utc.timeInMillis)
    }
    fun getDatesBetween(startDateInMillis: Long, endDateInMillis: Long): ArrayList<String>{
        var alDates: ArrayList<String> = ArrayList<String>()
        var dt: String = ""
        var mn: String = ""
        var yr: String = ""
        var date: String = ""

        var startCal: Calendar = Calendar.getInstance()
        var endCal: Calendar = Calendar.getInstance()

        startCal.timeInMillis = startDateInMillis
        endCal.timeInMillis = endDateInMillis

        while (startCal.time.before(endCal.time)){
            dt = startCal.time.date.toString()
            mn = startCal.time.month.toString()
            yr = startCal.time.year.toString()
            mn = (mn.toInt()+1).toString()
            yr = (yr.toInt()+1900).toString()
            if(dt.toInt()<=9){
                dt = "0${dt}"
            }
            if(mn.toInt()<=9){
                mn = "0${mn}"
            }
            date = yr+"-"+mn+"-"+dt
            alDates.add(date)
            startCal.add(Calendar.DATE, 1);
        }
        val sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastDate = sdf.format(endCal.getTime())
        alDates.add(lastDate)

        return alDates
    }
    @SuppressLint("NotifyDataSetChanged")
    fun getSalesBetween(){
        for(date in alDatesBetween){
            val qsSales = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(Constants.getDocDateForQuantity(date)).collection("sales").get()
            qsSales.addOnSuccessListener {
                documents ->
                if(!documents.isEmpty){
                    for (saleOnDate in documents) {
                        for (productDetails in alProductsForSales) {
                            if (productDetails.id.equals(saleOnDate.getString("id"))) {
                                try {
                                    if (hmProductQuantity.containsKey(productDetails.name)) {
                                        hmProductQuantity.replace(productDetails.name, (hmProductQuantity.get(productDetails.name)!!.toInt() + saleOnDate.getString("quantity")!!.toInt()))
                                    } else {
                                        hmProductQuantity.put(productDetails.name, saleOnDate.getString("quantity")!!.toInt())
                                    }
                                    replicateAlQuantity()
                                } catch (ex: Exception) {
                                    Constants.logThis(ex.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}