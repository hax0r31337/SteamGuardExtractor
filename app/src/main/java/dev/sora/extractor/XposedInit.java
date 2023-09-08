package dev.sora.extractor;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XposedInit implements IXposedHookLoadPackage {

    public final static String TAG = "SteamGuardExtractor";

    private static Context ctx;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        Log.i(TAG, "Load package: " + lpparam.packageName);

        if (lpparam.packageName.equals("com.google.android.webview")) {
            return;
        }

        // steam uses https://docs.expo.dev/versions/latest/sdk/securestore/ to store secrets of SteamGuard
        Class<?> classSecureStoreModule = lpparam.classLoader.loadClass("expo.modules.securestore.SecureStoreModule");
        /*
    AsyncFunction("setValueWithKeyAsync") Coroutine { value: String?, key: String?, options: SecureStoreOptions ->
      key ?: throw NullKeyException()
      return@Coroutine setItemImpl(key, value, options, false)
    }
         */
        XposedBridge.hookAllMethods(classSecureStoreModule, "setValueWithKeyAsync", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                secretCallback(param.args[1].toString());
            }
        });

        // hook read callback
        Class<?> classPromise;
        try {
            classPromise = lpparam.classLoader.loadClass("org.unimodules.adapters.react.PromiseWrapper");
        } catch (ClassNotFoundException e) {
            // https://github.com/expo/fyi/blob/main/expo-modules-migration.md
            classPromise = lpparam.classLoader.loadClass("expo.modules.adapters.react.PromiseWrapper");
        }

        // void resolve(Object value);
        XposedBridge.hookAllMethods(classPromise, "resolve", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!isCalledFromSecureStore()) {
                    return;
                }

                secretCallback(param.args[0].toString());
            }

            private boolean isCalledFromSecureStore() {
                StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
                for (StackTraceElement element : stackTraceElements) {
                    if (element.getClassName().equals(classSecureStoreModule.getName())) {
                        return true;
                    }
                }
                return false;
            }
        });

        // obtain context for pop-up alert
        XposedBridge.hookAllMethods(lpparam.classLoader.loadClass("com.valvesoftware.android.steam.community.MainActivity"), "onCreate", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                ctx = (Context) param.thisObject;
            }
        });
    }

    private static void secretCallback(String secret) {
        Log.i(TAG, secret);

        new AlertDialog.Builder(ctx)
                .setTitle("SteamGuard secret")
                .setMessage(secret)
                .setPositiveButton("Copy", (dialog, id) -> copy(secret))
                .setNegativeButton("OK", (dialog, id) -> { /* do nothing */ })
                .create().show();
    }

    private static void copy(String data) {
        ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", data);
        clipboard.setPrimaryClip(clip);
    }
}
