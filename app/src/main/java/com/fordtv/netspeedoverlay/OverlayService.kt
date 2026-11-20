package com.fordtv.netspeedoverlay
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.TextView
import android.app.usage.UsageStatsManager
import android.net.TrafficStats
class OverlayService : Service() {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var tvLine1: TextView? = null
    private var tvLine2: TextView? = null
    private var tvHint: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes = 0L
    private var lastTime = 0L
    private val targetPackages = setOf("com.netflix.ninja","com.netflix.mediaclient","com.hbo.hbomax","com.disney.disneyplus","com.amazon.amazonvideo.livingroom","com.google.android.youtube.tv")
    private val runnable = object : Runnable { override fun run() { updateStats(); handler.postDelayed(this, 1000) } }
    override fun onCreate() { super.onCreate(); createNotificationChannel(); startForeground(1, buildNotification()); showOverlay(); lastRxBytes = TrafficStats.getTotalRxBytes(); lastTime = System.currentTimeMillis(); handler.post(runnable) }
    override fun onDestroy() { handler.removeCallbacks(runnable); windowManager?.removeView(overlayView); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { val ch = NotificationChannel("overlay_ch", "悬浮窗服务", NotificationManager.IMPORTANCE_LOW); (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch) } }
    private fun buildNotification(): Notification { val pi = PendingIntent.getActivity(this,0,Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE); return Notification.Builder(this,"overlay_ch").setContentTitle("Netflix屏显模式运行中").setContentText("仅显示分辨率(推测) + 实时码率").setSmallIcon(android.R.drawable.stat_notify_sync).setContentIntent(pi).build() }
    private fun showOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_window, null)
        tvLine1 = overlayView!!.findViewById(R.id.tvLine1); tvLine2 = overlayView!!.findViewById(R.id.tvLine2); tvHint = overlayView!!.findViewById(R.id.tvHint)
        var initialX=0; var initialY=0; var touchX=0f; var touchY=0f
        overlayView!!.setOnTouchListener { _, event -> val params = overlayView!!.layoutParams as WindowManager.LayoutParams; when(event.action){ MotionEvent.ACTION_DOWN -> { initialX=params.x; initialY=params.y; touchX=event.rawX; touchY=event.rawY; true } MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - touchX).toInt(); params.y = initialY + (event.rawY - touchY).toInt(); windowManager!!.updateViewLayout(overlayView, params); true } else -> false } }
        overlayView!!.findViewById<View>(R.id.btnClose).setOnClickListener { stopSelf() }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.START; params.x=24; params.y=24; windowManager!!.addView(overlayView, params)
    }
    private fun updateStats() {
        val now = System.currentTimeMillis(); val curRx = TrafficStats.getTotalRxBytes()
        if (curRx == TrafficStats.UNSUPPORTED.toLong()) { tvLine1?.text="不支持流量统计"; return }
        val deltaBytes = curRx - lastRxBytes; val deltaTime = (now - lastTime)/1000.0; if (deltaTime<=0) return
        val speedBps = deltaBytes * 8 / deltaTime; val fg = getForegroundApp(); val isTarget = fg in targetPackages; lastRxBytes = curRx; lastTime = now; val speedMbps = speedBps / 1_000_000.0
        tvLine1?.text = guessResolution(speedMbps, isTarget)
        tvLine2?.text = if(isTarget) String.format("Bitrate: %.2f Mbps (%d kbps)", speedMbps, (speedMbps*1000).toInt()) else String.format("Bitrate: %.2f Mbps (非目标)", speedMbps)
        tvHint?.text = if(isTarget) "*网络估算，非DRM真实值 | 拖动移动" else "请打开 Netflix/HBO 播放"
    }
    private fun guessResolution(mbps: Double, isTarget: Boolean): String {
        if(!isTarget) return "Resolution: -- (等待目标应用)"
        return when { mbps < 1.0 -> "Resolution: 640x360 (推测) | SD"; mbps < 2.5 -> "Resolution: 854x480 (推测) | SD"; mbps < 5.0 -> "Resolution: 1280x720 (推测) | HD"; mbps < 10.0 -> "Resolution: 1920x1080 (推测) | FHD"; mbps < 18.0 -> "Resolution: 2560x1440 (推测) | QHD"; else -> "Resolution: 3840x2160 (推测) | UHD 4K" }
    }
    private fun getForegroundApp(): String? { val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager; val end = System.currentTimeMillis(); val list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, end-5000, end); if(list.isNullOrEmpty()) return null; return list.maxByOrNull{it.lastTimeUsed}?.packageName }
}
