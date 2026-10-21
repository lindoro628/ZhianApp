package com.yuanbao.zhian.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.yuanbao.zhian.LawDetailActivity
import com.yuanbao.zhian.R
import com.yuanbao.zhian.data.CaseItem
import com.yuanbao.zhian.data.ProgressStore
import com.yuanbao.zhian.data.Repo

class CaseFragment : Fragment(R.layout.fragment_case) {

    private var cases: List<CaseItem> = emptyList()
    private var cursor = 0
    private var answered = false
    private var rightCount = 0
    private var doneCount = 0

    private lateinit var tvProgress: TextView
    private lateinit var tvScene: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var tvResult: TextView
    private lateinit var tvAnalysis: TextView
    private lateinit var btnRef: Button
    private lateinit var btnNext: MaterialButton
    private lateinit var btnPrev: MaterialButton
    private val optionButtons = ArrayList<MaterialButton>(4)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvProgress = view.findViewById(R.id.tvProgress)
        tvScene = view.findViewById(R.id.tvScene)
        tvQuestion = view.findViewById(R.id.tvQuestion)
        resultCard = view.findViewById(R.id.resultCard)
        tvResult = view.findViewById(R.id.tvResult)
        tvAnalysis = view.findViewById(R.id.tvAnalysis)
        btnRef = view.findViewById(R.id.btnRef)
        btnNext = view.findViewById(R.id.btnNext)
        btnPrev = view.findViewById(R.id.btnPrev)
        optionButtons.clear()
        for (i in 0..3) {
            val id = resources.getIdentifier("btnOpt$i", "id", requireContext().packageName)
            optionButtons.add(view.findViewById(id))
        }

        cases = Repo.cases(requireContext())
        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { onChoose(index) }
        }
        btnNext.setOnClickListener { move(1) }
        btnPrev.setOnClickListener { move(-1) }
        showCase()
    }

    private fun move(delta: Int) {
        val target = cursor + delta
        if (target < 0 || target >= cases.size) return
        cursor = target
        showCase()
    }

    private fun showCase() {
        answered = false
        val item = cases[cursor]
        tvProgress.text = "情景 ${cursor + 1}/${cases.size}　已练 $doneCount 例，答对 $rightCount 例"
        tvScene.text = item.scene
        tvQuestion.text = item.question
        item.options.forEachIndexed { index, text ->
            optionButtons[index].text = "${'A' + index}. $text"
            optionButtons[index].backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.optionNormal)
            )
            optionButtons[index].isEnabled = true
        }
        resultCard.visibility = View.GONE
        btnPrev.isEnabled = cursor > 0
        btnNext.text = if (cursor == cases.size - 1) "已是最后一例" else "下一个情景"
    }

    private fun onChoose(choice: Int) {
        if (answered) return
        answered = true
        doneCount += 1
        val item = cases[cursor]
        val ok = choice == item.answer
        val verdict: String
        if (ok) {
            rightCount += 1
            val left = ProgressStore.markCorrect(requireContext(), 'c', item.index)
            verdict = if (left > 0) "✓ 判断正确（还需答对 $left 次移出错题库）" else "✓ 判断正确（已掌握，移出错题库）"
        } else {
            ProgressStore.markWrong(requireContext(), 'c', item.index)
            val peak = ProgressStore.wrongPeak(requireContext(), 'c', item.index)
            verdict = "✗ 判断错误，正确答案：${'A' + item.answer}　已收入错题库，累计错 $peak 次" +
                (if (peak > ProgressStore.KEY_POINT_THRESHOLD) " ★重点" else "")
        }

        val rightColor = ContextCompat.getColor(requireContext(), R.color.optionRight)
        val wrongColor = ContextCompat.getColor(requireContext(), R.color.optionWrong)
        optionButtons[item.answer].backgroundTintList = ColorStateList.valueOf(rightColor)
        if (!ok) optionButtons[choice].backgroundTintList = ColorStateList.valueOf(wrongColor)
        optionButtons.forEach { it.isEnabled = false }

        tvResult.text = verdict
        tvResult.setTextColor(
            ContextCompat.getColor(requireContext(), if (ok) R.color.optionRight else R.color.optionWrong)
        )
        tvAnalysis.text = item.analysis
        btnRef.text = "查看第${item.ref}条"
        btnRef.setOnClickListener {
            startActivity(LawDetailActivity.newIntent(requireContext(), item.ref))
        }
        resultCard.visibility = View.VISIBLE
        tvProgress.text = "情景 ${cursor + 1}/${cases.size}　已练 $doneCount 例，答对 $rightCount 例"
    }
}
