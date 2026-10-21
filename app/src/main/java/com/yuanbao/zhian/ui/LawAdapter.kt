package com.yuanbao.zhian.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.yuanbao.zhian.R
import com.yuanbao.zhian.data.LawArticle

class LawAdapter(
    private val onClick: (LawArticle) -> Unit
) : RecyclerView.Adapter<LawAdapter.Holder>() {

    private val items = mutableListOf<LawArticle>()

    /** 列表中高亮的条号，用于标出「上次读到这里」 */
    var highlightId: Int = -1

    fun submit(list: List<LawArticle>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_law, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position], items[position].id == highlightId, onClick)
    }

    override fun getItemCount(): Int = items.size

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root: View = itemView.findViewById(R.id.itemRoot)
        private val title: TextView = itemView.findViewById(R.id.tvTitle)
        private val meta: TextView = itemView.findViewById(R.id.tvMeta)
        private val digest: TextView = itemView.findViewById(R.id.tvDigest)

        fun bind(article: LawArticle, highlighted: Boolean, onClick: (LawArticle) -> Unit) {
            title.text = article.title
            meta.text = article.sectionOrChapter
            digest.text = article.digest
            root.setBackgroundColor(
                if (highlighted) {
                    ContextCompat.getColor(root.context, R.color.itemHighlight)
                } else {
                    ContextCompat.getColor(root.context, android.R.color.transparent)
                }
            )
            root.setOnClickListener { onClick(article) }
        }
    }
}
