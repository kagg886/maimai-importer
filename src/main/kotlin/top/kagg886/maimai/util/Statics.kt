package top.kagg886.maimai.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Transient
import kotlin.time.Duration.Companion.hours

object Statics {
    @Transient
    private val scope = CoroutineScope(Dispatchers.Default)

    private val delay = 1.hours


    var connection = 0
    var scanSuccess = 0
    var importSuccess = 0

    fun staticConnection() {
        connection += 1
        scope.launch {
            delay(delay)
            connection -= 1
        }
    }

    fun staticScanSuccess() {
        scanSuccess += 1
        scope.launch {
            delay(delay)
            scanSuccess -= 1
        }
    }

    fun staticImportSuccess() {
        importSuccess += 1
        scope.launch {
            delay(delay)
            importSuccess -= 1
        }
    }
}