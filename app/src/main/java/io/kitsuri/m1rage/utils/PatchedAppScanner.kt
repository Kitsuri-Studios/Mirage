import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.kitsuri.m1rage.model.PatchedAppInfo

object PatchedAppScanner {

    private const val TAG = "PatchedAppScanner"
    private const val HXO_PROVIDER = "com.hxo.loader.HxoLoader"
    private const val HXO_META_DATA_KEY = "io.kitsur.HXO_LOADED"

    fun scanPatchedApps(context: Context): List<PatchedAppInfo> {
        val pm = context.packageManager
        val result = mutableListOf<PatchedAppInfo>()

        val installedApps = pm.getInstalledApplications(0)

        for (app in installedApps) {
            try {
                val pkgInfo = pm.getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PROVIDERS or PackageManager.GET_META_DATA
                )

                val appInfo = pkgInfo.applicationInfo ?: continue


                val isPatched =
                    pkgInfo.providers?.any { it.name == HXO_PROVIDER } == true
                            ||
                            appInfo.metaData
                                ?.getBoolean(HXO_META_DATA_KEY, false) == true

                if (!isPatched) continue

                val appName = pm.getApplicationLabel(appInfo).toString()
                val icon = try {
                    pm.getApplicationIcon(appInfo)
                } catch (_: Exception) {
                    pm.defaultActivityIcon
                }

                result.add(
                    PatchedAppInfo(
                        packageName = app.packageName,
                        appName = appName,
                        icon = icon
                    )
                )

            } catch (e: Exception) {
                Log.w(TAG, "Skipping ${app.packageName}", e)
            }
        }

        return result
    }
}
