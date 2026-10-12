package com.xwab.app.core.favorites

import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.TestConfig
import co.touchlab.kermit.TestLogWriter

/** A logger that writes nowhere, for the tests that do not read what was logged. */
internal val SilentLogger = Logger(StaticConfig(logWriterList = emptyList()))

/** A logger over [writer], for the tests that read what was logged. */
@OptIn(ExperimentalKermitApi::class)
internal fun recordingLogger(writer: TestLogWriter): Logger =
    Logger(TestConfig(minSeverity = Severity.Verbose, logWriterList = listOf(writer)))
