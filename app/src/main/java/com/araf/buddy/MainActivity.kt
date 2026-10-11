package com.araf.buddy
import android.Manifest
import android.accessibilityservice.AccessibilityService as A
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.speech.*
import android.speech.tts.*
import android.view.View
import android.view.WindowManager
import android.webkit.*
import org.json.*
import java.net.*
import java.text.SimpleDateFormat
import java.util.*

// ====== CONFIG (hardcoded) ======
const val PROVIDER="gemini"   // "gemini" (free key) or "claude"
val GEMINI_MODELS=listOf("gemini-2.5-flash","gemini-3.6-flash","gemini-2.0-flash")  // tried in order
const val API_KEY="PASTE_YOUR_ANTHROPIC_API_KEY_HERE"
const val MODEL="claude-haiku-5-5"
const val SYS="You are Piko, Araf's super friendly, warm, playful companion living on his phone. Talk like a close friend. Reply in 1-3 short spoken sentences, no markdown, no emojis. If the user speaks Bengali, reply in Bengali script (বাংলা). If the user speaks English, reply in English. Never mix scripts."
fun bn(s:String)=s.any{it in '\u0980'..'\u09FF'}

class MainActivity:Activity(){
 companion object{var cur:MainActivity?=null}
 var talking=false;var chg=false;var mus=false;var susR:Runnable?=null
 lateinit var web:WebView
 var tts:TextToSpeech?=null
 var sr:SpeechRecognizer?=null
 var on=false;var torch=false
 val ui=Handler(Looper.getMainLooper())
 val hist=ArrayList<Pair<String,String>>()
 var lang="en-IN";var mode="auto";var bnNow=false
 fun r(en:String,b:String)=if(bnNow)b else en
 val mic=Manifest.permission.RECORD_AUDIO

 inner class Bridge{
  @JavascriptInterface fun tap(){ui.post{if(on)stopListen() else startListen()}}
  @JavascriptInterface fun hasKey():Boolean=gkey().isNotBlank()
  @JavascriptInterface fun keyHint():String=gkey().let{if(it.length>4)"\u2022\u2022\u2022\u2022"+it.takeLast(4) else ""}
  @JavascriptInterface fun saveKey(k:String){getSharedPreferences("p",0).edit().putString("gkey",k.trim()).apply()}
  @JavascriptInterface fun openUrl(u:String){
   if(u.startsWith("https://discord.gg/")||u.startsWith("https://aistudio.google.com/"))
    ui.post{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}}
 }
 fun gkey():String=getSharedPreferences("p",0).getString("gkey","")?:""
 fun js(s:String){if(!isDestroyed)web.evaluateJavascript(s,null)}

