package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.SplashActivity
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.models.SaleEntryModel
import com.harsh.mybiz.utilities.Constants
import com.pnpbook.adapters.ExpandableSalesAdapter


@SuppressLint("StaticFieldLeak")
class SaleFragment : Fragment() {

    companion object {
        var forceReloadProducts: Boolean = false
        lateinit var rvExpandableSales: RecyclerView
        lateinit var adapterExpandableSales: ExpandableSalesAdapter
        lateinit var adapterProductsSpinner: ArrayAdapter<String>
        lateinit var bsAddToSale: BottomSheetDialog
        lateinit var bsAddToSaleView: View
        lateinit var spProducts: Spinner
        lateinit var etQuantity: EditText
        lateinit var tvTotalAmount: TextView
        lateinit var ibAdd: ImageButton
        lateinit var ibMinus: ImageButton
        lateinit var ibClose: ImageButton
        lateinit var btnAddToSale: Button
        lateinit var fabAddSale: ImageButton
        lateinit var srlSalesRV: SwipeRefreshLayout
        lateinit var btnLoadMore: Button
        lateinit var pbLoadMore: ProgressBar
        var alProducts = ArrayList<ProductModel>()
        var alProductsForSales = ArrayList<ProductModel>()
        var alProductNames = ArrayList<String>()
        var alExpandableSales = ArrayList<ExpandableSalesModel>()
        lateinit var hmSale: HashMap<String, String>
        var monthlySale: Double = 0.0

        fun refreshSaleAdapter() {
            getSales()
        }

        fun getProductsForSales() {
            try {
                alProductsForSales.clear()
                val qs: Task<QuerySnapshot> =
                    Constants.fbStore.collection("businesses").document(Constants.uID)
                        .collection("products").get()
                qs.addOnSuccessListener { documents ->
                    for (product in documents) {
                        alProductsForSales.add(
                            ProductModel(
                                product.getString("id").toString(),
                                product.getString("name").toString(),
                                product.getString("price")!!.toDouble(),
                                product.id,
                                (!Constants.isDeleted(product))
                            )
                        )
                    }
                }
            } catch (ex: Exception) {
                Constants.logThis(ex.toString())
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun getSales(forceRefresh: Boolean = false) {
            if (Constants.alSalesCached.isEmpty() || forceRefresh) {
                if (forceRefresh) {
                    // Full reload: clear cache + pagination state, then re-fetch first window
                    Constants.alSalesCached.clear()
                    Constants.allSalesLoaded = false
                }
                SplashActivity.preloadSalesAndProducts { getSales(false) }
                return
            }

            alExpandableSales.clear()
            ExpandableSalesAdapter.alDatesTotal.clear()
            monthlySale = getMonthlySaleAmount()

            val grouped = Constants.alSalesCached.groupBy { it.date }
            for ((date, entries) in grouped) {
                var total = 0.0
                for (entry in entries) {
                    val product = Constants.alProductsOptimized.find { it.id == entry.productId }
                    if (product != null) total += (product.price * entry.quantity)
                }
                alExpandableSales.add(ExpandableSalesModel(false, date, total))
                ExpandableSalesAdapter.alDatesTotal.add(ExpandableSalesModel(false, date, total))
            }

            alExpandableSales.sortByDescending { it.date }
            ExpandableSalesAdapter.alDatesTotal.sortByDescending { it.date }

            adapterExpandableSales.notifyDataSetChanged()
            updateLoadMoreVisibility()
        }

        /** Called when user taps "Load More" — fetches the next 3-month window from Firestore */
        @SuppressLint("NotifyDataSetChanged")
        fun loadMoreSales() {
            if (Constants.allSalesLoaded) {
                updateLoadMoreVisibility()
                return
            }
            try {
                pbLoadMore.visibility = View.VISIBLE
                btnLoadMore.visibility = View.GONE
            } catch (_: Exception) {}

            SplashActivity.loadMoreSales {
                // Only append dates not already shown
                val alreadyShownDates = alExpandableSales.map { it.date }.toSet()
                val grouped = Constants.alSalesCached.groupBy { it.date }
                for ((date, entries) in grouped) {
                    if (date in alreadyShownDates) continue
                    var total = 0.0
                    for (entry in entries) {
                        val product = Constants.alProductsOptimized.find { it.id == entry.productId }
                        if (product != null) total += (product.price * entry.quantity)
                    }
                    alExpandableSales.add(ExpandableSalesModel(false, date, total))
                    ExpandableSalesAdapter.alDatesTotal.add(ExpandableSalesModel(false, date, total))
                }
                alExpandableSales.sortByDescending { it.date }
                ExpandableSalesAdapter.alDatesTotal.sortByDescending { it.date }
                adapterExpandableSales.notifyDataSetChanged()

                try { pbLoadMore.visibility = View.GONE } catch (_: Exception) {}
                updateLoadMoreVisibility()
            }
        }

        private fun updateLoadMoreVisibility() {
            try {
                if (Constants.allSalesLoaded) {
                    btnLoadMore.visibility = View.GONE
                    pbLoadMore.visibility = View.GONE
                } else {
                    btnLoadMore.visibility = View.VISIBLE
                    pbLoadMore.visibility = View.GONE
                }
            } catch (_: Exception) {}
        }

        fun getMonthlySaleAmount(): Double {
            val monthKey = Constants.getYearMonthForSales(Constants.getDateTime())
            val result = Constants.alSalesCached
                .filter { Constants.getDateForYearMonth(it.date) == monthKey }
                .sumOf { entry ->
                    val product = Constants.alProductsOptimized.find { it.id == entry.productId }
                    (product?.price ?: 0.0) * entry.quantity
                }
            monthlySale = result
            return result
        }
    }

    var qty: Int = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragSale: View = inflater.inflate(R.layout.fragment_sale, container, false)

        fabAddSale = fragSale.findViewById(R.id.fab_add_sale)
        srlSalesRV = fragSale.findViewById(R.id.srl_expandable_sales)
        rvExpandableSales = fragSale.findViewById(R.id.rv_expandable_sales)
        btnLoadMore = fragSale.findViewById(R.id.btn_load_more_sales)
        pbLoadMore = fragSale.findViewById(R.id.pb_load_more_sales)

        rvExpandableSales.layoutManager = LinearLayoutManager(fragSale.context)
        adapterExpandableSales = ExpandableSalesAdapter(fragSale.context, alExpandableSales)
        rvExpandableSales.adapter = adapterExpandableSales

        getProductsOptimized()
        getSales()

        // "Load More" button — loads the next 3-month window
        btnLoadMore.setOnClickListener {
            loadMoreSales()
        }

        srlSalesRV.setOnRefreshListener {
            getSales(true)
            srlSalesRV.isRefreshing = false
        }

        rvExpandableSales.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    android.os.Handler().postDelayed(
                        Runnable { fabAddSale.visibility = View.VISIBLE },
                        1000
                    )
                } else {
                    fabAddSale.visibility = View.GONE
                }
            }
        })

        fabAddSale.setOnClickListener {
            bsAddToSale = BottomSheetDialog(fragSale.context)
            bsAddToSaleView = LayoutInflater.from(fragSale.context)
                .inflate(R.layout.bottom_sheet_add_sale, null, false)
            bsAddToSale.setContentView(bsAddToSaleView)

            spProducts = bsAddToSaleView.findViewById(R.id.sp_products)
            etQuantity = bsAddToSaleView.findViewById(R.id.et_quantity)
            ibAdd = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_plus)
            ibMinus = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_minus)
            ibClose = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_close)
            btnAddToSale = bsAddToSaleView.findViewById(R.id.btn_bs_add_sale)
            tvTotalAmount = bsAddToSaleView.findViewById(R.id.tv_sale_total_amount)
            spProducts.adapter = adapterProductsSpinner

            ibClose.setOnClickListener { bsAddToSale.dismiss() }

            spProducts.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateTotal()
                }
            }

            etQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                @SuppressLint("SetTextI18n")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (etQuantity.text.isEmpty() || etQuantity.text.toString() == "0") {
                        etQuantity.setText("1")
                        qty = 1
                    } else {
                        qty = etQuantity.text.toString().toInt()
                    }
                    calculateTotal()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            ibAdd.setOnClickListener {
                qty = etQuantity.text.toString().toInt()
                qty++
                etQuantity.setText(qty.toString())
            }
            ibMinus.setOnClickListener {
                qty = etQuantity.text.toString().toInt()
                if (qty > 1) {
                    qty--
                    etQuantity.setText(qty.toString())
                }
            }

            btnAddToSale.setOnClickListener {
                try {
                    getSales()  // instant from memory
                    val qsForProductName =
                        Constants.fbStore.collection("businesses").document(Constants.uID)
                            .collection("products")
                            .whereEqualTo("name", spProducts.selectedItem.toString()).get()
                    qsForProductName.addOnSuccessListener { pDocuments ->
                        if (pDocuments.size() == 1) {
                            val pId = pDocuments.documents[0].getString("id")

                            val todayKey = "sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}"
                            val newQty = etQuantity.text.toString().toInt()

                            // Update local cache immediately
                            val existingEntry = Constants.alSalesCached.find {
                                it.date == todayKey && it.productId == pId
                            }
                            if (existingEntry != null) {
                                existingEntry.quantity += newQty
                            } else {
                                Constants.alSalesCached.add(
                                    SaleEntryModel(date = todayKey, productId = pId.toString(), quantity = newQty)
                                )
                            }

                            val qsForSale =
                                Constants.fbStore.collection("businesses").document(Constants.uID)
                                    .collection("sales")
                                    .document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}")
                                    .collection("sales").whereEqualTo("id", pId).get()
                            qsForSale.addOnSuccessListener { saleDocuments ->
                                if (saleDocuments.size() == 1) {
                                    try {
                                        var qty: Int = saleDocuments.documents[0].getString("quantity")!!.toInt()
                                        val docId: String = saleDocuments.documents[0].id
                                        qty += etQuantity.text.toString().toInt()
                                        Constants.fbStore.collection("businesses")
                                            .document(Constants.uID).collection("sales")
                                            .document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}")
                                            .collection("sales").document(docId)
                                            .update("quantity", qty.toString())
                                            .addOnSuccessListener {
                                                getSales()
                                                bsAddToSale.dismiss()
                                                Constants.toastThis(fragSale.context, "Product added to sales!")
                                            }
                                    } catch (ex: Exception) {
                                        Constants.logThis(ex.toString())
                                    }
                                } else if (saleDocuments.size() == 0) {
                                    hmSale = HashMap()
                                    var resolvedPId = ""
                                    val qs = Constants.fbStore.collection("businesses")
                                        .document(Constants.uID).collection("products")
                                        .whereEqualTo("name", spProducts.selectedItem.toString()).get()
                                    qs.addOnSuccessListener { documents ->
                                        for (product in documents) {
                                            resolvedPId = product.getString("id").toString()
                                        }
                                        hmSale["id"] = resolvedPId
                                        hmSale["quantity"] = etQuantity.text.toString()
                                        hmSale["date"] = Constants.getDateForDB(Constants.getDateTime())
                                        val hmDate = hashMapOf("did" to "rawID")
                                        Constants.fbStore.collection("businesses")
                                            .document(Constants.uID).collection("sales")
                                            .document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}")
                                            .set(hmDate).addOnSuccessListener {
                                                Constants.fbStore.collection("businesses")
                                                    .document(Constants.uID).collection("sales")
                                                    .document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}")
                                                    .collection("sales").document().set(hmSale)
                                                    .addOnSuccessListener {
                                                        getSales()
                                                        bsAddToSale.dismiss()
                                                        Constants.toastThis(fragSale.context, "Product added to sales!")
                                                    }
                                            }
                                    }
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Constants.logThis(ex.toString())
                }
            }

            if (etQuantity.text.isEmpty() || etQuantity.text.toString() == "0") {
                etQuantity.setText("1")
                qty = 1
            } else {
                qty = etQuantity.text.toString().toInt()
            }
            calculateTotal()
            bsAddToSale.show()
        }

        return fragSale
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getProductsOptimized() {
        try {
            if (Constants.alProductsOptimized.size > 0 && !forceReloadProducts) {
                // Already cached — just rebuild the spinner list
                alProductNames.clear()
                for (product in Constants.alProductsOptimized) {
                    if (!product.deleted) alProductNames.add(product.name)
                }
                Constants.alProductsOptimized.sortWith(Comparator.comparing(ProductModel::name))
                alProductNames.sort()
                adapterProductsSpinner = ArrayAdapter(
                    requireContext(),
                    R.layout.product_sp_item_ui,
                    R.id.tv_sp_item_product_name,
                    alProductNames
                )
                adapterProductsSpinner.notifyDataSetChanged()
                if (forceReloadProducts) {
                    calculateTotal()
                    forceReloadProducts = false
                }
                return
            }

            Constants.alProductsOptimized.clear()
            alProductNames.clear()
            val qs: Task<QuerySnapshot> =
                Constants.fbStore.collection("businesses").document(Constants.uID)
                    .collection("products").get()
            qs.addOnSuccessListener { documents ->
                for (product in documents) {
                    Constants.alProductsOptimized.add(
                        ProductModel(
                            product.getString("id").toString(),
                            product.getString("name").toString(),
                            product.getString("price")!!.toDouble(),
                            product.id,
                            Constants.isDeleted(product)
                        )
                    )
                    if (!Constants.isDeleted(product)) {
                        alProductNames.add(product.getString("name").toString())
                    }
                }
                Constants.alProductsOptimized.sortWith(Comparator.comparing(ProductModel::name))
                alProductNames.sort()
                adapterProductsSpinner = ArrayAdapter(
                    requireContext(),
                    R.layout.product_sp_item_ui,
                    R.id.tv_sp_item_product_name,
                    alProductNames
                )
                adapterProductsSpinner.notifyDataSetChanged()
                calculateTotal()
            }
        } catch (ex: Exception) {
            Constants.logThis(ex.toString())
        }
    }

    @SuppressLint("SetTextI18n")
    fun calculateTotal() {
        try {
            var price = 0.0
            for (product in Constants.alProductsOptimized) {
                if (product.name == spProducts.selectedItem.toString()) {
                    price = product.price
                }
            }
            tvTotalAmount.text = "₹ ${qty * price}"
        } catch (ex: Exception) {
            Constants.logThis(ex.toString())
        }
    }
}
