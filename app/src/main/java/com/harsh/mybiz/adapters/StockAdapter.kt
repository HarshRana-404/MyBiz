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
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants

class StockAdapter(context: Context, alStocks: ArrayList<StockModel>): RecyclerView.Adapter<StockAdapter.ViewHolder>(){
    val alStocks = alStocks
    val context = context

    lateinit var bsEditStock: BottomSheetDialog
    lateinit var bsEditStockView: View
    lateinit var ibBSDelete: ImageButton
    lateinit var etStockName: EditText
    lateinit var etStockPrice: EditText
    lateinit var btnSaveStock: Button
    lateinit var StockName: String
    lateinit var StockPrice: String
    var alreadyExists: Boolean = false

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cvStock: CardView = itemView.findViewById(R.id.cv_stock)
        val tvStockName: TextView = itemView.findViewById(R.id.tv_stock_name)
        val tvStockPrice: TextView = itemView.findViewById(R.id.tv_stock_price)
        val tvStockDate: TextView = itemView.findViewById(R.id.tv_stock_date)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val StockView: View = LayoutInflater.from(context).inflate(R.layout.stock_ui, parent, false)
        return ViewHolder(StockView)
    }

    override fun getItemCount(): Int {
        return alStocks.size
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val stock: StockModel = alStocks.get(position)
        holder.tvStockName.setText(stock.name)
        holder.tvStockPrice.setText("₹ ${stock.price}")
        holder.tvStockDate.setText(Constants.getDateToShow(stock.date))
        try {
            holder.cvStock.setOnClickListener(View.OnClickListener {
                bsEditStock = BottomSheetDialog(context)
                bsEditStockView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_edit_stock, null, false)
                bsEditStock.setContentView(bsEditStockView)
                etStockName = bsEditStockView.findViewById(R.id.et_bs_edit_stock_name)
                etStockPrice = bsEditStockView.findViewById(R.id.et_bs_edit_stock_price)
                btnSaveStock = bsEditStockView.findViewById(R.id.btn_bs_edit_stock_save)
                ibBSDelete= bsEditStockView.findViewById(R.id.ib_bs_edit_stock_delete)

                etStockName.setText(stock.name)
                etStockPrice.setText(stock.price.toString())

                ibBSDelete.setOnClickListener(View.OnClickListener {
                    var adb : AlertDialog.Builder = AlertDialog.Builder(context, R.style.myAlertDialogTheme)
                    adb.setTitle("Delete?")
                    adb.setMessage("Are you sure you want to delete ${stock.name}?")
                    var ad: AlertDialog = adb.create()

                    adb.setPositiveButton("YES"){
                        dialog, which -> Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").document(stock.docId).delete().addOnSuccessListener {
                        Constants.toastThis(context, "Stock deleted!")
                        ad.dismiss()
                        bsEditStock.dismiss()
                        alStocks.removeAt(position)
                        notifyDataSetChanged()
                        }
                    }
                    adb.setNegativeButton("NO"){
                        dialog, which -> ad.dismiss()
                    }
                    adb.show()
                })

                btnSaveStock.setOnClickListener(View.OnClickListener {
                    alreadyExists = false
                    StockName = etStockName.text.toString().trim()
                    StockPrice = etStockPrice.text.toString().trim()
                    if(!StockName.isEmpty() && !StockPrice.isEmpty()){
                        Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").document(stock.docId).update("name", StockName).addOnSuccessListener {
                            Constants.fbStore.collection("businesses").document(Constants.uID).collection("stocks").document(stock.docId).update("price", StockPrice).addOnSuccessListener {
                                Constants.toastThis(context, "Stock details saved!")
                                bsEditStock.dismiss()
                                stock.name = StockName
                                stock.price = StockPrice.toDouble()
                                notifyDataSetChanged()
                            }
                    }
                    }else{
                        Constants.toastThis(context, "Enter all details!")
                    }
                })
                bsEditStock.show()

            })
        }catch (ex: Exception){}
    }
}