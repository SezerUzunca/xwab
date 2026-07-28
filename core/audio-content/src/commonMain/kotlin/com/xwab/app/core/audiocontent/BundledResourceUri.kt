package com.xwab.app.core.audiocontent

import com.xwab.app.core.audiocontent.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
internal fun bundledResourceUri(path: String): String = Res.getUri(path)
