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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDragHandleView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.adapters.ProductAdapter
import com.harsh.mybiz.fragments.SaleFragment.Companion
import com.harsh.mybiz.fragments.SaleFragment.Companion.fabAddSale
import com.harsh.mybiz.fragments.SaleFragment.Companion.rvExpandableSales
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.utilities.Constants

class ProductsFragment : Fragment() {
    var alProducts: ArrayList<ProductModel> = ArrayList()
    var alSearchResultProducts: ArrayList<ProductModel> = ArrayList()
    lateinit var rvProducts: RecyclerView
    lateinit var productsAdapter: ProductAdapter
    lateinit var fabAddProduct: FloatingActionButton
    lateinit var hmProduct: HashMap<String, String>
    lateinit var bsAddProduct: BottomSheetDialog
    lateinit var bsAddProductView: View
    lateinit var ibBSClose: ImageButton
    lateinit var etProductName: EditText
    lateinit var etProductPrice: EditText
    lateinit var btnAddProduct: Button
    lateinit var productName: String
    lateinit var productPrice: String
    lateinit var productSearch: String
    lateinit var etProductSearch: EditText

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val fragProducts : View = inflater.inflate(R.layout.fragment_products, container, false)

        try{
            rvProducts = fragProducts.findViewById(R.id.rv_products)
            fabAddProduct = fragProducts.findViewById(R.id.fab_add_product)
            etProductSearch = fragProducts.findViewById(R.id.et_search_product)
            rvProducts.layoutManager = LinearLayoutManager(fragProducts.context)
            productsAdapter = ProductAdapter(fragProducts.context, alProducts)
            rvProducts.adapter = productsAdapter

            getProducts()

            etProductSearch.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {

                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    productSearch = etProductSearch.text.toString()
                    if(!productSearch.isEmpty()){
                        try {
                            alSearchResultProducts.clear()
                            for(product in alProducts){
                                if(product.name.contains(productSearch)){
                                    alSearchResultProducts.add(product)
                                }
                            }
                            productsAdapter = ProductAdapter(fragProducts.context, alSearchResultProducts)
                            rvProducts.adapter = productsAdapter
                            productsAdapter.notifyDataSetChanged()
                        }catch (ex: Exception){
                            Constants.logThis(ex.toString())
                        }
                    }else{
                        productsAdapter = ProductAdapter(fragProducts.context, alProducts)
                        rvProducts.adapter = productsAdapter
                        productsAdapter.notifyDataSetChanged()
                    }
                }
            })

            rvProducts.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        android.os.Handler().postDelayed(
                            Runnable { fabAddProduct.setVisibility(View.VISIBLE) },
                            1000
                        )
                    } else {
                        fabAddProduct.setVisibility(View.GONE)
                    }
                }
            })

            fabAddProduct.setOnClickListener(View.OnClickListener {
                bsAddProduct = BottomSheetDialog(fragProducts.context)
                bsAddProductView = LayoutInflater.from(fragProducts.context).inflate(R.layout.bottom_sheet_add_product, null, false)
                bsAddProduct.setContentView(bsAddProductView)
                etProductName = bsAddProductView.findViewById(R.id.et_bs_add_product_name)
                etProductPrice = bsAddProductView.findViewById(R.id.et_bs_add_product_price)
                btnAddProduct = bsAddProductView.findViewById(R.id.btn_bs_add_product)
                ibBSClose = bsAddProductView.findViewById(R.id.ib_bs_add_product_close)

                ibBSClose.setOnClickListener(View.OnClickListener {
                    bsAddProduct.dismiss()
                })

                btnAddProduct.setOnClickListener(View.OnClickListener {
                    productName = etProductName.text.toString().trim()
                    productPrice = etProductPrice.text.toString().trim()
                    if(!productName.isEmpty() && !productPrice.isEmpty()){
                        hmProduct = HashMap()
                        var alreadyExists: Boolean = false
                        for(product in alProducts){
                            if(product.name.equals(productName)){
                                Constants.toastThis(fragProducts.context, "This product already exists!")
                                alreadyExists = true
                            }
                        }
                        if(!alreadyExists){
                            try{
                                hmProduct.put("id", Constants.getUUIDForText(productName.toLowerCase().toString()))
                                hmProduct.put("name", productName)
                                hmProduct.put("price", productPrice)
                                hmProduct.put("deleted", "false")
                                Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").document().set(hmProduct).addOnSuccessListener {
                                    bsAddProduct.dismiss()
                                    Constants.toastThis(fragProducts.context, "Product Added!")
                                    getProducts()
                                }
                            }catch (ex: Exception){}
                        }
                    }else{
                        Constants.toastThis(fragProducts.context, "Enter all details!")
                    }
                })
                bsAddProduct.show()
            })
        }catch (ex: Exception){
            Constants.logThis(ex.toString())
        }

        return fragProducts
    }

    @SuppressLint("NotifyDataSetChanged")
    fun getProducts(){
        alProducts.clear()
        val qs: Task<QuerySnapshot> =  Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").get()
        qs.addOnSuccessListener{
            documents->
            for(product in documents){
                if(!product.getString("deleted").toBoolean()) {
                    alProducts.add(ProductModel(product.getString("id").toString(), product.getString("name").toString(), product.getString("price")!!.toDouble(), product.id, product.getString("deleted").toBoolean()))
                }
            }
            alProducts.sortWith(Comparator.comparing(ProductModel::name))
            productsAdapter.notifyDataSetChanged()
        }
    }
}