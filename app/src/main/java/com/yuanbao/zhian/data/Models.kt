package com.yuanbao.zhian.data

data class LawArticle(
    val id: Int,
    val chapter: String,
    val section: String,
    val text: String
) {
    val title: String
        get() = "第${id}条"

    val sectionOrChapter: String
        get() = if (section.isBlank()) chapter else "$chapter · $section"

    val digest: String
        get() {
            val flat = text.replace("\n", " ")
            return if (flat.length > 70) flat.substring(0, 70) + "…" else flat
        }
}

data class QuizQuestion(
    val index: Int,
    val q: String,
    val options: List<String>,
    val answer: Int,
    val analysis: String,
    val ref: Int
)

data class CaseItem(
    val index: Int,
    val scene: String,
    val question: String,
    val options: List<String>,
    val answer: Int,
    val analysis: String,
    val ref: Int
)
