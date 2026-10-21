package com.yuanbao.zhian.data

import android.content.Context
import org.json.JSONArray

object Repo {

    private var lawsCache: List<LawArticle>? = null
    private var quizCache: List<QuizQuestion>? = null
    private var caseCache: List<CaseItem>? = null

    private fun readAsset(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }

    fun laws(context: Context): List<LawArticle> {
        lawsCache?.let { return it }
        val arr = JSONArray(readAsset(context, "law.json"))
        val list = ArrayList<LawArticle>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                LawArticle(
                    id = o.getInt("id"),
                    chapter = o.optString("chapter", ""),
                    section = o.optString("section", ""),
                    text = o.optString("text", "")
                )
            )
        }
        list.sortBy { it.id }
        lawsCache = list
        return list
    }

    fun quiz(context: Context): List<QuizQuestion> {
        quizCache?.let { return it }
        val arr = JSONArray(readAsset(context, "quiz.json"))
        val list = ArrayList<QuizQuestion>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options")
            val optList = ArrayList<String>(opts.length())
            for (j in 0 until opts.length()) optList.add(opts.getString(j))
            list.add(
                QuizQuestion(
                    index = i,
                    q = o.getString("q"),
                    options = optList,
                    answer = o.getInt("answer"),
                    analysis = o.getString("analysis"),
                    ref = o.getInt("ref")
                )
            )
        }
        quizCache = list
        return list
    }

    fun cases(context: Context): List<CaseItem> {
        caseCache?.let { return it }
        val arr = JSONArray(readAsset(context, "cases.json"))
        val list = ArrayList<CaseItem>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val opts = o.getJSONArray("options")
            val optList = ArrayList<String>(opts.length())
            for (j in 0 until opts.length()) optList.add(opts.getString(j))
            list.add(
                CaseItem(
                    index = i,
                    scene = o.getString("scene"),
                    question = o.getString("question"),
                    options = optList,
                    answer = o.getInt("answer"),
                    analysis = o.getString("analysis"),
                    ref = o.getInt("ref")
                )
            )
        }
        caseCache = list
        return list
    }
}
