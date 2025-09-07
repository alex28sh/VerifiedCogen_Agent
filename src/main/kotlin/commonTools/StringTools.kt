package org.example.commonTools

fun String.insertAt(index: Int, insert: String): String {
    require(index in 0..length) { "Index $index out of bounds for length $length" }
    return substring(0, index) + insert + substring(index)
}