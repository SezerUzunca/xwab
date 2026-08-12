package com.xwab.app.core.catalog

data class Category(
    val id: CategoryId,
    val name: String,
    val description: String,
    val symbol: String,
    val musicCount: Int,
)
