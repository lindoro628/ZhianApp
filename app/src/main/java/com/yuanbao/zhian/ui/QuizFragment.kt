package com.yuanbao.zhian.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuanbao.zhian.LawDetailActivity
import com.yuanbao.zhian.R
import com.yuanbao.zhian.data.ProgressStore
import com.yuanbao.zhian.data.QuizQuestion
import com.yuanbao.zhian.data.Repo
import kotlin.math.ceil

class QuizFragment : Fragment(R.layout.fragment_quiz) {

    private val levelSize = 10

    private var all: List<QuizQuestion> = emptyList()
    private var session: List<QuizQuestion> = emptyList()
    private var level = 1
    private var wrongMode = false
    private var cursor = 0
    private var correctCount = 0
    private var answered = false
    private var suppressSpinner = false

    private lateinit var spinLevel: Spinner
    private lateinit var cbWrong: CheckBox
    private lateinit var tvProgress: TextView
    private lateinit var tvQuestion: TextView
    private lateinit var resultCard: LinearLayout
    private lateinit var tvResult: TextView
    private lateinit var tvAnalysis: TextView
    private lateinit var btnRef: Button
    private lateinit var btnNext: MaterialButton
    private val optionButtons = ArrayList<MaterialButton>(4)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinLevel = view.findViewById(R.id.spinLevel)
        cbWrong = view.findViewById(R.id.cbWrong)
        tvProgress = view.findViewById(R.id.tvProgress)
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

        all = Repo.quiz(requireContext())
        setupLevelSpinner()

        cbWrong.setOnCheckedChangeListener { _, isChecked ->
            wrongMode = isChecked
            spinLevel.isEnabled = !isChecked
            startSession()
        }

        btnNext.setOnClickListener {
            if (!answered) return@setOnClickListener
            cursor += 1
            if (cursor >= session.size) finishSession() else showQuestion()
        }

        optionButtons.forEachIndexed { index, button ->
            button.setOnClickListener { onChoose(index) }
        }

        startSession()
    }

    private fun setupLevelSpinner() {
        val total = all.size
        val levels = ceil(total.toDouble() / levelSize).toInt()
        val unlocked = ProgressStore.unlockedLevel(requireContext())
        val labels = ArrayList<String>(levels)
        for (i in 1..levels) {
            val from = (i - 1) * levelSize + 1
            val to = minOf(i * levelSize, total)
            labels.add(if (i <= unlocked) "第${i}关（${from}-${to}题）" else "第${i}关 🔒 未解锁")
        }
        spinLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            labels
        )
        spinLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                if (suppressSpinner) {
                    suppressSpinner = false
                    return
                }
                if (pos + 1 > ProgressStore.unlockedLevel(requireContext())) {
                    suppressSpinner = true
                    spinLevel.setSelection(level - 1)
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("该关卡尚未解锁")
                        .setMessage("通关上一关且正确率达到 60% 以上，即可解锁下一关。")
                        .setPositiveButton("知道了", null)
                        .show()
                    return
                }
                level = pos + 1
                startSession()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
        level = minOf(ProgressStore.unlockedLevel(requireContext()), levels)
        suppressSpinner = true
        spinLevel.setSelection(level - 1)
    }

    private fun startSession() {
        session = if (wrongMode) {
            val wrong = ProgressStore.wrongIndexes(requireContext(), 'q')
            all.filter { wrong.contains(it.index) }
        } else {
            val from = (level - 1) * levelSize
            all.subList(from, minOf(from + levelSize, all.size))
        }
        cursor = 0
        correctCount = 0

        if (session.isEmpty()) {
            tvProgress.text = "暂无题目"
            tvQuestion.text = if (wrongMode) {
                "错题库里没有练习题，先去做几关练习吧。"
            } else {
                "本关暂无题目。"
            }
            optionButtons.forEach { it.visibility = View.GONE }
            resultCard.visibility = View.GONE
            btnNext.visibility = View.GONE
            return
        }
        optionButtons.forEach { it.visibility = View.VISIBLE }
        btnNext.visibility = View.VISIBLE
        showQuestion()
    }

    private fun showQuestion() {
        answered = false
        val q = session[cursor]
        tvProgress.text = if (wrongMode) {
            "错题重练 ${cursor + 1}/${session.size}　已答对 $correctCount"
        } else {
            "第${level}关　${cursor + 1}/${session.size}　已答对 $correctCount"
        }
        tvQuestion.text = q.q
        q.options.forEachIndexed { index, text ->
            optionButtons[index].text = "${'A' + index}. $text"
            optionButtons[index].backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.optionNormal)
            )
            optionButtons[index].isEnabled = true
        }
        resultCard.visibility = View.GONE
        btnNext.text = if (cursor == session.size - 1) "完成本关" else "下一题"
    }

    private fun onChoose(choice: Int) {
        if (answered) return
        answered = true
        val q = session[cursor]
        val ok = choice == q.answer
        val verdict: String
        if (ok) {
            correctCount += 1
            val left = ProgressStore.markCorrect(requireContext(), 'q', q.index)
            verdict = if (left > 0) "✓ 回答正确（还需答对 $left 次移出错题库）" else "✓ 回答正确（已掌握，移出错题库）"
        } else {
            ProgressStore.markWrong(requireContext(), 'q', q.index)
            val peak = ProgressStore.wrongPeak(requireContext(), 'q', q.index)
            verdict = "✗ 回答错误，正确答案：${'A' + q.answer}　已收入错题库，累计错 $peak 次" +
                (if (peak > ProgressStore.KEY_POINT_THRESHOLD) " ★重点" else "")
        }

        val rightColor = ContextCompat.getColor(requireContext(), R.color.optionRight)
        val wrongColor = ContextCompat.getColor(requireContext(), R.color.optionWrong)
        optionButtons[q.answer].backgroundTintList = ColorStateList.valueOf(rightColor)
        if (!ok) optionButtons[choice].backgroundTintList = ColorStateList.valueOf(wrongColor)
        optionButtons.forEach { it.isEnabled = false }

        tvResult.text = verdict
        tvResult.setTextColor(
            ContextCompat.getColor(requireContext(), if (ok) R.color.optionRight else R.color.optionWrong)
        )
        tvAnalysis.text = q.analysis
        btnRef.text = "查看第${q.ref}条"
        btnRef.setOnClickListener {
            startActivity(LawDetailActivity.newIntent(requireContext(), q.ref))
        }
        resultCard.visibility = View.VISIBLE
    }

    private fun finishSession() {
        if (wrongMode) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("错题重练完成")
                .setMessage("本轮共 ${session.size} 题，答对 $correctCount 题。")
                .setPositiveButton("再来一轮") { _, _ -> startSession() }
                .show()
            return
        }
        val total = session.size
        val pass = correctCount * 100 >= total * 60
        ProgressStore.saveBest(requireContext(), level, correctCount)
        if (pass) ProgressStore.unlockLevel(requireContext(), level + 1)

        val message = buildString {
            append("本关共 $total 题，答对 $correctCount 题，正确率 ")
            append(correctCount * 100 / total).append("%。\n\n")
            append(if (pass) "已解锁下一关！" else "正确率需达到 60% 才能解锁下一关，再来一次吧。")
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("第${level}关完成")
            .setMessage(message)
            .setPositiveButton("再练一次") { _, _ -> startSession() }
            .setNegativeButton("关闭", null)
            .show()
    }
}
