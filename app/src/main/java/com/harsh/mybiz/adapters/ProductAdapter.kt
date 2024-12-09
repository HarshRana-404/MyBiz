package com.harsh.mybiz.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.utilities.Constants

class ProductAdapter(context: Context, alProducts: ArrayList<ProductModel>): RecyclerView.Adapter<ProductAdapter.ViewHolder>(){
    val alProducts = alProducts
    val context = context

    lateinit var bsEditProduct: BottomSheetDialog
    lateinit var bsEditProductView: View
    lateinit var ibBSDelete: ImageButton
    lateinit var etProductName: EditText
    lateinit var etProductPrice: EditText
    lateinit var btnSaveProduct: Button
    lateinit var productName: String
    lateinit var productPrice: String
    var alreadyExists: Boolean = false

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cvProduct: CardView = itemView.findViewById(R.id.cv_product)
        val tvProductName: TextView = itemView.findViewById(R.id.tv_product_name)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tv_product_price)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val productView: View = LayoutInflater.from(context).inflate(R.layout.product_ui, parent, false)
        return ViewHolder(productView)
    }

    override fun getItemCount(): Int {
        return alProducts.size
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: ProductModel = alProducts.get(position)
        holder.tvProductName.setText(product.name)
        holder.tvProductPrice.setText("₹ ${product.price}")
        try {
            holder.cvProduct.setOnClickListener(View.OnClickListener {
                bsEditProduct = BottomSheetDialog(context)
                bsEditProductView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_edit_product, null, false)
                bsEditProduct.setContentView(bsEditProductView)
                etProductName = bsEditProductView.findViewById(R.id.et_bs_edit_product_name)
                etProductPrice = bsEditProductView.findViewById(R.id.et_bs_edit_product_price)
                btnSaveProduct = bsEditProductView.findViewById(R.id.btn_bs_edit_save_product)
                ibBSDelete= bsEditProductView.findViewById(R.id.ib_bs_add_product_delete)

                etProductName.setText(product.name)
                etProductPrice.setText(product.price.toString())

                ibBSDelete.setOnClickListener(View.OnClickListener {
                    var adb : AlertDialog.Builder = AlertDialog.Builder(context, R.style.myAlertDialogTheme)
                    adb.setTitle("Delete?")
                    adb.setMessage("Are you sure you want to delete ${product.name}?")
                    var ad: AlertDialog = adb.create()

                    adb.setPositiveButton("YES"){
                        dialog, which -> Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").document(product.docId).update("deleted", "true").addOnSuccessListener {
                        Constants.toastThis(context, "Product deleted!")
                        ad.dismiss()
                        bsEditProduct.dismiss()
                        alProducts.removeAt(position)
                        notifyDataSetChanged()
                        }
                    }
                    adb.setNegativeButton("NO"){
                        dialog, which -> ad.dismiss()
                    }
                    adb.show()
                })

                btnSaveProduct.setOnClickListener(View.OnClickListener {
                    alreadyExists = false
                    productName = etProductName.text.toString().trim()
                    productPrice = etProductPrice.text.toString().trim()
                    if(!productName.isEmpty() && !productPrice.isEmpty()){
                        val qs: Task<QuerySnapshot> = Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").whereEqualTo("name", productName).get()
                        qs.addOnSuccessListener {
                                documents->
                            for(productDoc in documents){
                                if(!productDoc.id.equals(product.docId)) {
                                    alreadyExists = true
                                }
                            }
                            if(!alreadyExists){
                                Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").document(product.docId).update("name", productName).addOnSuccessListener {
                                    Constants.fbStore.collection("businesses").document(Constants.uID).collection("products").document(product.docId).update("price", productPrice).addOnSuccessListener {
                                        Constants.toastThis(context, "Product details saved!")
                                        bsEditProduct.dismiss()
                                        product.name = productName
                                        product.price = productPrice.toDouble()
                                        notifyDataSetChanged()
                                    }
                                }
                            }else{
                                Constants.toastThis(context, "Product already exists!")
                            }
                        }
                    }else{
                        Constants.toastThis(context, "Enter all details!")
                    }
                })
                bsEditProduct.show()

            })
        }catch (ex: Exception){}
    }
}