package com.yuanbao.zhian

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yuanbao.zhian.data.LawArticle
import com.yuanbao.zhian.data.ProgressStore
import com.yuanbao.zhian.data.Repo

class LawDetailActivity : AppCompatActivity() {

    private var currentId: Int = 1
    private lateinit var favButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_law_detail)

        currentId = intent.getIntExtra(EXTRA_ID, 1)

        val titleView = findViewById<TextView>(R.id.tvTitle)
        val metaView = findViewById<TextView>(R.id.tvMeta)
        val bodyView = findViewById<TextView>(R.id.tvBody)
        favButton = findViewById(R.id.btnFav)

        val prev = findViewById<Button>(R.id.btnPrev)
        val next = findViewById<Button>(R.id.btnNext)

        fun render(article: LawArticle?) {
            // 返回列表时带回最后停留的条号，用于定位和高亮
            setResult(RESULT_OK, Intent().putExtra(EXTRA_ID, currentId))
            if (article == null) {
                titleView.text = "第${currentId}条"
                metaView.text = ""
                bodyView.text = "未找到该条文。"
                favButton.isEnabled = false
                return
            }
            titleView.text = "第${article.id}条"
            metaView.text = article.sectionOrChapter
            bodyView.text = article.text
            favButton.isEnabled = true
            favButton.text = if (ProgressStore.isFavorite(this, article.id)) "★ 已收藏" else "☆ 收藏"
        }

        val laws = Repo.laws(this)
        render(laws.firstOrNull { it.id == currentId })

        favButton.setOnClickListener {
            val nowFav = ProgressStore.toggleFavorite(this, currentId)
            favButton.text = if (nowFav) "★ 已收藏" else "☆ 收藏"
        }

        prev.setOnClickListener {
            if (currentId > 1) {
                currentId -= 1
                render(laws.firstOrNull { it.id == currentId })
            }
        }
        next.setOnClickListener {
            if (currentId < 144) {
                currentId += 1
                render(laws.firstOrNull { it.id == currentId })
            }
        }
    }

    companion object {
        const val EXTRA_ID = "extra_article_id"

        fun newIntent(context: Context, id: Int): Intent =
            Intent(context, LawDetailActivity::class.java).putExtra(EXTRA_ID, id)
    }
}
