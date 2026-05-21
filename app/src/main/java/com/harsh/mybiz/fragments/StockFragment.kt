package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.harsh.mybiz.R
import com.harsh.mybiz.SplashActivity
import com.harsh.mybiz.adapters.StockAdapter
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants

class StockFragment : Fragment() {

    lateinit var rvStock: RecyclerView
    lateinit var fabAddStock: FloatingActionButton
    lateinit var bsAddStock: BottomSheetDialog
    lateinit var bsAddStockView: View
    lateinit var etStockName: EditText
    lateinit var etStockPrice: EditText
    lateinit var tvStockTitle: TextView
    lateinit var etStockSearch: EditText
    lateinit var ibBSClose: ImageButton
    lateinit var btnAddStock: Button
    lateinit var btnLoadMore: Button
    lateinit var pbLoadMore: ProgressBar
    lateinit var stockName: String
    lateinit var stockPrice: String
    lateinit var stockSearch: String
    lateinit var adapStock: StockAdapter
    val hmStock: HashMap<String, String> = HashMap()
    val alStockSearchResult: ArrayList<StockModel> = ArrayList()

    @SuppressLint("MissingInflatedId", "NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragStock: View = inflater.inflate(R.layout.fragment_stock, container, false)

        rvStock       = fragStock.findViewById(R.id.rv_stock)
        etStockSearch = fragStock.findViewById(R.id.et_search_stock)
        fabAddStock   = fragStock.findViewById(R.id.fab_add_stock)
        tvStockTitle  = fragStock.findViewById(R.id.tv_title)
        btnLoadMore   = fragStock.findViewById(R.id.btn_load_more_stocks)
        pbLoadMore    = fragStock.findViewById(R.id.pb_load_more_stocks)

        rvStock.layoutManager = LinearLayoutManager(fragStock.context)
        adapStock = StockAdapter(fragStock.context, Constants.alStocksCached)
        rvStock.adapter = adapStock

        getStocks()

        // Load More button — fetches the next 3-month window
        btnLoadMore.setOnClickListener {
            loadMoreStocks()
        }

        etStockSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                stockSearch = etStockSearch.text.toString().lowercase()
                if (stockSearch.isNotEmpty()) {
                    try {
                        alStockSearchResult.clear()
                        for (stock in Constants.alStocksCached) {
                            if (stock.name.contains(stockSearch) ||
                                Constants.getDateToShow(stock.date).contains(stockSearch)
                            ) {
                                alStockSearchResult.add(stock)
                            }
                        }
                        adapStock = StockAdapter(fragStock.context, alStockSearchResult)
                        rvStock.adapter = adapStock
                        adapStock.notifyDataSetChanged()

                        var stockTotal = 0.0
                        for (stock in alStockSearchResult) stockTotal += stock.price
                        tvStockTitle.text = "Stock total: $stockTotal"
                    } catch (ex: Exception) {
                        Constants.logThis(ex.toString())
                    }
                } else {
                    tvStockTitle.text = "Stocks:"
                    adapStock = StockAdapter(fragStock.context, Constants.alStocksCached)
                    rvStock.adapter = adapStock
                    adapStock.notifyDataSetChanged()
                }
            }
        })

        rvStock.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    android.os.Handler().postDelayed(
                        Runnable { fabAddStock.visibility = View.VISIBLE },
                        1000
                    )
                } else {
                    fabAddStock.visibility = View.GONE
                }
            }
        })

        fabAddStock.setOnClickListener {
            bsAddStock = BottomSheetDialog(fragStock.context)
            bsAddStockView = LayoutInflater.from(fragStock.context)
                .inflate(R.layout.bottom_sheet_add_stock, null, false)
            bsAddStock.setContentView(bsAddStockView)

            etStockName  = bsAddStockView.findViewById(R.id.et_bs_add_stock_name)
            etStockPrice = bsAddStockView.findViewById(R.id.et_bs_add_stock_price)
            btnAddStock  = bsAddStockView.findViewById(R.id.btn_bs_add_stock)
            ibBSClose    = bsAddStockView.findViewById(R.id.ib_bs_add_stock_close)

            btnAddStock.setOnClickListener {
                try {
                    stockName  = etStockName.text.toString().trim()
                    stockPrice = etStockPrice.text.toString().trim()
                    if (stockName.isNotEmpty() && stockPrice.isNotEmpty()) {
                        hmStock["id"]    = Constants.getUUIDForText(stockName)
                        hmStock["name"]  = stockName
                        hmStock["price"] = stockPrice
                        hmStock["date"]  = Constants.getDateForDB(Constants.getDateTime())

                        val newStockRef = Constants.fbStore.collection("businesses")
                            .document(Constants.uID).collection("stocks").document()
                        val newDocumentId = newStockRef.id

                        newStockRef.set(hmStock).addOnSuccessListener {
                            getStocks()
                            bsAddStock.dismiss()
                        }

                        // Optimistic local update
                        Constants.alStocksCached.add(
                            StockModel(
                                hmStock["id"].toString(),
                                hmStock["name"].toString(),
                                hmStock["price"]!!.toDouble(),
                                hmStock["date"].toString(),
                                newDocumentId
                            )
                        )
                        Constants.alStocksCached.sortWith(Comparator.comparing(StockModel::date).reversed())
                        adapStock.notifyDataSetChanged()
                    }
                } catch (ex: Exception) {
                    Constants.logThis(ex.toString())
                }
            }

            ibBSClose.setOnClickListener { bsAddStock.dismiss() }
            bsAddStock.show()
        }

        return fragStock
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getStocks() {
        if (Constants.alStocksCached.isNotEmpty()) {
            // Already have data — just refresh the adapter and show/hide Load More
            adapStock.notifyDataSetChanged()
            updateLoadMoreVisibility()
            return
        }
        // Nothing cached yet — do the initial paginated load
        Constants.allStocksLoaded = false
        SplashActivity.preloadStocks {
            adapStock.notifyDataSetChanged()
            updateLoadMoreVisibility()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadMoreStocks() {
        if (Constants.allStocksLoaded) {
            updateLoadMoreVisibility()
            return
        }
        pbLoadMore.visibility  = View.VISIBLE
        btnLoadMore.visibility = View.GONE

        SplashActivity.loadMoreStocks {
            adapStock.notifyDataSetChanged()
            pbLoadMore.visibility = View.GONE
            updateLoadMoreVisibility()
        }
    }

    private fun updateLoadMoreVisibility() {
        if (Constants.allStocksLoaded) {
            btnLoadMore.visibility = View.GONE
            pbLoadMore.visibility  = View.GONE
        } else {
            btnLoadMore.visibility = View.VISIBLE
            pbLoadMore.visibility  = View.GONE
        }
    }
}
