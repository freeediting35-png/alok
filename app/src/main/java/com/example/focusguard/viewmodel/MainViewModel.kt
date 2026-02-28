package com.example.focusguard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.focusguard.data.AppDatabase
import com.example.focusguard.data.FocusRepository
import com.example.focusguard.data.entity.AppRule
import com.example.focusguard.data.entity.User
import com.example.focusguard.data.entity.Violation
import com.example.focusguard.data.entity.WalletTransaction
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FocusRepository = FocusRepository(AppDatabase.getDatabase(application))

    val user: LiveData<User> = repository.user
    val rules: LiveData<List<AppRule>> = repository.allRules
    val transactions: LiveData<List<WalletTransaction>> = repository.transactions
    val violations: LiveData<List<Violation>> = repository.violations

    private val _installedApps = androidx.lifecycle.MutableLiveData<List<android.content.pm.ApplicationInfo>>()
    val installedApps: LiveData<List<android.content.pm.ApplicationInfo>> = _installedApps

    fun loadInstalledApps() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pm = getApplication<Application>().packageManager
            val apps = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
                .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 || (it.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0 } // Filter system apps broadly
            _installedApps.postValue(apps)
        }
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            repository.saveUser(user)
        }
    }

    fun saveRule(rule: AppRule) {
        viewModelScope.launch {
            repository.saveRule(rule)
        }
    }
}
