package com.harsh.mybiz.models

data class ExpandedSaleModel(val index: Int, val id: String, var name: String, var price: Double, val quantity:Int, val date: String, val docId : String, val deleted : Boolean) {
}