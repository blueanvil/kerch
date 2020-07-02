package com.blueanvil.kerch

import org.testng.Assert.fail
import java.time.Duration

/**
 * @author Cosmin Marginean
 */
fun wait(errorMessage: String, condition: () -> Boolean) {
    val seconds = 30
    val sleepMs = 100L
    var success: Boolean
    var startTime = System.nanoTime()
    while (true) {
        success = condition()
        val elapsedSeconds = Duration.ofNanos(System.nanoTime() - startTime).seconds
        if (success || elapsedSeconds >= seconds) {
            break
        }
        Thread.sleep(sleepMs)
    }
    if (!success) {
        fail(errorMessage)
    }
}
