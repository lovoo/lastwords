package com.lovoo.lastwords

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.lovoo.lastwords.util.CancelableRunnable
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LastWords monitors the app's activities and notifies registered listeners when all activities are
 * finishing or have been destroyed.
 */
object LastWords : Application.ActivityLifecycleCallbacks {
    /**
     * The grace period in ms we give the app to transition between activities to avoid false alarms
     * in terms of determining the finish state of the app. Right now the value is set to be quite
     * conservative.
     */
    private val WAIT_FOR_NEXT_ACTIVITY_MS = 5000L

    /**
     * This list is NOT thread safe and should just be accessed from the main thread.
     */
    private val activityList = ConcurrentHashMap<Int, WeakReference<Activity>>()
    private val listeners = mutableSetOf<Listener>()
    private var finishCheckRunnable: CancelableRunnable? = null

    private val activityLock = Object()
    private val listenerLock = Object()

    private var delay = WAIT_FOR_NEXT_ACTIVITY_MS

    /**
     * @return whether there are no non-finishing, non-destroyed activities at the moment.
     */
    val isAppFinished = AtomicBoolean(true)

    interface Listener {
        /**
         * Called when the app has been finished, that is no non-finishing, non-destroyed activities
         * are alive.
         */
        fun onAppFinished()
    }

    /**
     * Registers LastWords as an activity lifecycle callback.
     *
     * Best to be called from `Application.onCreate()`.
     *
     * @param application our [Application]
     * @throws [IllegalStateException] if LastWords has already been initialised.
     */
    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    /**
     * Finish all activities and end the application process.
     * Do not trigger this when you have other tasks running, like write database or filesystem.
     *
     * @param processFinishDelay time in ms to wait until the process is killed when last activity finished, default 5000ms
     */
    fun finishApp(processFinishDelay: Long = WAIT_FOR_NEXT_ACTIVITY_MS) {
        register(object: Listener {
            override fun onAppFinished() {
                unregister(this)
                finishApp()
            }
        })

        var list: List<Activity> = emptyList()
        synchronized(activityLock, {
            list = activityList.map { it.value.get() }.filter { isActivityAlive(it) } as List<Activity>
        })

        if (list.isEmpty()) {
            isAppFinished.set(true)
            // no valid activity found so shutdown
            System.exit(0)
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }

        delay = processFinishDelay
        list.forEach { it.finish() }
    }

    /**
     * Registers a listener to be notified when the app has finished.
     *
     * @param listener a [Listener]
     */
    fun register(listener: Listener) {
        synchronized(listenerLock, { listeners.add(listener) })
    }

    /**
     * Unregisters a listener.
     *
     * @param listener a [Listener]
     */
    fun unregister(listener: Listener) {
        synchronized(listenerLock, { listeners.remove(listener) })
    }

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {
        isAppFinished.set(false)
        synchronized(activityLock, {
            activity?.let { activityList.put(activity.hashCode(), WeakReference(activity)) }
        })
    }

    override fun onActivityStarted(p0: Activity?) {
        // no-op
    }

    override fun onActivityResumed(p0: Activity?) {
        // no-op
    }

    override fun onActivityPaused(p0: Activity?) {
        handleFinishState()
    }

    override fun onActivityStopped(p0: Activity?) {
        handleFinishState()
    }

    override fun onActivityDestroyed(p0: Activity?) {
        handleFinishState()
    }

    override fun onActivitySaveInstanceState(p0: Activity?, p1: Bundle?) {
        // no-op
    }

    private fun handleFinishState() {
        if (needToMarkAsFinished()) {
            finishCheckRunnable?.cancel()
            finishCheckRunnable = object : CancelableRunnable() {
                override fun runCancelable() {
                    if (needToMarkAsFinished()) {
                        synchronized(activityLock, { activityList.clear() })
                        isAppFinished.set(true)
                        synchronized(listenerLock, { listeners.forEach { it.onAppFinished() } })
                    }
                }
            }.apply {
                Handler(Looper.getMainLooper()).postDelayed(this, delay)
            }
        }
    }

    private fun needToMarkAsFinished(): Boolean {
        synchronized(activityLock, {
            return !isAppFinished.get() && activityList.entries.apply {
                removeAll {
                    !isActivityAlive(it.value.get())
                }
            }.size == 0
        })
    }

    private fun isActivityAlive(activity: Activity?) = activity != null && !activity.isFinishing && !activity.isDestroyed

}