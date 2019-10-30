package net.xzos.upgradeAll.server.app.engine.js

import net.xzos.upgradeAll.server.ServerContainer
import net.xzos.upgradeAll.server.app.engine.api.CoreApi
import net.xzos.upgradeAll.server.app.engine.js.utils.JSLog
import net.xzos.upgradeAll.server.app.engine.js.utils.JSUtils
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

internal class JavaScriptCoreEngine(
        private val logObjectTag: Array<String>,
        private val URL: String?,
        private val jsCode: String?
) : CoreApi {

    private val jsLog = JSLog(logObjectTag)
    internal val jsUtils = JSUtils(logObjectTag)
    private var scope: ScriptableObject? = null

    private fun initRhino(cx: Context, scope: Scriptable) {
        // 初始化 rhino 对象
        cx.optimizationLevel = -1
        // 载入 JavaScript 实例
        registerJavaMethods(scope)
        val methodsSuccess = executeVoidScript(cx, scope)
        if (methodsSuccess)
            ScriptableObject.putProperty(scope, "URL", URL)  // 初始化 URL
    }

    // 注册 Java 代码
    private fun registerJavaMethods(scope: Scriptable) {
        // 爬虫库
        val rhinoJSUtils = Context.javaToJS(jsUtils, scope)
        ScriptableObject.putProperty(scope, "JSUtils", rhinoJSUtils)
        // Log
        val rhinoLogUtils = Context.javaToJS(jsLog, scope)
        ScriptableObject.putProperty(scope, "Log", rhinoLogUtils)
    }

    // 加载 JavaScript 代码
    private fun executeVoidScript(cx: Context, scope: Scriptable): Boolean {
        if (jsCode == null) return false
        return try {
            cx.evaluateString(scope, jsCode, logObjectTag.toString(), 1, null)
            true
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, String.format("executeVoidScript: 脚本载入错误, ERROR_MESSAGE: %s", e.toString()))
            false
        }
    }

    // 运行 JS 代码
    private fun runJS(functionName: String, args: Array<Any>): Any? {
        val cx = Context.enter()
        val scope = scope ?: cx.initStandardObjects().also {
            initRhino(cx, it)
            scope = it
        }
        return try {
            (scope.get(functionName, scope) as Function).call(cx, scope, scope, args)
        } catch (e: Throwable) {
            Log.e(logObjectTag, TAG, "runJS: 脚本执行错误, 函数名: $functionName, ERROR_MESSAGE: $e")
            null
        } finally {
            Context.exit()
        }
    }

    override suspend fun getDefaultName(): String? {
        val result = runJS("getDefaultName", arrayOf()) ?: return null
        val defaultName = Context.toString(result)
        Log.d(logObjectTag, TAG, "getDefaultName: defaultName: $defaultName")
        return defaultName
    }

    override suspend fun getAppIconUrl(): String? {
        val result = runJS("getAppIconUrl", arrayOf())
                ?: return null
        val appIconUrl = Context.toString(result)
        Log.d(logObjectTag, TAG, "getAppIconUrl: appIconUrl: $appIconUrl")
        return appIconUrl
    }

    override suspend fun getReleaseNum(): Int {
        val result = runJS("getReleaseNum", arrayOf())
                ?: return 0
        val releaseNum = Context.toNumber(result).toInt()
        Log.d(logObjectTag, TAG, "getReleaseNum: releaseNum: $releaseNum")
        return releaseNum
    }

    override suspend fun getVersioning(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getVersioning", args)
                ?: runJS("getVersionNumber", args)
                ?: return null
        // TODO: 向下兼容两个主版本后移除，当前版本：0.1.0-alpha.3
        val versionNumber = Context.toString(result)
        Log.d(logObjectTag, TAG, "getVersioning: Versioning: $versionNumber")
        return versionNumber
    }

    override suspend fun getChangelog(releaseNum: Int): String? {
        val args = arrayOf<Any>(releaseNum)
        val result = runJS("getChangelog", args)
                ?: return null
        val changeLog = Context.toString(result)
        Log.d(logObjectTag, TAG, "getChangelog: Changelog: $changeLog")
        return changeLog
    }

    override suspend fun getReleaseDownload(releaseNum: Int): Map<String, String> {
        val args = arrayOf<Any>(releaseNum)
        val fileMap = mutableMapOf<String, String>()
        val result = runJS("getReleaseDownload", args)
                ?: return fileMap
        val fileJsonString = Context.toString(result)
        try {
            val returnMap = jsUtils.mapOfJsonObject(JSONObject(fileJsonString))
            for (key in returnMap.keys) {
                val keyString = key as String
                fileMap[keyString] = returnMap[key] as String
            }
        } catch (e: JSONException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值不符合 JsonObject 规范, fileJsonString : $fileJsonString")
        } catch (e: NullPointerException) {
            Log.e(logObjectTag, TAG, "getReleaseDownload: 返回值为 NULL, fileJsonString : $fileJsonString")
        }
        Log.d(logObjectTag, TAG, "getReleaseDownload: fileJson: $fileMap")
        return fileMap
    }

    override suspend fun downloadReleaseFile(downloadIndex: Pair<Int, Int>): String? {
        val result = runJS("downloadReleaseFile", arrayOf(downloadIndex.first, downloadIndex.second))
                ?: return null
        val filePath: String = Context.toString(result)
        Log.d(logObjectTag, TAG, "downloadReleaseFile: filePath: $filePath")
        return filePath
    }

    companion object {
        private const val TAG = "JavaScriptCoreEngine"
        private val Log = ServerContainer.Log
    }
}
