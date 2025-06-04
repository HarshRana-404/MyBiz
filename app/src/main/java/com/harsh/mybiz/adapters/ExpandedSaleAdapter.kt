package com.harsh.mybiz.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.harsh.mybiz.R
import com.harsh.mybiz.fragments.SaleFragment
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.ExpandedSaleModel
import com.harsh.mybiz.utilities.Constants

class ExpandedSaleAdapter(context: Context, alSales : ArrayList<ExpandedSaleModel>) : RecyclerView.Adapter<ExpandedSaleAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val llExpandedSale = itemView.findViewById<LinearLayout>(R.id.ll_sale_product)
        val tvProductIndex = itemView.findViewById<TextView>(R.id.tv_product_index)
        val tvProductName = itemView.findViewById<TextView>(R.id.tv_sale_product_name)
        val tvProductQuantity = itemView.findViewById<TextView>(R.id.tv_product_quantity)
        val tvProductTotal = itemView.findViewById<TextView>(R.id.tv_product_total)
    }
    val context = context
    val alSales = alSales
    lateinit var bsEditSaleView: View
    lateinit var bsEditSale: BottomSheetDialog
    lateinit var btnSaveSale: Button
    lateinit var etProductName: EditText
    lateinit var etProductQuantity: EditText
    lateinit var ibSaleDelete: ImageButton
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpandedSaleAdapter.ViewHolder {
        val saleView = LayoutInflater.from(context).inflate(R.layout.expanded_sale_ui, parent, false)
        return ViewHolder(saleView)
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId", "InflateParams", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ExpandedSaleAdapter.ViewHolder, position: Int) {
        val sale = alSales.get(position)
        if(sale.deleted){
            holder.tvProductIndex.setTextColor(context.resources.getColor(R.color.red))
            holder.tvProductName.setTextColor(context.resources.getColor(R.color.red))
            holder.tvProductQuantity.setTextColor(context.resources.getColor(R.color.red))
            holder.tvProductTotal.setTextColor(context.resources.getColor(R.color.red))
        }
        holder.tvProductIndex.setText("${sale.index}.")
        holder.tvProductName.setText(sale.name)
        holder.tvProductQuantity.setText(sale.quantity.toString())
        val total = (sale.price * sale.quantity)
        holder.tvProductTotal.setText("₹ ${total}")

        holder.llExpandedSale.setOnClickListener {
            try{
                bsEditSale = BottomSheetDialog(context)
                bsEditSaleView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_edit_sale, null, false)
                bsEditSale.setContentView(bsEditSaleView)
                etProductName = bsEditSaleView.findViewById(R.id.et_bs_edit_sale_product_name)
                etProductQuantity = bsEditSaleView.findViewById(R.id.et_bs_edit_sale_quantity)
                btnSaveSale = bsEditSaleView.findViewById(R.id.btn_bs_edit_sale)
                ibSaleDelete = bsEditSaleView.findViewById(R.id.ib_bs_edit_sale_delete)

                etProductName.setText(sale.name)
                etProductQuantity.setText(sale.quantity.toString())

                ibSaleDelete.setOnClickListener(View.OnClickListener {
                    try{
                        var adb : AlertDialog.Builder = AlertDialog.Builder(context, R.style.myAlertDialogTheme)
                        adb.setTitle("Delete?")
                        adb.setMessage("Are you sure you want to delete ${sale.name}?")
                        var ad: AlertDialog = adb.create()
                        var saleId = "@${sale.date}"
                        saleId = "sales_${Constants.getDateForExpandedDocDB(saleId)}"
                        adb.setPositiveButton("YES"){
                                dialog, which -> Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(saleId).collection("sales").document(sale.docId).delete().addOnSuccessListener {
                            Constants.toastThis(context, "${sale.name} deleted!")
                            ad.dismiss()
                            bsEditSale.dismiss()
//                            alSales.removeAt(position)
//                            notifyDataSetChanged()
                            SaleFragment.refreshSaleAdapter()
                        }
                        }
                        adb.setNegativeButton("NO"){
                                dialog, which -> ad.dismiss()
                        }
                        adb.show()
                    }catch (ex: Exception){
                        Constants.logThis(ex.toString())
                    }
                })

                btnSaveSale.setOnClickListener{
                    if(!etProductQuantity.text.isEmpty() || etProductQuantity.text.toString().equals("0")){
                        Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document("sales_${Constants.getDocDateForExpandedSale(sale.date)}").collection("sales").document(sale.docId).update("quantity", etProductQuantity.text.toString()).addOnSuccessListener {
                            bsEditSale.dismiss()
                            Constants.toastThis(context, "Sales updated!")
                            SaleFragment.refreshSaleAdapter()
                        }
                    }
                }

                bsEditSale.show()
            }catch (ex: Exception){
                Constants.logThis(ex.toString())
            }
        }

    }

    override fun getItemCount(): Int {
        return alSales.size
    }
}