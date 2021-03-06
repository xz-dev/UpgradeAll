package net.xzos.upgradeall.utils.install

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.IPackageInstaller
import android.content.pm.IPackageInstallerSession
import android.content.pm.IPackageManager
import android.content.pm.PackageInstaller
import android.os.Process
import moe.shizuku.api.ShizukuApiConstants
import moe.shizuku.api.ShizukuBinderWrapper
import moe.shizuku.api.ShizukuService
import moe.shizuku.api.SystemServiceHelper
import net.xzos.upgradeall.R
import net.xzos.upgradeall.application.MyApplication
import net.xzos.upgradeall.core.data.json.nongson.ObjectTag
import net.xzos.upgradeall.core.log.Log
import net.xzos.upgradeall.utils.MiscellaneousUtils
import net.xzos.upgradeall.utils.install.shizuku.IIntentSenderAdaptor
import net.xzos.upgradeall.utils.install.shizuku.IntentSenderUtils
import net.xzos.upgradeall.utils.install.shizuku.PackageInstallerUtils
import java.io.File
import java.util.concurrent.CountDownLatch


object ApkShizukuInstaller {

    private const val TAG = "ApkShizukuInstaller"
    private val logObjectTag = ObjectTag(ObjectTag.core, TAG)

    private val context: Context = MyApplication.context

    @Suppress("ObjectPropertyName")
    private val _packageManager: IPackageManager by lazy {
        IPackageManager.Stub.asInterface(ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")))
    }

    @Suppress("ObjectPropertyName")
    private val _packageInstaller: IPackageInstaller by lazy {
        IPackageInstaller.Stub.asInterface(ShizukuBinderWrapper(_packageManager.packageInstaller.asBinder()))
    }

    suspend fun install(file: File) {
        doApkInstall(file)
    }

    suspend fun multipleInstall(apkFileList: List<File>) {
        doMultipleInstall(apkFileList)
    }

    private fun doApkInstall(file: File) {
        var session: PackageInstaller.Session? = null
        val res: StringBuilder = StringBuilder()

        try {
            session = getPackageInstallerSession()

            res.append('\n').append("write: ")
            doWriteSession(session, file)

            res.append('\n').append("commit: ")
            val result = doCommitSession(session)
            val status = result!!.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            res.append('\n').append("status: ").append(status).append(" (").append(message).append(")")
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr)
        } finally {
            try {
                session?.close()
            } catch (tr: Throwable) {
                res.append(tr)
            }
        }
        Log.i(logObjectTag, TAG, res.toString())
        MiscellaneousUtils.showToast(res.toString())
    }

    private fun doMultipleInstall(apkFileList: List<File>) {
        val res: StringBuilder = StringBuilder()
        var totalSize: Long = 0
        try {
            for (listOfFile in apkFileList) {
                if (listOfFile.isFile) {
                    res.append('\n').append("check file: " + listOfFile.name)
                    totalSize += listOfFile.length()
                }
            }
        } catch (e: Exception) {
            res.append('\n').append("error: " + e.message)
            return
        }

        val session = getPackageInstallerSession(totalSize)
        try {
            for (apkFile in apkFileList) {
                res.append('\n').append("write: ")
                doWriteSession(session, apkFile)
            }
            res.append('\n').append("commit: ")
            val result = doCommitSession(session)
            val status = result!!.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
            val message = result.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            res.append('\n').append("status: ").append(status).append(" (").append(message).append(")")
        } catch (tr: Throwable) {
            tr.printStackTrace()
            res.append(tr)
        } finally {
            try {
                session.close()
            } catch (tr: Throwable) {
                res.append(tr)
            }
        }
        Log.i(logObjectTag, TAG, res.toString())
        MiscellaneousUtils.showToast(res.toString())
    }

    private fun getPackageInstaller(): PackageInstaller {
        val isRoot = ShizukuService.getUid() == 0
        // the reason for use "com.android.shell" as installer package under adb is that getMySessions will check installer package's owner
        val installerPackageName = if (isRoot) context.packageName else "com.android.shell"
        val userId = if (isRoot) Process.myUserHandle().hashCode() else 0
        return PackageInstallerUtils.createPackageInstaller(_packageInstaller, installerPackageName, userId)
    }

    private fun getPackageInstallerSession(totalSize: Long? = null): PackageInstaller.Session {
        val res: StringBuilder = StringBuilder()
        res.append("createSession: ")

        val params: PackageInstaller.SessionParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        totalSize?.let {
            params.setSize(totalSize)
        }
        var installFlags = PackageInstallerUtils.getInstallFlags(params)
        installFlags = installFlags or (0x00000004 /*PackageManager.INSTALL_ALLOW_TEST*/ or 0x00000002) /*PackageManager.INSTALL_REPLACE_EXISTING*/
        PackageInstallerUtils.setInstallFlags(params, installFlags)

        val packageInstaller = getPackageInstaller()
        val sessionId = packageInstaller.createSession(params)
        res.append(sessionId)
        @Suppress("LocalVariableName")
        val _session = IPackageInstallerSession.Stub.asInterface(ShizukuBinderWrapper(_packageInstaller.openSession(sessionId).asBinder()))
        Log.i(logObjectTag, TAG, res.toString())
        return PackageInstallerUtils.createSession(_session)
    }

    private fun doWriteSession(session: PackageInstaller.Session, apkFile: File) {
        val inputStream = apkFile.inputStream()
        val outputStream = session.openWrite(apkFile.name, 0, -1)
        val buf = ByteArray(8192)
        var len: Int

        while (inputStream.read(buf).also { len = it } > 0) {
            outputStream.write(buf, 0, len)
            outputStream.flush()
            session.fsync(outputStream)
        }
        inputStream.close()
        outputStream.close()
    }

    private fun doCommitSession(session: PackageInstaller.Session): Intent? {
        val results = arrayOf<Intent?>(null)
        val countDownLatch = CountDownLatch(1)
        val intentSender: IntentSender = IntentSenderUtils.newInstance(object : IIntentSenderAdaptor() {
            override fun send(intent: Intent?) {
                results[0] = intent
                countDownLatch.countDown()
            }
        })
        session.commit(intentSender)

        countDownLatch.await()
        return results[0]
    }

    fun requestShizukuPermission(activity: Activity, PERMISSIONS_REQUEST_CONTACTS: Int): Boolean {
        return MiscellaneousUtils.requestPermission(
                activity, ShizukuApiConstants.PERMISSION,
                PERMISSIONS_REQUEST_CONTACTS, R.string.shizuku_permission_request)
    }
}
