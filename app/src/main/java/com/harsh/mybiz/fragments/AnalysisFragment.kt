package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.util.Pair
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.harsh.mybiz.R
import com.harsh.mybiz.SplashActivity
import com.harsh.mybiz.adapters.SalesBetweenAdapter
import com.harsh.mybiz.adapters.StocksBetweenAdapter
import com.harsh.mybiz.models.ExpandedSaleModel
import com.harsh.mybiz.models.StockModel
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
    lateinit var tvStockDates: TextView
    lateinit var tvStockBetweenTotal: TextView
    lateinit var tvSalesBetweenTotal: TextView
    lateinit var btnSalesBetweenDates: Button
    lateinit var pbAnalysis: ProgressBar
    lateinit var rvSalesBetween: RecyclerView
    lateinit var adapSalesBetween: SalesBetweenAdapter
    lateinit var rvStockBetween: RecyclerView
    lateinit var adapStockBetween: StocksBetweenAdapter

    var alStocksBetween = ArrayList<StockModel>()
    var alSalesBetween  = ArrayList<ExpandedSaleModel>()
    var hmProductQuantity = HashMap<String, Int>()

    var startDate: String = ""
    var endDate: String   = ""
    var startDateInMillis: Long = 0
    var endDateInMillis: Long   = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_analysis, container, false)

        tvMonthlySale       = v.findViewById(R.id.tv_sale_monthly_value)
        tvMonthlyStock      = v.findViewById(R.id.tv_stock_monthly_value)
        tvMostSoldProduct   = v.findViewById(R.id.tv_most_selling_product)
        tvDailyAverage      = v.findViewById(R.id.tv_daily_average)
        tvSalesDates        = v.findViewById(R.id.tv_sale_dates)
        tvStockDates        = v.findViewById(R.id.tv_stock_dates)
        tvStockBetweenTotal = v.findViewById(R.id.tv_stock_between_total)
        tvSalesBetweenTotal = v.findViewById(R.id.tv_sale_between_total)
        btnSalesBetweenDates = v.findViewById(R.id.btn_sales_between_dates)
        pbAnalysis          = v.findViewById(R.id.pb_analysis)
        rvSalesBetween      = v.findViewById(R.id.rv_sales_between_dates)
        rvStockBetween      = v.findViewById(R.id.rv_stock_between_dates)

        adapSalesBetween = SalesBetweenAdapter(requireContext(), alSalesBetween)
        rvSalesBetween.layoutManager = LinearLayoutManager(requireContext())
        rvSalesBetween.adapter = adapSalesBetween

        adapStockBetween = StocksBetweenAdapter(requireContext(), alStocksBetween)
        rvStockBetween.layoutManager = LinearLayoutManager(requireContext())
        rvStockBetween.adapter = adapStockBetween

        tvSalesDates.visibility        = View.GONE
        tvSalesBetweenTotal.visibility = View.GONE
        tvStockDates.visibility        = View.GONE
        tvStockBetweenTotal.visibility = View.GONE
        pbAnalysis.visibility          = View.GONE

        loadOptimizedData()

        btnSalesBetweenDates.setOnClickListener {
            openDateRangePicker()
        }

        return v
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun loadOptimizedData() {
        try {
            var monthKey = Constants.getYearMonthForSales(Constants.getDateTime())
            val today = Calendar.getInstance()
            val currentDay = today.get(Calendar.DAY_OF_MONTH)

            val monthlySale = Constants.alSalesCached
                .filter { Constants.getDateForYearMonth(it.date) == monthKey }
                .sumOf { entry ->
                    val product = Constants.alProductsOptimized.find { it.id == entry.productId }
                    (product?.price ?: 0.0) * entry.quantity
                }

            tvMonthlySale.text  = "₹ $monthlySale"
            tvDailyAverage.text = "₹ ${String.format("%.2f", monthlySale / currentDay.toDouble())}"

            // Stock month key is "YYYY-MM"
            val stockMonthKey = monthKey.replace("_", "-")
            val monthlyStock = Constants.alStocksCached
                .filter { it.date.contains(stockMonthKey) }
                .sumOf { it.price }
            tvMonthlyStock.text = "₹ $monthlyStock"

            val hmMostSold = HashMap<String, Int>()
            Constants.alSalesCached.forEach { sale ->
                val product = Constants.alProductsOptimized.find { it.id == sale.productId }
                product?.let {
                    hmMostSold[it.name] = (hmMostSold[it.name] ?: 0) + sale.quantity
                }
            }
            tvMostSoldProduct.text = hmMostSold.maxByOrNull { it.value }?.key ?: "No Sales"

        } catch (ex: Exception) {
            Constants.logThis(ex.toString())
        }
    }

    // ─────────────────── DATE RANGE PICKER ─────────────────── //

    private fun openDateRangePicker() {
        val dtp = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select date range to get sales")
            .setSelection(Pair(null, null))
            .setTheme(R.style.myDateTimePicker)
            .build()

        dtp.show(parentFragmentManager, "")
        dtp.addOnPositiveButtonClickListener {
            startDateInMillis = it.first!!
            endDateInMillis   = it.second!!
            startDate = convertDate(startDateInMillis)
            endDate   = convertDate(endDateInMillis)

            tvSalesDates.visibility        = View.VISIBLE
            tvSalesBetweenTotal.visibility = View.VISIBLE
            tvStockDates.visibility        = View.VISIBLE
            tvStockBetweenTotal.visibility = View.VISIBLE

            // Cache-first: check what's already loaded, fetch only what's missing
            fetchMissingDataThenCalculate()
        }
    }

    // ─────────────────── CACHE-FIRST RANGE LOGIC ─────────────────── //

    /**
     * Checks whether [startDate]..[endDate] is fully covered by the current caches.
     * If sales or stocks data is missing for part of the range, fetches only those
     * gaps from Firestore, then calls [calculateSalesBetween].
     *
     * Strategy:
     *  - Sales cache: find which dates in the range have NO entry in alSalesCached.
     *    If any date is missing, fetch the entire [startDate..endDate] sales window
     *    (Firestore will return only docs that exist, so no wasted reads).
     *  - Stocks cache: same approach — if any date in range has no stock entry,
     *    fetch the full range from Firestore.
     *
     * After both fetches complete (or if cache was already sufficient), render.
     */
    @SuppressLint("SetTextI18n")
    private fun fetchMissingDataThenCalculate() {
        pbAnalysis.visibility = View.VISIBLE
        btnSalesBetweenDates.isEnabled = false

        val salesCovered  = isSalesCoveredInCache(startDate, endDate)
        val stocksCovered = isStocksCoveredInCache(startDate, endDate)

        var pendingFetches = 0
        if (!salesCovered)  pendingFetches++
        if (!stocksCovered) pendingFetches++

        if (pendingFetches == 0) {
            // Everything is in cache — render immediately
            pbAnalysis.visibility = View.GONE
            btnSalesBetweenDates.isEnabled = true
            calculateSalesBetween()
            return
        }

        val onFetchDone = {
            pendingFetches--
            if (pendingFetches == 0) {
                pbAnalysis.visibility = View.GONE
                btnSalesBetweenDates.isEnabled = true
                calculateSalesBetween()
            }
        }

        if (!salesCovered) {
            // Convert plain dates to sales doc-ID keys for the range query
            val startKey = plainDateToSalesKey(startDate)
            val endKey   = plainDateToSalesKey(endDate)
            SplashActivity.fetchSalesForRange(startKey, endKey) { onFetchDone() }
        }

        if (!stocksCovered) {
            SplashActivity.fetchStocksForRange(startDate, endDate) { onFetchDone() }
        }
    }

    /**
     * Returns true if the sales cache already contains data covering [startDate]..[endDate].
     * We consider it covered if at least one sale entry exists anywhere in the range
     * OR if the range is entirely within the already-loaded pagination window.
     *
     * A simpler and safe heuristic: check whether the range's start date is ≥ the
     * oldest date currently in the cache. If yes, the cache window covers it.
     * If no, we need to fetch.
     */
    private fun isSalesCoveredInCache(startDate: String, endDate: String): Boolean {
        if (Constants.alSalesCached.isEmpty()) return false

        // Convert plain "YYYY-MM-DD" to sales key format "sales_YYYY_MM_DD" for comparison
        val startKey = plainDateToSalesKey(startDate)
        val endKey   = plainDateToSalesKey(endDate)

        // Find the oldest and newest date in the cache
        val oldestCached = Constants.alSalesCached.minOfOrNull { it.date } ?: return false
        val newestCached = Constants.alSalesCached.maxOfOrNull { it.date } ?: return false

        // The range is fully covered if cache spans [startKey, endKey]
        return oldestCached <= startKey && newestCached >= endKey
    }

    /**
     * Returns true if the stocks cache already covers [startDate]..[endDate].
     */
    private fun isStocksCoveredInCache(startDate: String, endDate: String): Boolean {
        if (Constants.alStocksCached.isEmpty()) return false

        val oldestCached = Constants.alStocksCached.minOfOrNull { it.date } ?: return false
        val newestCached = Constants.alStocksCached.maxOfOrNull { it.date } ?: return false

        return oldestCached <= startDate && newestCached >= endDate
    }

    // ─────────────────── CALCULATION ─────────────────── //

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun calculateSalesBetween() {
        alSalesBetween.clear()
        hmProductQuantity.clear()

        // Aggregate sales quantities per product for the selected range
        Constants.alSalesCached
            .filter { Constants.getDateForYYYYMMDD(it.date) in startDate..endDate }
            .forEach { entry ->
                val product = Constants.alProductsOptimized.find { it.id == entry.productId }
                product?.let {
                    hmProductQuantity[it.name] = (hmProductQuantity[it.name] ?: 0) + entry.quantity
                }
            }

        var salesTotal = 0.0
        var index = 1
        hmProductQuantity.forEach { (productName, qty) ->
            val product = Constants.alProductsOptimized.find { it.name == productName } ?: return@forEach
            salesTotal += product.price * qty
            alSalesBetween.add(
                ExpandedSaleModel(index++, "", productName, product.price, qty, "", "", product.deleted)
            )
        }

        // Aggregate stocks for the selected range
        alStocksBetween.clear()
        var stockTotal = 0.0
        Constants.alStocksCached
            .filter { it.date in startDate..endDate }
            .forEach { stock ->
                stockTotal += stock.price
                alStocksBetween.add(
                    StockModel(
                        id    = stock.id,
                        name  = stock.name,
                        price = stock.price,
                        date  = Constants.getDateToShow(stock.date),
                        docId = stock.docId
                    )
                )
            }

        tvSalesDates.text =
            "Sale from ${Constants.getDateToShow("$startDate@")} to ${Constants.getDateToShow("$endDate@")}"
        tvSalesBetweenTotal.text = "₹ $salesTotal"
        adapSalesBetween.notifyDataSetChanged()

        tvStockDates.text =
            "Stock from ${Constants.getDateToShow("$startDate@")} to ${Constants.getDateToShow("$endDate@")}"
        tvStockBetweenTotal.text = "₹ $stockTotal"
        adapStockBetween.notifyDataSetChanged()
    }

    // ─────────────────── HELPERS ─────────────────── //

    /** Converts millis from the date picker to "YYYY-MM-DD" */
    private fun convertDate(ms: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(ms)

    /** Converts "YYYY-MM-DD" → "sales_YYYY_MM_DD" for sales cache key comparison */
    private fun plainDateToSalesKey(date: String): String =
        "sales_" + date.replace("-", "_")
}
