package com.codexpong.mobile

import android.app.Application
import com.codexpong.mobile.core.config.AppContainer

/**
 * 앱 전역 의존성을 준비하는 Application 클래스. (한국어 주석 필수)
 */
class CodexApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
