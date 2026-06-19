package com.cleaner.filemanager.model

/**
 * Representasi satu kartu kategori di dashboard (Hidden, Cache, Large, Image, dst).
 */
data class CategoryCard(
    val type: CategoryType,
    val title: String,
    val size: Long,
    val count: Int,
    val iconRes: Int
)

enum class CategoryType {
    HIDDEN,
    CACHE,
    LARGE,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT,
    APK,
    OTHER
}
