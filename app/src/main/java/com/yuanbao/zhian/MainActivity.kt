package com.yuanbao.zhian

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yuanbao.zhian.ui.CaseFragment
import com.yuanbao.zhian.ui.LawFragment
import com.yuanbao.zhian.ui.QuizFragment
import com.yuanbao.zhian.ui.WrongFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pager = findViewById<ViewPager2>(R.id.pager)
        val bottom = findViewById<BottomNavigationView>(R.id.bottomNav)

        pager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> LawFragment()
                1 -> QuizFragment()
                2 -> CaseFragment()
                else -> WrongFragment()
            }
        }
        pager.isUserInputEnabled = false
        pager.offscreenPageLimit = 3

        bottom.setOnItemSelectedListener { item ->
            pager.currentItem = when (item.itemId) {
                R.id.nav_law -> 0
                R.id.nav_quiz -> 1
                R.id.nav_case -> 2
                else -> 3
            }
            true
        }
    }
}