 override fun onCreate(b:Bundle?){
  super.onCreate(b);cur=this
  window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
  web=WebView(this).apply{
   settings.javaScriptEnabled=true;settings.mediaPlaybackRequiresUserGesture=false;settings.setSupportZoom(false);overScrollMode=View.OVER_SCROLL_NEVER;isVerticalScrollBarEnabled=false;isHorizontalScrollBarEnabled=false;setLayerType(View.LAYER_TYPE_HARDWARE,null)
   setBackgroundColor(0);addJavascriptInterface(Bridge(),"Android")
   systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
   loadUrl("file:///android_asset/index.html")}
  setContentView(web)
  tts=TextToSpeech(this){
   tts?.language=Locale("en","IN")
   tts?.setOnUtteranceProgressListener(object:UtteranceProgressListener(){
    override fun onStart(u:String?){ui.post{talking=true;js("setMood('talk')")}}
    override fun onDone(u:String?){ui.post{talking=false;js("setMood('idle')");if(on)listen()}}
    @Deprecated("x") override fun onError(u:String?){onDone(u)}
   })}
  if(checkSelfPermission(mic)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(mic),1)
  val sp=getSharedPreferences("p",0)
  val en=Settings.Secure.getString(contentResolver,"enabled_notification_listeners")?.contains(packageName)==true
  if(!en&&!sp.getBoolean("asked",false))ui.postDelayed({sp.edit().putBoolean("asked",true).apply();startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))},5000)
 }
 val batt=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){
  i?:return
  val st=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1)
  val lv=i.getIntExtra(BatteryManager.EXTRA_LEVEL,0)*100/maxOf(1,i.getIntExtra(BatteryManager.EXTRA_SCALE,100))
  val c2=st==BatteryManager.BATTERY_STATUS_CHARGING||st==BatteryManager.BATTERY_STATUS_FULL
  js("setBattery($lv)")
  if(c2!=chg){chg=c2;js("fx('charging',$c2)")}}}
 val poll=object:Runnable{override fun run(){
  val m=(getSystemService(AUDIO_SERVICE) as AudioManager).isMusicActive&&!talking
  if(m!=mus){mus=m;js("fx('music',$m)")}
  ui.postDelayed(this,2500)}}
 fun onNotif(){ui.post{susR?.let{ui.removeCallbacks(it)};js("sus(true)");val r=Runnable{js("sus(false)")};susR=r;ui.postDelayed(r,4500)}}
 override fun onPause(){super.onPause();web.onPause();web.pauseTimers();try{unregisterReceiver(batt)}catch(e:Exception){};ui.removeCallbacks(poll)}
 override fun onResume(){super.onResume();web.onResume();web.resumeTimers();registerReceiver(batt,IntentFilter(Intent.ACTION_BATTERY_CHANGED));ui.post(poll);if(on)ui.postDelayed({listen()},600)}
 override fun onDestroy(){cur=null;ui.removeCallbacksAndMessages(null);web.destroy();sr?.destroy();tts?.shutdown();super.onDestroy()}

 fun startListen(){
  if(checkSelfPermission(mic)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(mic),1);return}
  on=true;js("setMood('listen')");say("Hey Araf! Piko here, I am listening.")
 }
 fun stopListen(){on=false;sr?.destroy();sr=null;tts?.stop();js("setMood('idle')")}
 var fails=0
 fun retry(){if(!on)return;fails++;ui.postDelayed({if(on)listen()},minOf(400L*fails,4000L))}
 fun initSr(){
  val x=SpeechRecognizer.createSpeechRecognizer(this);sr=x
  x.setRecognitionListener(object:RecognitionListener{
   override fun onResults(r:Bundle?){
    val t=r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
    if(t.isNullOrBlank())retry() else{fails=0;if(mode=="auto")lang=if(bn(t))"bn-BD" else "en-IN";js("setMood('think')");process(t)}}
   override fun onError(e:Int){
    if(e==4||e==5||e==8){x.destroy();if(sr===x)sr=null}
    if(mode=="auto"&&(e==6||e==7))lang=if(lang=="en-IN")"bn-BD" else "en-IN"
    if(e==6||e==7)fails=0
    retry()}
   override fun onReadyForSpeech(p:Bundle?){js("setMood('listen')")}
   override fun onBeginningOfSpeech(){} override fun onRmsChanged(v:Float){} override fun onBufferReceived(b:ByteArray?){}
   override fun onEndOfSpeech(){} override fun onPartialResults(b:Bundle?){} override fun onEvent(t:Int,b:Bundle?){}
  })
 }
 fun listen(){
  if(sr==null)initSr()
  try{
   sr!!.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    .putExtra(RecognizerIntent.EXTRA_LANGUAGE,lang)
    .putExtra("android.speech.extra.ADDITIONAL_LANGUAGES",arrayOf(if(lang=="en-IN")"bn-BD" else "en-IN")))
  }catch(e:Exception){sr?.destroy();sr=null;retry()}
 }
 fun say(s:String){ui.post{tts?.setLanguage(if(bn(s))Locale("bn","BD") else Locale("en","IN"));tts?.speak(s,TextToSpeech.QUEUE_FLUSH,null,"u")}}

 fun process(t:String){
  val r=try{handle(t)}catch(e:Exception){"Oops, that didn't work. ${e.message}"}
  if(r!=null)say(r) else ask(t)
 }
 fun ask(q:String){
  if(PROVIDER=="gemini"&&gkey().isBlank()){say(r("Please add your Gemini API key in settings first.","আগে সেটিংসে Gemini API key যোগ করো।"));return}
  hist.add("user" to q)
  Thread{
   val a=try{if(PROVIDER=="gemini")gemini() else claude()}catch(e:Exception){
    if(bn(q))"আমি ইন্টারনেট বা এপিআই কী-তে সমস্যা পাচ্ছি। একটু চেক করো।" else "I couldn't reach my brain. Check the internet and the API key."}
   hist.add("assistant" to a)
   while(hist.size>12)hist.removeAt(0)
   say(a)
  }.start()
 }
 fun post(u:String,h:Map<String,String>,b:JSONObject):String{
  val c=URL(u).openConnection() as HttpURLConnection
  c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=20000
  c.setRequestProperty("content-type","application/json");h.forEach{(k,v)->c.setRequestProperty(k,v)}
  c.outputStream.write(b.toString().toByteArray())
  return (if(c.responseCode<400)c.inputStream else c.errorStream).bufferedReader().readText()
 }
 fun claude():String{
  val m=JSONArray();hist.toList().forEach{m.put(JSONObject().put("role",it.first).put("content",it.second))}
  val s=post("https://api.anthropic.com/v1/messages",mapOf("x-api-key" to API_KEY,"anthropic-version" to "2023-06-01"),
   JSONObject().put("model",MODEL).put("max_tokens",300).put("system",SYS).put("messages",m))
  return JSONObject(s).getJSONArray("content").getJSONObject(0).getString("text")
 }
 var gi=0
 fun gemini():String{
  val m=JSONArray();hist.toList().forEach{m.put(JSONObject().put("role",if(it.first=="user")"user" else "model").put("parts",JSONArray().put(JSONObject().put("text",it.second))))}
  val b=JSONObject().put("systemInstruction",JSONObject().put("parts",JSONArray().put(JSONObject().put("text",SYS))))
   .put("contents",m).put("generationConfig",JSONObject().put("maxOutputTokens",1024))
  for(k in GEMINI_MODELS.indices){
   val i=(gi+k)%GEMINI_MODELS.size
   try{
    val o=JSONObject(post("https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODELS[i]}:generateContent",mapOf("x-goog-api-key" to gkey()),b))
    val t=o.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
    gi=i;return t
   }catch(e:java.io.IOException){throw e}catch(e:Exception){}
  }
  throw Exception("gemini failed")
 }

 fun go(u:String,pkg:String?){
  val i=Intent(Intent.ACTION_VIEW,Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  if(pkg!=null)i.setPackage(pkg)
  try{startActivity(i)}catch(e:Exception){i.setPackage(null);startActivity(i)}
 }
 fun openApp(n:String):Boolean{
  val pm=packageManager;val k=n.replace(" ","")
  val l=pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
   .firstOrNull{val x=it.loadLabel(pm).toString().lowercase().replace(" ","");x.contains(k)||k.contains(x)}?:return false
  startActivity(pm.getLaunchIntentForPackage(l.activityInfo.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
  return true
 }
 fun bright(abs:Int?,d:Int):String{
  if(!Settings.System.canWrite(this)){
   startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:$packageName")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   return "I need permission to change brightness. Please allow it, then ask me again."}
  val cur=Settings.System.getInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,128)
  val n=(if(abs!=null)abs*255/100 else cur+d).coerceIn(5,255)
  Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS_MODE,0)
  Settings.System.putInt(contentResolver,Settings.System.SCREEN_BRIGHTNESS,n)
  return r("Okay, brightness is ${n*100/255} percent","ঠিক আছে, ব্রাইটনেস এখন ${n*100/255} শতাংশ")
 }
 fun a11y(f:(PhoneService)->String):String{
  val s=PhoneService.i?:run{
   startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   return "Please turn on Piko in Accessibility settings so I can control your phone."}
  return f(s)
 }

val bnMap=listOf("ইউটিউবে" to "youtube","ইউটিউব" to "youtube","গুগলে" to "google","গুগল" to "google","ফেসবুক" to "facebook","হোয়াটসঅ্যাপ" to "whatsapp","ক্যামেরা" to "camera","সেটিংস" to "settings","ক্রোম" to "chrome","ইনস্টাগ্রাম" to "instagram","টিকটক" to "tiktok","ম্যাসেঞ্জার" to "messenger","টেলিগ্রাম" to "telegram","ডিসকর্ড" to "discord",
  "সার্চ করো" to "search","সার্চ কর" to "search","সার্চ" to "search","খুঁজে দেখো" to "search","খুঁজো" to "search","খোঁজো" to "search","ওপেন করো" to "open","ওপেন কর" to "open","ওপেন" to "open","খুলে দাও" to "open","খুলো" to "open","খোল" to "open",
  "ব্রাইটনেস" to "brightness","উজ্জ্বলতা" to "brightness","কমাও" to "dim","কমা" to "dim","বাড়াও" to "increase","বাড়া" to "increase","ভলিউম" to "volume","ফ্ল্যাশলাইট" to "flashlight","টর্চ" to "torch","জ্বালাও" to "on","নিভাও" to "off","স্ক্রিন" to "screen",
  "বাংলায়" to "bangla","বাংলা" to "bangla","ইংরেজিতে" to "english","ইংরেজি" to "english","বলো" to "bolo","এবং" to "and","পিছনে যাও" to "go back","হোমে যাও" to "go home","এখন কয়টা বাজে" to "what time","সময়" to "what time")
 fun norm(s:String):String{var t=s.lowercase();bnMap.forEach{t=t.replace(it.first,it.second)};return t.replace(Regex("\\s+")," ").trim()}
 fun handle(raw:String):String?{
  bnNow=bn(raw);val t=norm(raw)
  if(t.length<40&&listOf("speak","talk","switch","mode","language","bolo").any{it in t}){
   if("bangla" in t||"bengali" in t){mode="bn";lang="bn-BD";bnNow=true;return "ঠিক আছে, এখন থেকে বাংলায় কথা বলব।"}
   if("english" in t){mode="en";lang="en-IN";bnNow=false;return "Okay, I'll speak English."}
   if("auto" in t){mode="auto";return r("Okay, I'll understand both English and Bangla.","ঠিক আছে, আমি বাংলা আর ইংরেজি দুটোই বুঝব।")}
  }
  for(r in listOf(Regex("open youtube (?:and )?(?:search|play)(?: for)? (.+)"),Regex("(?:search|play)(?: for)? (.+) on youtube"),Regex("youtube (?:open )?(?:and )?(.+) (?:search|play)$")))
   r.find(t)?.let{val q=it.groupValues[1];go("https://www.youtube.com/results?search_query="+Uri.encode(q),"com.google.android.youtube");return r("Searching YouTube for $q","ইউটিউবে $q সার্চ করছি")}
  for(r in listOf(Regex("(?:search|google)(?: for)? (.+) (?:on|in) google"),Regex("^(?:google|search for|search) (.+)"),Regex("^(.+?) google search$")))
   r.find(t)?.let{val q=it.groupValues[1];go("https://www.google.com/search?q="+Uri.encode(q),null);return r("Here's what Google says about $q","গুগলে $q খুঁজছি")}
  if("notification" in t&&("access" in t||"enable" in t||"allow" in t)){
   startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
   return r("Please turn on notification access for Piko, so I can keep an eye on them.","নোটিফিকেশন অ্যাক্সেস চালু করে দাও, যাতে আমি নোটিফিকেশন দেখতে পারি।")}
  if("bright" in t||"dim" in t){
   Regex("(\\d+) ?(?:%|percent)").find(t)?.let{return bright(it.groupValues[1].toInt(),0)}
   val down="dim" in t||listOf("decrease","lower","less","reduce","down","min").any{it in t}
   return bright(null,if(down)-60 else 60)
  }
  val am=getSystemService(AUDIO_SERVICE) as AudioManager
  if("volume" in t){
   val up=listOf("up","increase","raise","more","louder").any{it in t}
   am.adjustStreamVolume(AudioManager.STREAM_MUSIC,if(up)AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,AudioManager.FLAG_SHOW_UI)
   return if(up)r("Volume up","ভলিউম বাড়ালাম") else r("Volume down","ভলিউম কমালাম")
  }
  if("flashlight" in t||"torch" in t){
   torch="on" in t.split(" ");val cm=getSystemService(CAMERA_SERVICE) as CameraManager
   cm.setTorchMode(cm.cameraIdList[0],torch);return if(torch)"Flashlight on" else "Flashlight off"
  }
  if("what time" in t)return "It's "+SimpleDateFormat("h:mm a",Locale.getDefault()).format(Date())
  when{
   t=="go back"||t=="back"->return a11y{it.g(A.GLOBAL_ACTION_BACK,"Going back")}
   t=="go home"||t=="home"->return a11y{it.g(A.GLOBAL_ACTION_HOME,"Going home")}
   "recent apps" in t||"recents" in t->return a11y{it.g(A.GLOBAL_ACTION_RECENTS,"Here are your recent apps")}
   "notification" in t->return a11y{it.g(A.GLOBAL_ACTION_NOTIFICATIONS,"Opening notifications")}
   "quick settings" in t->return a11y{it.g(A.GLOBAL_ACTION_QUICK_SETTINGS,"Opening quick settings")}
   "lock" in t&&"screen" in t->return a11y{it.g(A.GLOBAL_ACTION_LOCK_SCREEN,"Locking the screen")}
   "screenshot" in t->return a11y{it.g(A.GLOBAL_ACTION_TAKE_SCREENSHOT,"Took a screenshot")}
   t.startsWith("scroll down")->return a11y{it.swipe("down")}
   t.startsWith("scroll up")->return a11y{it.swipe("up")}
   t.startsWith("click ")||t.startsWith("tap ")||t.startsWith("press ")->{val w=t.substringAfter(" ");return a11y{it.click(w)}}
   t.startsWith("type ")->{val w=t.substringAfter(" ");return a11y{it.type(w)}}
  }
  for(rx in listOf(Regex("^(?:please )?(?:open|launch|start) (.+)"),Regex("^(.+) open$")))rx.find(t)?.let{
   val n=it.groupValues[1]
   return if(openApp(n))r("Opening $n","$n খুলছি") else r("Hmm, I couldn't find an app called $n","$n নামে কোনো অ্যাপ পেলাম না")}
  return null
 }
}
