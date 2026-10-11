package com.araf.buddy
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.webkit.*

class MainActivity:Activity(){
 lateinit var web:WebView
 val mic=Manifest.permission.RECORD_AUDIO
 val ui=Handler(Looper.getMainLooper())
 val full=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

 override fun onCreate(b:Bundle?){
  super.onCreate(b)
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  if(Build.VERSION.SDK_INT>=28){val a=window.attributes;a.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;window.attributes=a}
  web=WebView(this).apply{
   settings.javaScriptEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;settings.setSupportZoom(false)
   overScrollMode=View.OVER_SCROLL_NEVER;isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false
   setBackgroundColor(0);setLayerType(View.LAYER_TYPE_HARDWARE,null)
   addJavascriptInterface(Bridge(this@MainActivity),"Android")
   systemUiVisibility=full
   webViewClient=object:WebViewClient(){override fun onPageFinished(v:WebView?,u:String?){Hub.svc?.sync(web)}}
   loadUrl("file:///android_asset/index.html")}
  setContentView(web)
  ensureService()
 }
 override fun onWindowFocusChanged(h:Boolean){super.onWindowFocusChanged(h);if(h)web.systemUiVisibility=full}
 fun ensureService(){
  val ok=checkSelfPermission(mic)==PackageManager.PERMISSION_GRANTED
  if(ok&&Hub.svc==null)startForegroundService(Intent(this,PikoService::class.java))
  val need=ArrayList<String>()
  if(!ok)need.add(mic)
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)need.add(Manifest.permission.POST_NOTIFICATIONS)
  if(need.isNotEmpty())requestPermissions(need.toTypedArray(),1)
 }
 override fun onRequestPermissionsResult(c:Int,p:Array<out String>,g:IntArray){
  super.onRequestPermissionsResult(c,p,g)
  if(checkSelfPermission(mic)==PackageManager.PERMISSION_GRANTED&&Hub.svc==null)startForegroundService(Intent(this,PikoService::class.java))
  nextSetup()
 }
 fun nextSetup(){
  val sp=getSharedPreferences("p",0)
  if(checkSelfPermission(mic)!=PackageManager.PERMISSION_GRANTED||sp.getString("gkey","").isNullOrBlank())return
  ui.postDelayed({
   val nl=Settings.Secure.getString(contentResolver,"enabled_notification_listeners")?.contains(packageName)==true
   if(!Settings.canDrawOverlays(this)&&!sp.getBoolean("askedOv",false)){
    sp.edit().putBoolean("askedOv",true).apply()
    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))
   }else if(!nl&&!sp.getBoolean("asked",false)){
    sp.edit().putBoolean("asked",true).apply()
    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
   }
  },1500)
 }
 override fun onStart(){super.onStart();Hub.svc?.hideBubble()}
 override fun onResume(){
  super.onResume();web.onResume();Hub.webs.addIfAbsent(web)
  Hub.svc?.hideBubble();Hub.svc?.sync(web);nextSetup()
 }
 override fun onPause(){super.onPause();web.onPause();Hub.webs.remove(web)}
 override fun onStop(){super.onStop();Hub.svc?.showBubble()}
 override fun onDestroy(){ui.removeCallbacksAndMessages(null);Hub.webs.remove(web);web.destroy();super.onDestroy()}
}
