package com.yuanbao.zhian.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuanbao.zhian.LawDetailActivity
import com.yuanbao.zhian.R
import com.yuanbao.zhian.data.ProgressStore
import com.yuanbao.zhian.data.Repo

class WrongFragment : Fragment(R.layout.fragment_wrong) {

    private lateinit var listPart: LinearLayout
    private lateinit var practicePart: ScrollView
    private lateinit var tvStat: TextView
    private lateinit var spinFilter: Spinner
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    private lateinit var tvProg: TextView
    private lateinit var badgeBox: LinearLayout
    private lateinit var tvScene: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var tvResult: TextView
    private lateinit var tvAnalysis: TextView
    private lateinit var btnRef: Button
    private lateinit var btnNext: MaterialButton
    private val optionButtons = ArrayList<MaterialButton>(4)

    private var cases: List<com.yuanbao.zhian.data.CaseItem> = emptyList()
    private var quizList: List<com.yuanbao.zhian.data.QuizQuestion> = emptyList()

    private var queue = ArrayList<ProgressStore.WrongEntry>()
    private var current: Current? = null
    private var answered = false
    private var done = 0
    private var right = 0

    private data class Current(
        val entry: ProgressStore.WrongEntry,
        val title: String,
        val scene: String?,
        val options: List<String>,
        val answer: Int,
        val analysis: String,
        val ref: Int
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listPart = view.findViewById(R.id.listPart)
        practicePart = view.findViewById(R.id.practicePart)
        tvStat = view.findViewById(R.id.tvStat)
        spinFilter = view.findViewById(R.id.spinFilter)
        rv = view.findViewById(R.id.rvWrong)
        tvEmpty = view.findViewById(R.id.tvEmpty)

        tvProg = view.findViewById(R.id.tvProg)
        badgeBox = view.findViewById(R.id.badgeBox)
        tvScene = view.findViewById(R.id.tvScene)
        tvQuestion = view.findViewById(R.id.tvQuestion)
        resultCard = view.findViewById(R.id.resultCard)
        tvResult = view.findViewById(R.id.tvResult)
        tvAnalysis = view.findViewById(R.id.tvAnalysis)
        btnRef = view.findViewById(R.id.btnRef)
        btnNext = view.findViewById(R.id.btnNext)

        optionButtons.clear()
        for (i in 0..3) {
            val id = resources.getIdentifier("btnOpt$i", "id", requireContext().packageName)
            optionButtons.add(view.findViewById(id))
        }

        quizList = Repo.quiz(requireContext())
        cases = Repo.cases(requireContext())

        val filters = listOf("全部错题（按错误次数排序）", "★ 仅重点（错误 > 3 次）", "仅练习题", "仅情景案例")
        spinFilter.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            filters
        )
        spinFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                renderList()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        rv.layoutManager = LinearLayoutManager(requireContext())

