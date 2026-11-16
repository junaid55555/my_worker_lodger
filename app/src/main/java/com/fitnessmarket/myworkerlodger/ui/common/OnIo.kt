package com.fitnessmarket.myworkerlodger.ui.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs a domain call off the main thread, since the on-device store reads and writes the database. */
suspend fun <T> onIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }
