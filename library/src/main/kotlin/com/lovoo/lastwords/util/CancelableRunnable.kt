package com.lovoo.lastwords.util

/**
 * This class implements a [java.lang.Runnable]s which can be canceled before they it is executed.
 * This is especially interesting for delayed execution.
 */
abstract class CancelableRunnable : Runnable {

    var canceled = false
        private set

    var wasExecuted = false
        private set

    override fun run() {
        if (!canceled) {
            runCancelable()
            wasExecuted = true
        }
    }

    abstract fun runCancelable()

    fun cancel() {
        canceled = true
    }
}