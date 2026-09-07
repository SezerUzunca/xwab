package com.xwab.app.core.sound.port

public data class Category(
    public val id: CategoryId,
    public val name: String,
    public val description: String,
    public val symbol: String,
    public val musicCount: Int,
)
