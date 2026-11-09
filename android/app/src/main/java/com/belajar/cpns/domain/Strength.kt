package com.belajar.cpns.domain

import com.belajar.cpns.data.db.TagStatEntity

enum class Strength(val label: String) { LEMAH("Lemah"), SEDANG("Sedang"), KUAT("Kuat"), BELUM("Belum ada data") }

/** Kekuatan per tag dari rasio skor yang didapat terhadap maksimal. Minimal 3 soal supaya tidak asal. */
fun TagStatEntity.strength(): Strength = when {
    attempts < 3 || maxSum == 0 -> Strength.BELUM
    scoreSum.toDouble() / maxSum < 0.6 -> Strength.LEMAH
    scoreSum.toDouble() / maxSum < 0.8 -> Strength.SEDANG
    else -> Strength.KUAT
}

fun TagStatEntity.ratio(): Double = if (maxSum == 0) 0.0 else scoreSum.toDouble() / maxSum

fun TagStatEntity.avgSeconds(): Int = if (attempts == 0) 0 else (timeSumMs / attempts / 1000).toInt()

/** Untuk TKP: rata-rata skor per soal (1 sampai 5). */
fun TagStatEntity.avgScore(): Double = if (attempts == 0) 0.0 else scoreSum.toDouble() / attempts

fun tagLabel(tag: String): String = tag.replace('_', ' ').replaceFirstChar { it.uppercase() }
