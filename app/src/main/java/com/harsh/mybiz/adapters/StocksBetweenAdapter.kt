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
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants

class StocksBetweenAdapter(context: Context, alStock : ArrayList<StockModel>) : RecyclerView.Adapter<StocksBetweenAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val llExpandedSale = itemView.findViewById<LinearLayout>(R.id.ll_sale_product)
        val tvStockIndex = itemView.findViewById<TextView>(R.id.tv_product_index)
        val tvStockName = itemView.findViewById<TextView>(R.id.tv_sale_product_name)
        val tvStockDate = itemView.findViewById<TextView>(R.id.tv_product_quantity)
        val tvStockTotal = itemView.findViewById<TextView>(R.id.tv_product_total)
    }
    val context = context
    val alStock = alStock
    lateinit var bsEditSaleView: View
    lateinit var bsEditSale: BottomSheetDialog
    lateinit var btnSaveSale: Button
    lateinit var etProductName: EditText
    lateinit var etProductQuantity: EditText
    lateinit var ibSaleDelete: ImageButton
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StocksBetweenAdapter.ViewHolder {
        val saleView = LayoutInflater.from(context).inflate(R.layout.expanded_sale_ui, parent, false)
        return ViewHolder(saleView)
    }

    @SuppressLint("SetTextI18n", "MissingInflatedId", "InflateParams", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: StocksBetweenAdapter.ViewHolder, position: Int) {
        val stock = alStock.get(position)
        holder.tvStockIndex.setText("${(position+1)}.")
        holder.tvStockName.setText(stock.name)
        holder.tvStockDate.setText(stock.date)
        val total = (stock.price)
        holder.tvStockTotal.setText("₹ ${total}")

    }

    override fun getItemCount(): Int {
        return alStock.size
    }
}