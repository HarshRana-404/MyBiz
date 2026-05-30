package com.harsh.mybiz.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

    private lateinit var rvStock: RecyclerView
    private lateinit var fabAddStock: FloatingActionButton
    private lateinit var tvStockTitle: TextView
    private lateinit var etStockSearch: EditText
    private lateinit var adapStock: StockAdapter

    // Debounce handler — waits 300ms after the user stops typing before filtering
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragStock: View = inflater.inflate(R.layout.fragment_stock, container, false)

        rvStock      = fragStock.findViewById(R.id.rv_stock)
        etStockSearch = fragStock.findViewById(R.id.et_search_stock)
        fabAddStock  = fragStock.findViewById(R.id.fab_add_stock)
        tvStockTitle = fragStock.findViewById(R.id.tv_title)

        // Adapter owns the Load More button as a footer — no NestedScrollView needed
        adapStock = StockAdapter(
            context = fragStock.context,
            alStocksMaster = Constants.alStocksCached,
            onLoadMoreClick = { loadMoreStocks() }
        )
        rvStock.layoutManager = LinearLayoutManager(fragStock.context)
        rvStock.adapter = adapStock

        getStocks()

        // ── Search with 300ms debounce ──────────────────────────────────────
        etStockSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            @SuppressLint("SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel any pending search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }

                searchRunnable = Runnable {
                    val query = etStockSearch.text.toString().trim()
                    adapStock.filter(query)

                    if (query.isEmpty()) {
                        tvStockTitle.text = "Stocks:"
                    } else {
                        // Calculate total from the filtered display — adapter exposes nothing,
                        // so we compute it from the same filter logic here
                        val total = Constants.alStocksCached
                            .filter { stock ->
                                stock.name.lowercase().contains(query.lowercase()) ||
                                Constants.getDateToShow(stock.date).contains(query.lowercase())
                            }
                            .sumOf { it.price }
                        tvStockTitle.text = "Stock total: $total"
                    }
                }
                searchHandler.postDelayed(searchRunnable!!, 300)
            }
        })

        // ── FAB scroll hide/show ────────────────────────────────────────────
        rvStock.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    Handler(Looper.getMainLooper()).postDelayed(
                        { fabAddStock.visibility = View.VISIBLE }, 1000
                    )
                } else {
                    fabAddStock.visibility = View.GONE
                }
            }
        })

        // ── Add stock FAB ───────────────────────────────────────────────────
        fabAddStock.setOnClickListener {
            val bsAddStock = BottomSheetDialog(fragStock.context)
            val bsAddStockView = LayoutInflater.from(fragStock.context)
                .inflate(R.layout.bottom_sheet_add_stock, null, false)
            bsAddStock.setContentView(bsAddStockView)

            val etStockName  = bsAddStockView.findViewById<EditText>(R.id.et_bs_add_stock_name)
            val etStockPrice = bsAddStockView.findViewById<EditText>(R.id.et_bs_add_stock_price)
            val btnAddStock  = bsAddStockView.findViewById<Button>(R.id.btn_bs_add_stock)
            val ibBSClose    = bsAddStockView.findViewById<ImageButton>(R.id.ib_bs_add_stock_close)

            btnAddStock.setOnClickListener {
                try {
                    val stockName  = etStockName.text.toString().trim()
                    val stockPrice = etStockPrice.text.toString().trim()
                    if (stockName.isNotEmpty() && stockPrice.isNotEmpty()) {
                        val hmStock = hashMapOf(
                            "id"    to Constants.getUUIDForText(stockName),
                            "name"  to stockName,
                            "price" to stockPrice,
                            "date"  to Constants.getDateForDB(Constants.getDateTime())
                        )

                        val newStockRef = Constants.fbStore.collection("businesses")
                            .document(Constants.uID).collection("stocks").document()

                        // Optimistic local update — add to cache and refresh adapter immediately
                        // so the user sees the new item without waiting for Firestore
                        val newStock = StockModel(
                            hmStock["id"]!!,
                            hmStock["name"]!!,
                            hmStock["price"]!!.toDouble(),
                            hmStock["date"]!!,
                            newStockRef.id
                        )
                        Constants.alStocksCached.add(newStock)
                        Constants.alStocksCached.sortWith(Comparator.comparing(StockModel::date).reversed())
                        adapStock.refreshFromMaster()

                        bsAddStock.dismiss()

                        // Write to Firestore in the background
                        newStockRef.set(hmStock).addOnFailureListener {
                            // Rollback on failure
                            Constants.alStocksCached.remove(newStock)
                            adapStock.refreshFromMaster()
                            Constants.toastThis(fragStock.context, "Failed to save stock. Please retry.")
                        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any pending search callbacks to avoid leaks
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
    }

    // ── Data loading ────────────────────────────────────────────────────────

    private fun getStocks() {
        if (Constants.alStocksCached.isNotEmpty()) {
            adapStock.refreshFromMaster()
            updateLoadMoreState()
            return
        }
        Constants.allStocksLoaded = false
        adapStock.isLoading = true
        SplashActivity.preloadStocks {
            adapStock.isLoading = false
            adapStock.refreshFromMaster()
            updateLoadMoreState()
        }
    }

    private fun loadMoreStocks() {
        if (Constants.allStocksLoaded) {
            updateLoadMoreState()
            return
        }
        adapStock.isLoading    = true
        adapStock.showLoadMore = false

        SplashActivity.loadMoreStocks {
            adapStock.isLoading = false
            adapStock.refreshFromMaster()
            updateLoadMoreState()
        }
    }

    private fun updateLoadMoreState() {
        adapStock.showLoadMore = !Constants.allStocksLoaded
        adapStock.isLoading    = false
    }
}
