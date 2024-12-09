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
import com.harsh.mybiz.models.ExpandedSaleModel
import com.harsh.mybiz.utilities.Constants

class SalesBetweenAdapter(context: Context, alSales : ArrayList<ExpandedSaleModel>) : RecyclerView.Adapter<SalesBetweenAdapter.ViewHolder>() {
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesBetweenAdapter.ViewHolder {
        val saleView = LayoutInflater.from(context).inflate(R.layout.expanded_sale_ui, parent, false)
        return ViewHolder(saleView)
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId", "InflateParams", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: SalesBetweenAdapter.ViewHolder, position: Int) {
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

    }

    override fun getItemCount(): Int {
        return alSales.size
    }
}