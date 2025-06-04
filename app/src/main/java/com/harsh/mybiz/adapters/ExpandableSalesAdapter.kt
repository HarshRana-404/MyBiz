package com.pnpbook.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.QuerySnapshot
import com.harsh.mybiz.R
import com.harsh.mybiz.adapters.ExpandedSaleAdapter
import com.harsh.mybiz.models.ExpandableSalesModel
import com.harsh.mybiz.models.ProductModel
import com.harsh.mybiz.models.ExpandedSaleModel
import com.harsh.mybiz.utilities.Constants

class ExpandableSalesAdapter(context: android.content.Context, alExpandableSale: ArrayList<ExpandableSalesModel>) : RecyclerView.Adapter<ExpandableSalesAdapter.ViewHolder>() {
    companion object{
        var alProductsForSales = ArrayList<ProductModel>()
        var alDatesTotal = ArrayList<ExpandableSalesModel>()
        val shareIntent = Intent(Intent.ACTION_SEND)
        var isShared = false
    }
    val context = context
    val alExpandableSale = alExpandableSale
    val alExpandedSale = ArrayList<ExpandedSaleModel>()
    val alShareSale = ArrayList<ExpandedSaleModel>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cvExpandableSale: CardView = itemView.findViewById(R.id.cv_expandable_sale)
        val tvSaleDate: TextView = itemView.findViewById(R.id.tv_sale_date)
        val tvSaleAmount: TextView = itemView.findViewById(R.id.tv_sale_amount)
        val rvExpandedSale: RecyclerView = itemView.findViewById(R.id.rv_expanded_sales)
        val ivExpand: ImageView = itemView.findViewById(R.id.iv_expand)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpandableSalesAdapter.ViewHolder {
        val expandableSaleView: View = LayoutInflater.from(context).inflate(R.layout.expandable_sale_ui, parent, false)
        return ViewHolder(expandableSaleView)
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ExpandableSalesAdapter.ViewHolder, position: Int) {
        isShared = false
        val expandableSale = alExpandableSale.get(position)
        holder.tvSaleDate.setText(Constants.getDocDateToShow(expandableSale.date))
        holder.tvSaleAmount.setText("₹ ${expandableSale.amount.toString()}")
        if(!expandableSale.expanded) {
            holder.ivExpand.rotation = 0.0F
            holder.rvExpandedSale.visibility = View.GONE
        }else{
            holder.ivExpand.rotation = 180.0F
            holder.rvExpandedSale.visibility = View.VISIBLE
        }
        holder.cvExpandableSale.setOnClickListener {
            if(!expandableSale.expanded) {
                holder.rvExpandedSale.visibility = View.VISIBLE
                val animExpand = TranslateAnimation(0.0F, 0.0F, -200.0F, 0.0F)
                val animToDown = RotateAnimation(0.0F, 180.0F, holder.ivExpand.pivotX, holder.ivExpand.pivotY)
                animExpand.duration = 300
                animToDown.duration = 500
                holder.ivExpand.startAnimation(animToDown)
                animToDown.setFillEnabled(true)
                animToDown.setFillAfter(true)
                alExpandedSale.clear()
                holder.rvExpandedSale.startAnimation(animExpand)
                holder.rvExpandedSale.layoutManager = LinearLayoutManager(context)
                val adapExpandedSale = ExpandedSaleAdapter(context, alExpandedSale)
                holder.rvExpandedSale.adapter = adapExpandedSale
                val docDate = "sales_${Constants.getDocDateForExpandedSale(holder.tvSaleDate.text.toString())}"
                val qsSaleDocs = Constants.fbStore.collection("businesses").document(Constants.uID)
                    .collection("sales").document(docDate).collection("sales").get()
                var index = 1
                qsSaleDocs.addOnSuccessListener {
                    saleColDocs ->
                    try {
                        for (sale in saleColDocs) {
                            for (pDetails in alProductsForSales) {
                                if (pDetails.id.equals(sale.getString("id"))) {
                                    val quantity = sale.getString("quantity")!!.toInt()
                                    alExpandedSale.add(ExpandedSaleModel(index, pDetails.id, pDetails.name, pDetails.price, quantity, Constants.getDocDateToShow(docDate), sale.id, pDetails.deleted))
                                    index++
                                    adapExpandedSale.notifyDataSetChanged()
                                    break
                                }
                            }
                        }
                    } catch (ex: Exception) {
                        Constants.logThis(ex.toString())
                    }
                }
            }else{
//                val animCollapse = TranslateAnimation(0.0F, 0.0F, 0.0F, -100.0F)
//                animCollapse.duration = 200
//                holder.rvExpandedSale.startAnimation(animCollapse)
                val animToUp = RotateAnimation(180.0F, 0.0F, holder.ivExpand.pivotX, holder.ivExpand.pivotY)
                animToUp.duration = 500
                holder.ivExpand.startAnimation(animToUp)
                holder.ivExpand.rotation = 360.0F
                holder.rvExpandedSale.visibility = View.GONE
                alExpandedSale.clear()
            }
            alExpandableSale.get(position).expanded = !alExpandableSale.get(position).expanded
        }

        holder.cvExpandableSale.setOnLongClickListener(View.OnLongClickListener {
//            Constants.toastThis(context, "App will re-start in 30 seconds...")
//            isShared = true
            alShareSale.clear()
            val docDate = "sales_${Constants.getDocDateForExpandedSale(holder.tvSaleDate.text.toString())}"
            val qsSaleDocs = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(docDate).collection("sales").get()
            var index = 1
            qsSaleDocs.addOnSuccessListener {
                    saleColDocs ->
                try {
                    for (sale in saleColDocs) {
                        for (pDetails in alProductsForSales) {
                            if (pDetails.id.equals(sale.getString("id"))) {
                                val quantity = sale.getString("quantity")!!.toInt()
                                alShareSale.add(ExpandedSaleModel(index, pDetails.id, pDetails.name, pDetails.price, quantity, Constants.getDocDateToShow(docDate), sale.id, pDetails.deleted))
                                index++
                                break
                            }
                        }
                    }
                    var shareSaleSubject: String = ""
                    var shareSaleBody: String = ""
                    shareSaleBody = ""
                    for(sale in alShareSale){
                        shareSaleBody += "${sale.index}. ${sale.name} : ${sale.quantity} × ₹ ${sale.price} = ₹ ${(sale.price*sale.quantity)}\n\n"
                    }
                    var total = 0.0
                    val qs = Constants.fbStore.collection("businesses").document(Constants.uID).get()
                    qs.addOnSuccessListener {
                        for(saleDate in alDatesTotal){
                            Constants.logThis(Constants.getYearMonthForDatesTotal(alExpandableSale.get(position).date))
                            if(Constants.getYearMonthForDatesTotal(alExpandableSale.get(position).date).equals(Constants.getYearMonthForDatesTotal(saleDate.date))){
                                total+=saleDate.amount
                            }
                        }
                        shareSaleSubject = qs.getResult().getString("name").toString()
                        shareSaleSubject += " sale [${holder.tvSaleDate.text}] : [${holder.tvSaleAmount.text}]  monthly: [₹ ${total}]"
                        shareIntent.setType("text/plain")
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSaleSubject)
                        val shareMessage = shareSaleSubject +"\n\n" + shareSaleBody
                        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage.trim())
                        context.startActivity(Intent.createChooser(shareIntent, "Share"))
                    }

//                    previously used logic which created multiple share intents...
                    /*
                    val qs = Constants.fbStore.collection("businesses").document(Constants.uID).get()
                    qs.addOnSuccessListener {
                        try {
                            var monthlySale = 0.0
                            val qsSales = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").get()
                            qsSales.addOnSuccessListener {
                                    documents->
                                try{
                                    for(sale in documents){
                                        if(sale.id.contains(Constants.getYearMonthForSales(Constants.getDateToShow("${holder.tvSaleDate.text.toString()}@")+"@"))){
                                            val qsSalesOnDate = Constants.fbStore.collection("businesses").document(Constants.uID).collection("sales").document(sale.id).collection("sales").get()
                                            qsSalesOnDate.addOnSuccessListener {
                                                    saleDoc->
                                                for(saleOnDate in saleDoc){
                                                    for(pDetails in alProductsForSales){
                                                        if(pDetails.id.equals(saleOnDate.getString("id"))){
                                                            val price = pDetails.price.toDouble()
                                                            val quantity = saleOnDate.getString("quantity")!!.toInt()
                                                            monthlySale += (price * quantity)
//                                                            shareSaleSubject = qs.getResult().getString("name").toString()
//                                                            shareSaleSubject += " sale [${holder.tvSaleDate.text}] : [${holder.tvSaleAmount.text}]  monthly: [₹ ${monthlySale}]"
//                                                            shareSaleBody = ""
//                                                            for(sale in alShareSale){
//                                                                shareSaleBody += "${sale.index}. ${sale.name} : ${sale.quantity} × ₹ ${sale.price} = ₹ ${(sale.price*sale.quantity)}\n\n"
//                                                            }
//                                                            shareIntent.setType("text/plain")
//                                                            shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSaleSubject)
//                                                            val shareMessage = shareSaleSubject +"\n\n" + shareSaleBody
//                                                            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage.trim())
//                                                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                                            break
                                                        }
                                                    }
                                                }
//                                                shareIntent.setType("text/plain")
//                                                shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSaleSubject)
//                                                val shareMessage = shareSaleSubject +"\n\n" + shareSaleBody
//                                                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage.trim())
//                                                context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                            }
                                        }
                                    }
                                }catch (ex: Exception){
                                    Constants.logThis(ex.toString())
                                }
                            }
                        }catch (ex: Exception){
                            Constants.logThis(ex.toString())
                        }
                    }
                    */
                } catch (ex: Exception) {
                    Constants.logThis(ex.toString())
                }
            }
            return@OnLongClickListener true
        })

    }

    override fun getItemCount(): Int {
        return alExpandableSale.size
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
}