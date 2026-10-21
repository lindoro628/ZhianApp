package com.yuanbao.zhian.ui

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yuanbao.zhian.R

class WrongAdapter(
    private val rows: List<WrongFragment.Row>,
    private val onClick: (com.yuanbao.zhian.data.ProgressStore.WrongEntry) -> Unit
) : RecyclerView.Adapter<WrongAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wrong, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(rows[position], onClick)
    }

    override fun getItemCount(): Int = rows.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val meta: TextView = itemView.findViewById(R.id.tvMeta)

        fun bind(row: WrongFragment.Row, onClick: (com.yuanbao.zhian.data.ProgressStore.WrongEntry) -> Unit) {
            val e = row.entry
            val plain = row.title.replace("\n", " ")
            title.text = if (plain.length > 90) plain.substring(0, 90) + "…" else plain
            val typeText = if (e.type == 'q') "练习题" else "情景案例"
            meta.text = buildString {
                append(typeText)
                append("　错 ").append(e.count).append(" 次")
                if (e.isKeyPoint) append("　★ 重点")
                append("　累计最高 ").append(e.peak).append(" 次")
            }
            meta.gravity = Gravity.START
            itemView.setOnClickListener { onClick(e) }
        }
    }
}
