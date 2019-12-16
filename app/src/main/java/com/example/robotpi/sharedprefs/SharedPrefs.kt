package com.example.robotpi.sharedprefs

import android.app.Activity
import android.content.Context
import com.example.robotpi.R

class SharedPrefs {

    companion object {
        fun saveLocalhost(activity: Activity, value: String) {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
            with(sharedPref.edit()) {
                putString(activity.getString(R.string.shared_prefs_local_host_key), value)
                commit()
            }
        }

        fun readLocalhost(activity: Activity): String? {
            val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return ""
            return sharedPref.getString(
                activity.getString(R.string.shared_prefs_local_host_key),
                activity.getString(R.string.local_host_default_value)
            )
        }
    }
}