        view.findViewById<Button>(R.id.btnPracticeAll).setOnClickListener {
            startPractice(entries())
        }
        view.findViewById<Button>(R.id.btnBackList).setOnClickListener {
            showList()
        }

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { choose(index) }
        }

        renderList()
        showList()
    }

    override fun onResume() {
        super.onResume()
        // 练习过程中不重绘列表，避免打断当前这一轮
        if (practicePart.visibility != View.VISIBLE) renderList()
    }

    private fun entries(): List<ProgressStore.WrongEntry> {
        val all = ProgressStore.wrongEntries(requireContext())
        return when (spinFilter.selectedItemPosition) {
            1 -> all.filter { it.isKeyPoint }
            2 -> all.filter { it.type == 'q' }
            3 -> all.filter { it.type == 'c' }
            else -> all
        }
    }

    private fun resolve(entry: ProgressStore.WrongEntry): Current? =
        if (entry.type == 'q') {
            quizList.firstOrNull { it.index == entry.index }?.let {
                Current(entry, it.q, null, it.options, it.answer, it.analysis, it.ref)
            }
        } else {
            cases.firstOrNull { it.index == entry.index }?.let {
                Current(entry, it.question, it.scene, it.options, it.answer, it.analysis, it.ref)
            }
        }

    private fun renderList() {
        val list = entries()
        val total = ProgressStore.wrongCount(requireContext())
        val kp = ProgressStore.keyPointCount(requireContext())
        tvStat.text = "共 $total 道错题，其中重点 $kp 道" +
            if (list.size != total) "（当前筛选 ${list.size} 道）" else ""

        val rows = list.mapNotNull { entry ->
            val c = resolve(entry) ?: return@mapNotNull null
            Row(entry, c.title)
        }

        rv.adapter = WrongAdapter(rows) { entry ->
            startPractice(listOf(entry))
        }
        val empty = rows.isEmpty()
        rv.visibility = if (empty) View.GONE else View.VISIBLE
        tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    data class Row(val entry: ProgressStore.WrongEntry, val title: String)

    private fun showList() {
        listPart.visibility = View.VISIBLE
        practicePart.visibility = View.GONE
        renderList()
    }

    private fun showPractice() {
        listPart.visibility = View.GONE
        practicePart.visibility = View.VISIBLE
    }

    private fun startPractice(list: List<ProgressStore.WrongEntry>) {
        queue = ArrayList(list)
        done = 0
        right = 0
        if (queue.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("没有可练习的错题")
                .setMessage("当前筛选条件下没有错题，换个筛选或先去做题吧。")
                .setPositiveButton("知道了", null)
                .show()
            return
        }
        showPractice()
        next()
    }

    private fun next() {
        answered = false
        if (queue.isEmpty()) {
            finishRound()
            return
        }
        val entry = queue.first()
        val c = resolve(entry)
        if (c == null) {
            queue.removeAt(0)
            next()
            return
        }
        current = c
        tvProg.text = "错题库 ${done + 1}/${done + queue.size}　已答对 $right"

        badgeBox.removeAllViews()
        badgeBox.addView(badge(if (entry.type == 'q') "练习题" else "情景案例", entry.type == 'c'))
        if (entry.isKeyPoint) {
            badgeBox.addView(badge("★ 重点（累计错 ${entry.peak} 次）", false, keyPoint = true))
        } else {
            badgeBox.addView(badge("待清偿 ${entry.count} 次", false))
        }

        if (c.scene == null) {
            tvScene.visibility = View.GONE
        } else {
            tvScene.visibility = View.VISIBLE
            tvScene.text = c.scene
        }
        tvQuestion.text = c.title

        optionButtons.forEach { it.visibility = View.VISIBLE }
        c.options.forEachIndexed { index, text ->
            optionButtons[index].text = "${'A' + index}. $text"
            optionButtons[index].backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.optionNormal)
            )
            optionButtons[index].isEnabled = true
        }
        resultCard.visibility = View.GONE
        btnNext.isEnabled = false
        btnNext.text = if (queue.size > 1) "下一题" else "完成本轮"
        btnNext.setOnClickListener {
            if (queue.isNotEmpty()) queue.removeAt(0)
            next()
        }
        practicePart.scrollTo(0, 0)
    }

    private fun badge(text: String, isCase: Boolean, keyPoint: Boolean = false): TextView {
        val tv = TextView(requireContext())
        tv.text = text
        tv.textSize = 11f
        tv.setPadding(14, 4, 14, 4)
        tv.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (keyPoint) R.color.optionWrong else if (isCase) R.color.badgeCase else R.color.colorPrimary
            )
        )
        tv.background = badgeBg(keyPoint, isCase)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.marginEnd = 12
        tv.layoutParams = lp
        return tv
    }

    private fun badgeBg(keyPoint: Boolean, isCase: Boolean) = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = 10f
        setColor(
            ContextCompat.getColor(
                requireContext(),
                when {
                    keyPoint -> R.color.badgeKeyBg
                    isCase -> R.color.badgeCaseBg
                    else -> R.color.badgeQuizBg
                }
            )
        )
    }

    private fun choose(choice: Int) {
        if (answered) return
        answered = true
        val c = current ?: return
        val ok = choice == c.answer
        done += 1

        val rightColor = ContextCompat.getColor(requireContext(), R.color.optionRight)
        val wrongColor = ContextCompat.getColor(requireContext(), R.color.optionWrong)
        optionButtons[c.answer].backgroundTintList = ColorStateList.valueOf(rightColor)
        if (!ok) optionButtons[choice].backgroundTintList = ColorStateList.valueOf(wrongColor)
        optionButtons.forEach { it.isEnabled = false }

        val verdict = if (ok) {
            right += 1
            val left = ProgressStore.markCorrect(requireContext(), c.entry.type, c.entry.index)
            if (left > 0) "✓ 答对！还需再答对 $left 次即可移出错题库。" else "✓ 答对！已掌握，移出错题库。"
        } else {
            ProgressStore.markWrong(requireContext(), c.entry.type, c.entry.index)
            val peak = ProgressStore.wrongPeak(requireContext(), c.entry.type, c.entry.index)
            "✗ 又错了，累计 $peak 次" +
                if (peak > ProgressStore.KEY_POINT_THRESHOLD) "，已标记为重点，请重点突破。" else "，继续加油。"
        }

        tvResult.text = verdict
        tvResult.setTextColor(
            ContextCompat.getColor(requireContext(), if (ok) R.color.optionRight else R.color.optionWrong)
        )
        tvAnalysis.text = c.analysis
        btnRef.text = "查看第${c.ref}条"
        btnRef.setOnClickListener {
            startActivity(LawDetailActivity.newIntent(requireContext(), c.ref))
        }
        resultCard.visibility = View.VISIBLE
        btnNext.isEnabled = true
    }

    private fun finishRound() {
        val remain = done - right
        tvProg.text = "本轮完成"
        tvQuestion.text = "本轮共 $done 道，答对 $right 道。" +
            if (remain > 0) "仍有 $remain 道答错，已留在错题库，稍后再来巩固。" else "全部答对，继续加油！"
        tvScene.visibility = View.GONE
        badgeBox.removeAllViews()
        optionButtons.forEach { it.visibility = View.GONE }
        resultCard.visibility = View.GONE
        btnNext.isEnabled = true
        btnNext.text = "再来一轮"
        btnNext.setOnClickListener { startPractice(entries()) }
    }
}
