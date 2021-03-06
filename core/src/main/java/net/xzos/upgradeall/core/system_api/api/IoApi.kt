package net.xzos.upgradeall.core.system_api.api

import net.xzos.upgradeall.core.data.json.gson.DownloadInfoItem
import net.xzos.upgradeall.core.data.json.gson.PackageIdGson
import net.xzos.upgradeall.core.server_manager.module.applications.AppInfo
import net.xzos.upgradeall.core.system_api.interfaces.IoApi


// 平台相关 IO 互操作
object IoApi {

    private var ioApiInterface: IoApi? = null
    private var appInfoListTmpMap: MutableMap<String, List<AppInfo>> = mutableMapOf()

    fun setInterfaces(interfacesClass: IoApi) {
        ioApiInterface = interfacesClass
    }

    // 注释相应平台的下载软件
    internal suspend fun downloadFile(
            taskName: String,
            downloadInfoList: List<DownloadInfoItem>,
            externalDownloader: Boolean
    ) {
        if (downloadInfoList.isEmpty()) return
        ioApiInterface?.downloadFile(taskName, downloadInfoList, externalDownloader)
    }

    // 查询软件信息
    internal fun getAppVersionNumber(targetChecker: PackageIdGson?): String? {
        return ioApiInterface?.getAppVersionNumber(targetChecker)
    }

    // 获取软件信息列表
    fun getAppInfoList(type: String): List<AppInfo>? {
        return appInfoListTmpMap[type] ?: ioApiInterface?.getAppInfoList(type)?.also {
            appInfoListTmpMap[type] = it
        }
    }
}
