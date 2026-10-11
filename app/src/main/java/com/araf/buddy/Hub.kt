package com.araf.buddy
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.webkit.JavascriptInterface
import android.webkit.WebView
import java.util.concurrent.CopyOnWriteArrayList

object Hub{val webs=CopyOnWriteArrayList<WebView>();var svc:PikoService?=null}

class Bridge(val c:Context){
 val h=Handler(Looper.getMainLooper())
 fun key():String=c.getSharedPreferences("p",0).getString("gkey","")?:""
 @JavascriptInterface fun tap(){h.post{val s=Hub.svc;if(s!=null)s.toggle() else (c as? MainActivity)?.ensureService()}}
 @JavascriptInterface fun hasKey():Boolean=key().isNotBlank()
 @JavascriptInterface fun keyHint():String=key().let{if(it.length>4)"\u2022\u2022\u2022\u2022"+it.takeLast(4) else ""}
 @JavascriptInterface fun saveKey(k:String){c.getSharedPreferences("p",0).edit().putString("gkey",k.trim()).apply();h.postDelayed({(c as? MainActivity)?.nextSetup()},1200)}
 @JavascriptInterface fun openUrl(u:String){
  if(u.startsWith("https://discord.gg/")||u.startsWith("https://aistudio.google.com/"))
   h.post{c.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
 @JavascriptInterface fun openSetting(k:String){
  val i=when(k){
   "overlay"->Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+c.packageName))
   "write"->Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+c.packageName))
   "notif"->Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
   "access"->Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
   else->return}
  h.post{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
}
