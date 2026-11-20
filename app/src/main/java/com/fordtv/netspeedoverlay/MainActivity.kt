package com.fordtv.netspeedoverlay
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<MaterialButton>(R.id.btnOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
            } else Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnUsage).setOnClickListener {
            if (!hasUsageAccess()) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                Toast.makeText(this, "请找到本应用并开启 使用情况访问权限", Toast.LENGTH_LONG).show()
            } else Toast.makeText(this, "使用情况权限已授予", Toast.LENGTH_SHORT).show()
        }
        findViewById<MaterialButton>(R.id.btnStart).setOnClickListener {
            if (!Settings.canDrawOverlays(this) || !hasUsageAccess()) {
                Toast.makeText(this, "请先授予上方两个权限", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            val svc = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)
            Toast.makeText(this, "悬浮窗已启动，按HOME键再打开Netflix", Toast.LENGTH_LONG).show()
        }
        findViewById<MaterialButton>(R.id.btnStop).setOnClickListener { stopService(Intent(this, OverlayService::class.java)) }
    }
    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }
}
