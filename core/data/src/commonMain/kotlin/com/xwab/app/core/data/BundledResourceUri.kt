package com.xwab.app.core.data

import com.xwab.app.core.data.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal fun bundledResourceUri(path: String): String = Res.getUri(path)
