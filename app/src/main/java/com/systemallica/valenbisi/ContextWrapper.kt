package com.systemallica.valenbisi

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build

import java.util.Locale

class ContextWrapper(base: Context) : android.content.ContextWrapper(base) {
    companion object {

        fun wrap(context: Context, language: String): android.content.ContextWrapper {
            var mContext = context
            val config = mContext.resources.configuration
            val sysLocale: Locale
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                sysLocale = getSystemLocale(config)
            } else {
                sysLocale = getSystemLocaleLegacy(config)
            }
            if (language != "" && sysLocale.language != language) {
                val locale = Locale(language)
                Locale.setDefault(locale)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    setSystemLocale(config, locale)
                } else {
                    setSystemLocaleLegacy(config, locale)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    mContext = mContext.createConfigurationContext(config)
                } else {
                    mContext.resources.updateConfiguration(config, mContext.resources.displayMetrics)
                }
            }
            return ContextWrapper(mContext)
        }

        fun getSystemLocaleLegacy(config: Configuration): Locale {
            return config.locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun getSystemLocale(config: Configuration): Locale {
            return config.locales.get(0)
        }

        fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
            config.locale = locale
        }

        @TargetApi(Build.VERSION_CODES.N)
        fun setSystemLocale(config: Configuration, locale: Locale) {
            config.setLocale(locale)
        }
    }
}