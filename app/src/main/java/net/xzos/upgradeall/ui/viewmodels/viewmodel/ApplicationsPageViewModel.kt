package net.xzos.upgradeall.ui.viewmodels.viewmodel

import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.oberver.Observer
import net.xzos.upgradeall.core.server_manager.module.applications.Applications
import net.xzos.upgradeall.utils.mutableLiveDataOf
import net.xzos.upgradeall.utils.setValueBackground

class ApplicationsPageViewModel : AppListContainerViewModel() {

    init {
        appListLiveData = mutableLiveDataOf()
    }

    lateinit var observe: Observer
    lateinit var applications: Applications

    internal fun setApplications(applications: Applications) {
        this.applications = applications
        setAppList(applications)
        observe = object : Observer {
            override fun onChanged(vars: Array<out Any>): Any? {
                return setAppList(applications)
            }
        }
        applications.observeForever(observe)
    }

    private fun setAppList(applications: Applications) {
        val needUpdateAppList = runBlocking { applications.getNeedUpdateAppList(false) }
        val appList = needUpdateAppList + applications.apps.filter {
            !needUpdateAppList.contains(it)
        }
        appListLiveData.setValueBackground(appList)
    }

    override fun onCleared() {
        super.onCleared()
        applications.removeObserver(observe)
    }
}
