package com.example.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.Observer
import com.example.focusguard.data.AppDatabase
import com.example.focusguard.data.FocusRepository
import com.example.focusguard.data.entity.AppRule
import com.example.focusguard.data.entity.User
import com.example.focusguard.data.entity.Violation
import com.example.focusguard.data.entity.ViolationType
import com.example.focusguard.ui.LockOverlayActivity
import com.example.focusguard.util.NotificationHelper
import com.example.focusguard.util.UsageManager
import kotlinx.coroutines.*
import java.util.Calendar

class FocusAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var repository: FocusRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var usageManager: UsageManager

    @Volatile
    private var currentPackage: String? = null
    private var scrollStartTime: Long = 0
    private var lastScrollTime: Long = 0
    private var isScrolling = false
    private val scrollTimeout = 2000L

    private var rulesCache: Map<String, AppRule> = emptyMap()
    private var currentUser: User? = null
    private var monitorJob: Job? = null

    private val rulesObserver = Observer<List<AppRule>> { rules ->
        rulesCache = rules.associateBy { it.packageName }
    }

    private val userObserver = Observer<User> { user ->
        currentUser = user
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = FocusRepository(AppDatabase.getDatabase(applicationContext))
        notificationHelper = NotificationHelper(applicationContext)
        usageManager = UsageManager(applicationContext)

        serviceScope.launch(Dispatchers.Main) {
            repository.allRules.observeForever(rulesObserver)
            repository.user.observeForever(userObserver)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::repository.isInitialized) {
            serviceScope.launch(Dispatchers.Main) {
                repository.allRules.removeObserver(rulesObserver)
                repository.user.removeObserver(userObserver)
            }
        }
        serviceScope.cancel()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val pkgName = event.packageName?.toString()
                if (pkgName != null && pkgName != currentPackage) {
                    currentPackage = pkgName
                    serviceScope.launch(Dispatchers.IO) {
                        checkAppRules(pkgName)
                    }
                    startMonitoring(pkgName)
                    resetScrollTracking()
                }
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                val pkgName = event.packageName?.toString() ?: currentPackage
                if (pkgName != null) {
                    handleScrollEvent(pkgName)
                }
            }
        }
    }

    private fun startMonitoring(packageName: String) {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive && currentPackage == packageName) {
                checkAppRules(packageName)
                delay(60000) // Check every minute
            }
        }
    }

    private suspend fun checkAppRules(packageName: String) {
        // Study Mode Check
        val user = currentUser
        if (user != null && user.studyStartHour != -1 && user.studyEndHour != -1) {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            val inSchedule = if (user.studyStartHour < user.studyEndHour) {
                hour >= user.studyStartHour && hour < user.studyEndHour
            } else {
                hour >= user.studyStartHour || hour < user.studyEndHour
            }

            if (inSchedule) {
                val rule = rulesCache[packageName]
                // If no rule exists, it's not whitelisted by default.
                if (rule == null || !rule.isWhitelisted) {
                    withContext(Dispatchers.Main) {
                        blockApp(packageName)
                        notificationHelper.showWarning("Study Mode Active", "Only whitelisted apps allowed.")
                    }
                    return
                }
            }
        }

        val rule = rulesCache[packageName]
        if (rule != null) {
            if (rule.isBlocked) {
                withContext(Dispatchers.Main) {
                    blockApp(packageName)
                }
                return
            }

            if (rule.dailyLimitMinutes > 0) {
                val usage = usageManager.getTodayUsage(packageName)
                val limitMillis = rule.dailyLimitMinutes * 60 * 1000L
                if (usage > limitMillis) {
                    withContext(Dispatchers.Main) {
                        blockApp(packageName)
                        notificationHelper.showWarning("Time Limit Reached", "${rule.appName} is locked for the day.")
                    }
                }
            }
        }
    }

    private fun handleScrollEvent(packageName: String) {
        val rule = rulesCache[packageName] ?: return
        if (!rule.isDoomscrollMonitored) return

        val currentTime = System.currentTimeMillis()

        if (!isScrolling) {
            isScrolling = true
            scrollStartTime = currentTime
        }

        lastScrollTime = currentTime

        val duration = (currentTime - scrollStartTime) / 1000
        if (duration > rule.doomscrollLimitSeconds) {
            // Threshold exceeded
            if (duration % 60 == 0L) {
                 triggerDoomscrollWarning(packageName, duration)
            }
        }
    }

    private fun resetScrollTracking() {
        isScrolling = false
        scrollStartTime = 0
        lastScrollTime = 0
    }

    private fun triggerDoomscrollWarning(packageName: String, duration: Long) {
        notificationHelper.showWarning(
            "Doomscrolling Detected!",
            "You've been scrolling for ${duration / 60} minutes. Penalty applied!"
        )

        serviceScope.launch(Dispatchers.IO) {
            val user = repository.getUserSync() ?: return@launch
            val penalty = user.violationPenalty

            val violation = Violation(
                userId = user.id,
                timestamp = System.currentTimeMillis(),
                type = ViolationType.DOOMSCROLL,
                packageName = packageName,
                details = "Doomscrolling ${duration}s",
                penaltyApplied = penalty
            )
            repository.addViolation(violation)
        }
    }

    private fun blockApp(packageName: String) {
        val intent = Intent(this, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("PACKAGE_NAME", packageName)
        }
        startActivity(intent)
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    override fun onInterrupt() {
        // Service interrupted
    }
}
