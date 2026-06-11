package com.kuroanime

import android.app.Activity
import java.lang.ref.WeakReference

object WebViewHost {
    private var activityRef: WeakReference<Activity>? = null

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun getActivity(): Activity? {
        return activityRef?.get()
    }

    fun clear() {
        activityRef?.clear()
        activityRef = null
    }
}
