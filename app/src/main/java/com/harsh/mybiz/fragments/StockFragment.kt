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
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.harsh.mybiz.R
import com.harsh.mybiz.adapters.ProductAdapter
import com.harsh.mybiz.adapters.StockAdapter
import com.harsh.mybiz.fragments.SaleFragment.Companion.fabAddSale
import com.harsh.mybiz.fragments.SaleFragment.Companion.rvExpandableSales
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants
import java.util.Arrays

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
    lateinit var stockName: String
    lateinit var stockPrice: String
    lateinit var stockSearch: String
    lateinit var adapStock: StockAdapter
    val hmStock: HashMap<String, String> = HashMap<String, String>()
    val alStocks: ArrayList<StockModel> = ArrayList<StockModel>()
    val alStockSearchResult: ArrayList<StockModel> = ArrayList<StockModel>()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragStock : View = inflater.inflate(R.layout.fragment_stock, container, false)

        rvStock = fragStock.findViewById(R.id.rv_stock)
        etStockSearch = fragStock.findViewById(R.id.et_search_stock)
        adapStock = StockAdapter(fragStock.context, alStocks)
        fabAddStock = fragStock.findViewById(R.id.fab_add_stock)
        rvStock.layoutManager = LinearLayoutManager(fragStock.context)
        tvStockTitle = fragStock.findViewById(R.id.tv_title)
        rvStock.adapter = adapStock
        getStocks()

        etStockSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                stockSearch = etStockSearch.text.toString()
                if(!stockSearch.isEmpty()){
                    try {
                        alStockSearchResult.clear()
                        for(product in alStocks){
                            if(product.name.contains(stockSearch) || Constants.getDateToShow(product.date).contains(stockSearch)){
                                alStockSearchResult.add(product)
                            }
                        }
                        adapStock = StockAdapter(fragStock.context, alStockSearchResult)
                        rvStock.adapter = adapStock
                        adapStock.notifyDataSetChanged()

                        var stockTotal = 0.0;
                        for(stock in alStockSearchResult){
                            stockTotal += stock.price;
                        }
                        tvStockTitle.setText("Stock total: "+stockTotal)
                    }catch (ex: Exception){
                        Constants.logThis(ex.toString())
                    }
                }else{
                    tvStockTitle.setText("Stocks:")
                    adapStock = StockAdapter(fragStock.context, alStocks)
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
                        Runnable { fabAddStock.setVisibility(View.VISIBLE) },
                        1000
                    )
                } else {
                    fabAddStock.setVisibility(View.GONE)
                }
            }
        })

        fabAddStock.setOnClickListener{
            bsAddStock = BottomSheetDialog(fragStock.context)
            bsAddStockView = LayoutInflater.from(fragStock.context).inflate(R.layout.bottom_sheet_add_stock, null, false)
            bsAddStock.setContentView(bsAddStockView)

            etStockName = bsAddStockView.findViewById(R.id.et_bs_add_stock_name)
            etStockPrice = bsAddStockView.findViewById(R.id.et_bs_add_stock_price)
            btnAddStock = bsAddStockView.findViewById(R.id.btn_bs_add_stock)
            ibBSClose = bsAddStockView.findViewById(R.id.ib_bs_add_stock_close)

            btnAddStock.setOnClickListener{
                try {
                    stockName = etStockName.text.toString().trim()
                    stockPrice = etStockPrice.text.toString().trim()
                    if(!stockName.isEmpty() && !stockPrice.isEmpty()){
                        hmStock.put("id", Constants.getUUIDForText(stockName))
                        hmStock.put("name", stockName)
                        hmStock.put("price", stockPrice)
                        hmStock.put("date", Constants.getDateForDB(Constants.getDateTime()))
                        Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").document().set(hmStock).addOnSuccessListener {
                            getStocks()
                            bsAddStock.dismiss()
                        }
                    }
                }catch (ex: Exception){
                    Constants.logThis(ex.toString())
                }
            }
            ibBSClose.setOnClickListener {
                bsAddStock.dismiss()
            }
            bsAddStock.show()
        }

        return fragStock
    }
    @SuppressLint("NotifyDataSetChanged", "SetTextI18n")
    fun getStocks(){
        alStocks.clear()
        val qs = Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").get()
        qs.addOnSuccessListener {
            documents->
            for(stock in documents){
                alStocks.add(StockModel(stock.getString("id").toString(), stock.getString("name").toString(), stock.getString("price")!!.toDouble(), stock.getString("date").toString(), stock.id))
            }
            alStocks.sortWith(Comparator.comparing(StockModel::date).reversed())
//            var stockTotal = 0.0;
//            for(stock in alStocks){
//                stockTotal += stock.price;
//            }
//            tvStockTitle.setText("Stock total: "+stockTotal)
            adapStock.notifyDataSetChanged()
        }
    }
}