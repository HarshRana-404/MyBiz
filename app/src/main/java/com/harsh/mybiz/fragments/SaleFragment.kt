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
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.utilities.Constants
import com.pnpbook.adapters.ExpandableSalesAdapter
import java.util.logging.Handler


class SaleFragment : Fragment() {

    @SuppressLint("StaticFieldLeak")
    companion object{
        lateinit var rvExpandableSales: RecyclerView
        lateinit var adapterExpandableSales: ExpandableSalesAdapter
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
        var alProducts = ArrayList<ProductModel>()
        var alProductsForSales = ArrayList<ProductModel>()
        var alProductNames = ArrayList<String>()
        var alExpandableSales = ArrayList<ExpandableSalesModel>()
        lateinit var hmSale: HashMap<String, String>
        var monthlySale: Double = 0.0
        fun refreshSaleAdapter(){
            getSales()
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
        @SuppressLint("NotifyDataSetChanged")
        fun getSales(){
            try{
                getMonthlySaleAmount()
                getProductsForSales()
                var total = 0.0
                alExpandableSales.clear()

                var qs = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").get()
                qs.addOnSuccessListener {
                        saleDocs->
                    try{
                        for(saleDoc in saleDocs){
                            val docDate = saleDoc.id
                            var qsSaleDocs = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(docDate).collection("sales").get()
                            qsSaleDocs.addOnSuccessListener {
                                    saleColDocs->
                                try{
                                    for(sale in saleColDocs){
                                        for(pDetails in alProductsForSales){
                                            if(pDetails.id.equals(sale.getString("id"))){
                                                val price = pDetails.price.toDouble()
                                                val quantity = sale.getString("quantity")!!.toInt()
                                                total += (price * quantity)
                                            }
                                        }
                                    }
                                    alExpandableSales.add(ExpandableSalesModel(false, docDate, total))
                                    alExpandableSales.sortWith(Comparator.comparing(ExpandableSalesModel::date).reversed())
                                    for(i in 0..alExpandableSales.size-2){
                                        if(alExpandableSales.get(i).date.equals(alExpandableSales.get(i+1).date)){
                                            alExpandableSales.removeAt(i+1)
                                        }
                                    }
                                    adapterExpandableSales.getProductsForSales()
                                    adapterExpandableSales.notifyDataSetChanged()
                                    total = 0.0
                                }catch (ex: Exception){
                                    Constants.logThis(ex.toString())
                                }
                            }
                        }
                    }catch (ex: Exception){}
                }
            }catch (ex: Exception){
                Constants.logThis(ex.toString())
            }
        }
        fun getMonthlySaleAmount(){
            monthlySale = 0.0
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
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    var qty : Int = 1
    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragSale : View = inflater.inflate(R.layout.fragment_sale, container, false)
        fabAddSale = fragSale.findViewById(R.id.fab_add_sale)
        rvExpandableSales = fragSale.findViewById(R.id.rv_expandable_sales)
        rvExpandableSales.layoutManager = LinearLayoutManager(fragSale.context)
        adapterExpandableSales = ExpandableSalesAdapter(fragSale.context, alExpandableSales)
        rvExpandableSales.adapter = adapterExpandableSales

        getSales()

        rvExpandableSales.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    android.os.Handler().postDelayed(
                        Runnable { fabAddSale.setVisibility(View.VISIBLE) },
                        1000
                    )
                } else {
                    fabAddSale.setVisibility(View.GONE)
                }
            }
        })

        fabAddSale.setOnClickListener(View.OnClickListener {
            bsAddToSale = BottomSheetDialog(fragSale.context)
            bsAddToSaleView = LayoutInflater.from(fragSale.context).inflate(R.layout.bottom_sheet_add_sale, null, false)
            bsAddToSale.setContentView(bsAddToSaleView)

            spProducts = bsAddToSaleView.findViewById(R.id.sp_products)
            etQuantity = bsAddToSaleView.findViewById(R.id.et_quantity)
            ibAdd = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_plus)
            ibMinus = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_minus)
            ibClose = bsAddToSaleView.findViewById(R.id.ib_bs_add_sale_close)
            btnAddToSale = bsAddToSaleView.findViewById(R.id.btn_bs_add_sale)
            tvTotalAmount = bsAddToSaleView.findViewById(R.id.tv_sale_total_amount)
            getProducts()

            ibClose.setOnClickListener(View.OnClickListener {
                bsAddToSale.dismiss()
            })

            spProducts?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(parent: AdapterView<*>?) {}
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    calculateTotal()
                }
            }

            etQuantity.addTextChangedListener(object : TextWatcher{
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                @SuppressLint("SetTextI18n")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if(etQuantity.text.isEmpty() || etQuantity.text.equals("0")){
                        etQuantity.setText("1")
                        qty = 1
                    }else{
                        qty = etQuantity.text.toString().toInt()
                    }
                    calculateTotal()
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            ibAdd.setOnClickListener(View.OnClickListener {
                qty = etQuantity.text.toString().toInt()
                qty++
                etQuantity.setText(qty.toString())
            })
            ibMinus.setOnClickListener(View.OnClickListener {
                qty = etQuantity.text.toString().toInt()
                if(qty>1){
                    qty--
                    etQuantity.setText(qty.toString())
                }
            })
            btnAddToSale.setOnClickListener(View.OnClickListener {
                try{

                    var qsForProductName = Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").whereEqualTo("name", spProducts.selectedItem.toString()).get()
                    qsForProductName.addOnSuccessListener {
                        pDocuments->
                        if (pDocuments.size() == 1) {
                            val pId = pDocuments.documents.get(0).getString("id")
                            var qsForSale = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}").collection("sales").whereEqualTo("id", pId).get()
                            qsForSale.addOnSuccessListener {
                                saleDocuments->
                                if(saleDocuments.size()==1){
                                    try{
                                        var qty: Int = saleDocuments.documents.get(0).getString("quantity")!!.toInt()
                                        var docId: String = saleDocuments.documents.get(0).id
                                        qty += etQuantity.text.toString().toInt()
                                        Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}").collection("sales").document(docId).update("quantity", qty.toString()).addOnSuccessListener {
                                            getSales()
                                            bsAddToSale.dismiss()
                                            Constants.toastThis(fragSale.context, "Product added to sales!")
                                        }
                                    }catch (ex: Exception){
                                        Constants.logThis(ex.toString())
                                    }
                                }else if(saleDocuments.size()==0){
                                    hmSale = HashMap<String, String>()
                                    var pId = ""
                                    var qs = Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").whereEqualTo("name", spProducts.selectedItem.toString()).get()
                                    qs.addOnSuccessListener { documents ->
                                        for (product in documents) {
                                            pId = product.getString("id").toString()
                                        }
                                        hmSale.put("id", pId)
                                        hmSale.put("quantity", etQuantity.text.toString())
                                        hmSale.put("date", Constants.getDateForDB(Constants.getDateTime()))
                                        val hmDate = HashMap<String, String>()
                                        hmDate.put("did", "rawID")
                                        Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}").set(hmDate).addOnSuccessListener {
                                            Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document("sales_${Constants.getDateForSaleDocDB(Constants.getDateTime())}").collection("sales").document().set(hmSale).addOnSuccessListener {
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
                }catch (ex: Exception){
                    Constants.logThis(ex.toString())
                }
            })

            // Below code is to gain amount with multiple to its quantity
            if(etQuantity.text.isEmpty() || etQuantity.text.equals("0")){
                etQuantity.setText("1")
                qty = 1
            }else{
                qty = etQuantity.text.toString().toInt()
            }
            calculateTotal()
            bsAddToSale.show()
        })
        return fragSale
    }
    fun getProducts(){
        alProducts.clear()
        alProductNames.clear()
        val qs: Task<QuerySnapshot> =  Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").get()
        qs.addOnSuccessListener{
                documents->
            for(product in documents){
                if(!product.getString("deleted").toBoolean()){
                    alProducts.add(ProductModel(product.getString("id").toString(), product.getString("name").toString(), product.getString("price")!!.toDouble(), product.id, product.getString("deleted").toBoolean()))
                    alProductNames.add(product.getString("name").toString())
                }
            }
            alProducts.sortWith(Comparator.comparing(ProductModel::name))
            alProductNames.sort()
            var adap = ArrayAdapter(requireContext(), R.layout.product_sp_item_ui, R.id.tv_sp_item_product_name, alProductNames)
            spProducts.adapter = adap
            calculateTotal()
        }
    }
    @SuppressLint("SetTextI18n")
    fun calculateTotal(){
        var price: Double = 0.0
        var total: Double = 0.0
        for(product in alProducts){
            if(product.name.equals(spProducts.selectedItem.toString())){
                price = product.price
            }
        }
        total = qty * price
        tvTotalAmount.setText("₹ $total")
    }
}