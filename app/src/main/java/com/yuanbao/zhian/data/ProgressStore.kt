package com.yuanbao.zhian.data

import android.content.Context

object ProgressStore {

    private const val PREF = "zhian_progress"
    private const val KEY_FAV = "favorites"
    private const val KEY_UNLOCKED = "unlocked_level"
    private const val KEY_BEST = "best_"

    /** 错题前缀：'q' 练习题，'c' 情景案例；后缀为题号 */
    private const val P_WC = "wc_"   // 当前待清偿错误次数，答对一次减 1，归零移出错题库
    private const val P_WP = "wp_"   // 历史最高错误次数，>3 标记为重点

    /** 累计错误超过该次数即标记为重点 */
    const val KEY_POINT_THRESHOLD = 3

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun intSet(c: Context, key: String): MutableSet<Int> =
        prefs(c).getStringSet(key, HashSet())!!.mapNotNull { it.toIntOrNull() }.toMutableSet()

    private fun saveIntSet(c: Context, key: String, set: Set<Int>) {
        prefs(c).edit().putStringSet(key, HashSet(set.map { it.toString() })).apply()
    }

    /** 错题库中的一条记录 */
    data class WrongEntry(
        val type: Char,        // 'q' 或 'c'
        val index: Int,        // 在题库 / 案例库中的下标
        val count: Int,        // 当前待清偿次数
        val peak: Int          // 历史最高错误次数
    ) {
        val key: String get() = keyOf(type, index)
        val isKeyPoint: Boolean get() = peak > KEY_POINT_THRESHOLD
    }

    fun keyOf(type: Char, index: Int): String = "$type$index"

    /** 答错一次：待清偿次数 +1，并刷新历史峰值 */
    fun markWrong(c: Context, type: Char, index: Int) {
        val p = prefs(c)
        val k = keyOf(type, index)
        val now = p.getInt(P_WC + k, 0) + 1
        val peak = maxOf(p.getInt(P_WP + k, 0), now)
        p.edit().putInt(P_WC + k, now).putInt(P_WP + k, peak).apply()
    }

    /** 答对一次：待清偿次数 -1，归零则移出错题库 */
    fun markCorrect(c: Context, type: Char, index: Int): Int {
        val p = prefs(c)
        val k = keyOf(type, index)
        val cur = p.getInt(P_WC + k, 0)
        if (cur <= 0) return 0
        val now = cur - 1
        val editor = p.edit()
        if (now <= 0) {
            editor.remove(P_WC + k).remove(P_WP + k)
        } else {
            editor.putInt(P_WC + k, now)
        }
        editor.apply()
        return now
    }

    /** 当前未清偿次数 */
    fun wrongCount(c: Context, type: Char, index: Int): Int =
        prefs(c).getInt(P_WC + keyOf(type, index), 0)

    /** 历史最高错误次数 */
    fun wrongPeak(c: Context, type: Char, index: Int): Int =
        prefs(c).getInt(P_WP + keyOf(type, index), 0)

    /** 全部错题，重点在前、错误多的在前 */
    fun wrongEntries(c: Context): List<WrongEntry> {
        val p = prefs(c)
        return p.all.keys
            .filter { it.startsWith(P_WC) }
            .mapNotNull { k ->
                val raw = k.removePrefix(P_WC)
                val type = raw.firstOrNull()
                val index = raw.drop(1).toIntOrNull()
                if (type == null || index == null) return@mapNotNull null
                if (type != 'q' && type != 'c') return@mapNotNull null
                val count = p.getInt(k, 0)
                if (count <= 0) return@mapNotNull null
                WrongEntry(type, index, count, p.getInt(P_WP + raw, count))
            }
            .sortedWith(compareByDescending<WrongEntry> { it.peak }.thenByDescending { it.count })
    }

    /** 指定类型（'q'/'c'）的错题下标集合 */
    fun wrongIndexes(c: Context, type: Char): Set<Int> =
        wrongEntries(c).filter { it.type == type }.map { it.index }.toSet()

    fun wrongCount(c: Context): Int = wrongEntries(c).size

    fun keyPointCount(c: Context): Int = wrongEntries(c).count { it.isKeyPoint }

    /* ---------- 收藏、关卡、成绩 ---------- */

    fun isFavorite(c: Context, id: Int): Boolean = intSet(c, KEY_FAV).contains(id)

    fun toggleFavorite(c: Context, id: Int): Boolean {
        val set = intSet(c, KEY_FAV)
        val now = if (set.contains(id)) {
            set.remove(id)
            false
        } else {
            set.add(id)
            true
        }
        saveIntSet(c, KEY_FAV, set)
        return now
    }

    fun favorites(c: Context): Set<Int> = intSet(c, KEY_FAV)

    fun unlockedLevel(c: Context): Int = prefs(c).getInt(KEY_UNLOCKED, 1)

    fun unlockLevel(c: Context, level: Int) {
        if (level > unlockedLevel(c)) prefs(c).edit().putInt(KEY_UNLOCKED, level).apply()
    }

    fun bestScore(c: Context, level: Int): Int = prefs(c).getInt(KEY_BEST + level, -1)

    fun saveBest(c: Context, level: Int, correct: Int) {
        if (correct > bestScore(c, level)) prefs(c).edit().putInt(KEY_BEST + level, correct).apply()
    }
}
