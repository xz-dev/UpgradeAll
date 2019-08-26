package net.xzos.UpgradeAll.server.app.engine.js;

import androidx.annotation.NonNull;

import net.xzos.UpgradeAll.server.app.engine.api.EngineApi;

import org.json.JSONObject;

import java.util.ArrayList;

public class JavaScriptEngine extends EngineApi {

    private static final String TAG = "JavaScriptEngine";
    private static String[] LogObjectTag;

    private JavaScriptCoreEngine javaScriptCoreEngine;

    private int releaseNum = 0;
    private ArrayList<String> versionNumberList = new ArrayList<>();
    private ArrayList<JSONObject> releaseDownloadList = new ArrayList<>();

    private JavaScriptEngine(@NonNull Builder builder) {
        this.javaScriptCoreEngine = builder.javaScriptCoreEngine;
        LogObjectTag = builder.logObjectTag;
        if (builder.enableLogJsCode) {
            Log.i(LogObjectTag, TAG, String.format("JavaScriptCoreEngine: jsCode: \n%s", builder.jsCode));  // 只打印一次 JS 脚本
        }
    }

    public static class Builder {
        private final JavaScriptCoreEngine javaScriptCoreEngine;
        private final String[] logObjectTag;
        private final String jsCode;
        private boolean enableLogJsCode;

        public Builder(String[] logObjectTag, String URL, String jsCode) {
            this.javaScriptCoreEngine = new JavaScriptCoreEngine(logObjectTag, URL, jsCode);
            this.logObjectTag = javaScriptCoreEngine.getLogObjectTag();
            this.jsCode = jsCode;
        }

        public Builder enableLogJsCode(boolean enableLogJsCode) {
            this.enableLogJsCode = enableLogJsCode;
            return this;
        }

        public JavaScriptEngine build() {
            return new JavaScriptEngine(this);
        }
    }

    @Override
    public boolean refreshData() {
        int releaseNum = getReleaseNum();
        if (releaseNum != 0) {
            for (int i = 0; i < releaseNum; i++) {
                versionNumberList.add(getVersionNumber(i));
                releaseDownloadList.add(getReleaseDownload(i));
            }
            return true;
        } else
            return false;
    }

    @Override
    public int getReleaseNum() {
        if (this.releaseNum == 0) {
            try {
                this.releaseNum = javaScriptCoreEngine.getReleaseNum();
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getReleaseNum: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
            }
        }
        return this.releaseNum;
    }

    @Override
    public String getVersionNumber(int releaseNum) {
        String versionNumber = null;
        if (versionNumberList.size() == 0) {
            try {
                versionNumber = javaScriptCoreEngine.getVersionNumber(releaseNum);
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getVersionNumber: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
                return null;
            }
        } else if (releaseNum >= 0 && releaseNum < versionNumberList.size())
            versionNumber = versionNumberList.get(releaseNum);
        return versionNumber;
    }

    @Override
    public JSONObject getReleaseDownload(int releaseNum) {
        JSONObject releaseDownload = null;
        if (releaseDownloadList.size() == 0) {
            try {
                releaseDownload = javaScriptCoreEngine.getReleaseDownload(releaseNum);
            } catch (Throwable e) {
                Log.e(LogObjectTag, TAG, "getReleaseDownload: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
                return null;
            }
        } else if (releaseNum >= 0 && releaseNum < releaseDownloadList.size())
            releaseDownload = releaseDownloadList.get(releaseNum);
        return releaseDownload;
    }

    @Override
    public String getDefaultName() {
        try {
            return javaScriptCoreEngine.getDefaultName();
        } catch (Throwable e) {
            Log.e(LogObjectTag, TAG, "getDefaultName: 脚本执行错误, ERROR_MESSAGE: " + e.toString());
        }
        return null;
    }
}