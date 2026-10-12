package com.xwab.app.core.playback.platform

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig

/** A logger that writes nowhere, for the tests that do not read what was logged. */
internal val SilentLogger = Logger(StaticConfig(logWriterList = emptyList()))
