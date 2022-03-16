package com.ndhzs.bottomsheettest

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

/**
 * ...
 * @author 985892345 (Guo Xiangrui)
 * @email 2767465918@qq.com
 * @date 2022/3/14 21:25
 */
class RvAdapter : RecyclerView.Adapter<RvAdapter.ViewH>() {
    class ViewH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tv = itemView.findViewById<TextView>(R.id.item_tv)
        val cv = itemView.findViewById<CardView>(R.id.item_cv)
        init {
            cv.setOnClickListener {
                Toast.makeText(it.context, tv.text, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_item, parent, false)
        return ViewH(view)
    }

    override fun onBindViewHolder(holder: ViewH, position: Int) {
        holder.tv.text = position.toString()
    }

    override fun getItemCount(): Int  = 10
}