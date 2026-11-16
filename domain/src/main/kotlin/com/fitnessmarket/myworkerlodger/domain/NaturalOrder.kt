package com.fitnessmarket.myworkerlodger.domain

/**
 * Orders names the way people read them: runs of digits compare by value ("B-2" before "B-10")
 * and letters compare ignoring case.
 */
internal object NaturalOrder : Comparator<String> {
    private val chunk = Regex("\\d+|\\D+")

    override fun compare(a: String, b: String): Int {
        val left = chunk.findAll(a).map { it.value }.toList()
        val right = chunk.findAll(b).map { it.value }.toList()
        for (i in 0 until minOf(left.size, right.size)) {
            val result = compareChunks(left[i], right[i])
            if (result != 0) return result
        }
        return left.size.compareTo(right.size)
    }

    private fun compareChunks(a: String, b: String): Int {
        if (a[0].isDigit() && b[0].isDigit()) {
            val x = a.trimStart('0')
            val y = b.trimStart('0')
            return if (x.length != y.length) x.length.compareTo(y.length) else x.compareTo(y)
        }
        return a.compareTo(b, ignoreCase = true)
    }
}
