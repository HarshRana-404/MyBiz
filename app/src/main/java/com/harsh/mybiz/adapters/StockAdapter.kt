package com.harsh.mybiz.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.harsh.mybiz.R
import com.harsh.mybiz.models.StockModel
import com.harsh.mybiz.utilities.Constants

/**
 * StockAdapter with:
 *  - Two view types: ITEM (stock row) and FOOTER (Load More button + spinner)
 *  - filter() for search — updates an internal display list, no new adapter instance needed
 *  - notifyItemChanged(position) on edit/delete instead of full notifyDataSetChanged
 *  - onLoadMoreClick callback wired to the footer button
 */
class StockAdapter(
    private val context: Context,
    /** The full master list — never filtered, used as source of truth */
    private val alStocksMaster: MutableList<StockModel>,
    private val onLoadMoreClick: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM   = 0
        private const val VIEW_TYPE_FOOTER = 1
    }

    /** The currently displayed list — either full or filtered */
    private val alDisplay = mutableListOf<StockModel>().also { it.addAll(alStocksMaster) }

    var showLoadMore: Boolean = false
        set(value) { field = value; notifyItemChanged(alDisplay.size) }

    var isLoading: Boolean = false
        set(value) { field = value; notifyItemChanged(alDisplay.size) }

    // ─── ViewHolders ────────────────────────────────────────────────────────

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cvStock: CardView   = itemView.findViewById(R.id.cv_stock)
        val tvName: TextView    = itemView.findViewById(R.id.tv_stock_name)
        val tvPrice: TextView   = itemView.findViewById(R.id.tv_stock_price)
        val tvDate: TextView    = itemView.findViewById(R.id.tv_stock_date)
    }

    class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pbLoadMore: ProgressBar = itemView.findViewById(R.id.pb_load_more_stocks)
        val btnLoadMore: Button     = itemView.findViewById(R.id.btn_load_more_stocks)
    }

    // ─── Adapter overrides ──────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int =
        if (position < alDisplay.size) VIEW_TYPE_ITEM else VIEW_TYPE_FOOTER

    override fun getItemCount(): Int = alDisplay.size + 1  // +1 for footer

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(context)
        return if (viewType == VIEW_TYPE_ITEM) {
            ItemViewHolder(inflater.inflate(R.layout.stock_ui, parent, false))
        } else {
            FooterViewHolder(inflater.inflate(R.layout.stock_footer_ui, parent, false))
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is FooterViewHolder) {
            holder.pbLoadMore.visibility  = if (isLoading)   View.VISIBLE else View.GONE
            holder.btnLoadMore.visibility = if (showLoadMore && !isLoading) View.VISIBLE else View.GONE
            holder.btnLoadMore.setOnClickListener { onLoadMoreClick() }
            return
        }

        val vh    = holder as ItemViewHolder
        val stock = alDisplay[position]

        vh.tvName.text  = stock.name
        vh.tvPrice.text = "₹ ${stock.price}"
        vh.tvDate.text  = Constants.getDateToShow(stock.date)

        vh.cvStock.setOnClickListener {
            showEditBottomSheet(stock, position)
        }
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Filters the display list by [query]. Pass empty string to reset.
     * Does NOT create a new adapter — just swaps the display list and notifies.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        alDisplay.clear()
        if (query.isEmpty()) {
            alDisplay.addAll(alStocksMaster)
        } else {
            val q = query.lowercase()
            alStocksMaster.filterTo(alDisplay) { stock ->
                stock.name.lowercase().contains(q) ||
                Constants.getDateToShow(stock.date).contains(q)
            }
        }
        notifyDataSetChanged()
    }

    /** Call after adding new items to alStocksMaster to refresh the display list. */
    @SuppressLint("NotifyDataSetChanged")
    fun refreshFromMaster() {
        alDisplay.clear()
        alDisplay.addAll(alStocksMaster)
        notifyDataSetChanged()
    }

    // ─── Edit bottom sheet ───────────────────────────────────────────────────

    private fun showEditBottomSheet(stock: StockModel, position: Int) {
        try {
            val bsEditStock     = BottomSheetDialog(context)
            val bsEditStockView = LayoutInflater.from(context)
                .inflate(R.layout.bottom_sheet_edit_stock, null, false)
            bsEditStock.setContentView(bsEditStockView)

            val etName    = bsEditStockView.findViewById<EditText>(R.id.et_bs_edit_stock_name)
            val etPrice   = bsEditStockView.findViewById<EditText>(R.id.et_bs_edit_stock_price)
            val btnSave   = bsEditStockView.findViewById<Button>(R.id.btn_bs_edit_stock_save)
            val ibDelete  = bsEditStockView.findViewById<ImageButton>(R.id.ib_bs_edit_stock_delete)

            etName.setText(stock.name)
            etPrice.setText(stock.price.toString())

            ibDelete.setOnClickListener {
                AlertDialog.Builder(context, R.style.myAlertDialogTheme)
                    .setTitle("Delete?")
                    .setMessage("Are you sure you want to delete ${stock.name}?")
                    .setPositiveButton("YES") { dialog, _ ->
                        Constants.fbStore.collection("businesses").document(Constants.uID)
                            .collection("stocks").document(stock.docId)
                            .delete()
                            .addOnSuccessListener {
                                Constants.toastThis(context, "Stock deleted!")
                                dialog.dismiss()
                                bsEditStock.dismiss()
                                // Remove from both master and display lists
                                alStocksMaster.remove(stock)
                                alDisplay.remove(stock)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, alDisplay.size - position)
                            }
                    }
                    .setNegativeButton("NO", null)
                    .show()
            }

            btnSave.setOnClickListener {
                val newName  = etName.text.toString().trim()
                val newPrice = etPrice.text.toString().trim()
                if (newName.isNotEmpty() && newPrice.isNotEmpty()) {
                    Constants.fbStore.collection("businesses").document(Constants.uID)
                        .collection("stocks").document(stock.docId)
                        .update("name", newName, "price", newPrice)
                        .addOnSuccessListener {
                            Constants.toastThis(context, "Stock details saved!")
                            bsEditStock.dismiss()
                            // Update model in-place — only rebind this one row
                            stock.name  = newName
                            stock.price = newPrice.toDouble()
                            notifyItemChanged(position)
                        }
                } else {
                    Constants.toastThis(context, "Enter all details!")
                }
            }

            bsEditStock.show()
        } catch (ex: Exception) {
            Constants.logThis(ex.toString())
        }
    }
}
