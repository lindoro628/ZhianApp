package com.yuanbao.zhian.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuanbao.zhian.LawDetailActivity
import com.yuanbao.zhian.R
import com.yuanbao.zhian.data.LawArticle
import com.yuanbao.zhian.data.ProgressStore
import com.yuanbao.zhian.data.Repo

class LawFragment : Fragment(R.layout.fragment_law) {

    private lateinit var adapter: LawAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var emptyView: TextView
    private var all: List<LawArticle> = emptyList()
    private var filterPos = 0
    private var keyword = ""

    /** 上次读到的条号，返回列表时高亮并滚回该位置 */
    private var lastReadId = -1
    private var savedTopPosition = 0
    private var savedTopOffset = 0
    private var restorePending = false

    /** 从详情页返回时带回最后停留的条号 */
    private val detailLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = result.data?.getIntExtra(LawDetailActivity.EXTRA_ID, -1) ?: -1
        if (id > 0) {
            lastReadId = id
            restorePending = true
            apply()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val spinner = view.findViewById<Spinner>(R.id.spinChapter)
        val search = view.findViewById<EditText>(R.id.etSearch)
        val rv = view.findViewById<RecyclerView>(R.id.rvLaw)
        emptyView = view.findViewById(R.id.tvEmpty)

        all = Repo.laws(requireContext())
        adapter = LawAdapter { article ->
            // 记住当前列表滚动位置，返回时接着往下读
            savedTopPosition = layoutManager.findFirstVisibleItemPosition()
            val firstView = layoutManager.findViewByPosition(savedTopPosition)
            savedTopOffset = firstView?.top ?: 0
            lastReadId = article.id
            restorePending = true
            detailLauncher.launch(LawDetailActivity.newIntent(requireContext(), article.id))
        }
        layoutManager = LinearLayoutManager(requireContext())
        rv.layoutManager = layoutManager
        rv.adapter = adapter

        val options = listOf(
            "全部条文",
            "第一章 总则",
            "第二章 处罚的种类和适用",
            "第三章 违反治安管理的行为和处罚",
            "第四章 处罚程序",
            "第五章 执法监督",
            "第六章 附则",
            "★ 我的收藏"
        )
        spinner.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                filterPos = pos
                apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        search.addTextChangedListener {
            keyword = it?.toString()?.trim().orEmpty()
            apply()
        }

        apply()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) apply()
    }

    private fun apply() {
        val favSet = ProgressStore.favorites(requireContext())
        var list = when (filterPos) {
            0 -> all
            7 -> all.filter { favSet.contains(it.id) }
            else -> {
                val prefix = when (filterPos) {
                    1 -> "第一章"
                    2 -> "第二章"
                    3 -> "第三章"
                    4 -> "第四章"
                    5 -> "第五章"
                    else -> "第六章"
                }
                all.filter { it.chapter.startsWith(prefix) }
            }
        }
        if (keyword.isNotEmpty()) {
            list = list.filter {
                it.text.contains(keyword) ||
                    it.chapter.contains(keyword) ||
                    it.section.contains(keyword) ||
                    it.id.toString() == keyword ||
                    ("第${it.id}条").contains(keyword)
            }
        }

        adapter.highlightId = lastReadId
        adapter.submit(list)

        if (list.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            emptyView.text = if (keyword.isEmpty() && filterPos == 7) {
                "还没有收藏任何条文，点击条文后在详情页收藏。"
            } else {
                "没有匹配的条文，换个关键词试试。"
            }
            return
        }
        emptyView.visibility = View.GONE

        if (restorePending) {
            restorePending = false
            val pos = list.indexOfFirst { it.id == lastReadId }
            if (pos >= 0) {
                // 该条在可见范围内就不打断视线，否则滚到列表中部
                val first = layoutManager.findFirstVisibleItemPosition()
                val last = layoutManager.findLastVisibleItemPosition()
                if (pos in first..last) {
                    layoutManager.scrollToPositionWithOffset(savedTopPosition, savedTopOffset)
                } else {
                    layoutManager.scrollToPositionWithOffset(pos, 0)
                }
            } else {
                layoutManager.scrollToPositionWithOffset(savedTopPosition, savedTopOffset)
            }
        }
    }
}
