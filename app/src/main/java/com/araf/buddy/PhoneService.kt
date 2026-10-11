package com.araf.buddy
import android.accessibilityservice.*
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.*

class PhoneService:AccessibilityService(){
 companion object{var i:PhoneService?=null}
 override fun onServiceConnected(){i=this}
 override fun onUnbind(x:Intent?):Boolean{i=null;return super.onUnbind(x)}
 override fun onAccessibilityEvent(e:AccessibilityEvent?){}
 override fun onInterrupt(){}
 fun g(a:Int,m:String):String{performGlobalAction(a);return m}
 fun swipe(dir:String):String{
  val d=resources.displayMetrics;val w=d.widthPixels.toFloat();val h=d.heightPixels.toFloat()
  val down=dir=="down"
  val p=Path().apply{moveTo(w/2,if(down)h*.7f else h*.3f);lineTo(w/2,if(down)h*.3f else h*.7f)}
  dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,300)).build(),null,null)
  return "Scrolling $dir"
 }
 fun click(w:String):String{
  val r=rootInActiveWindow?:return "I can't see the screen right now"
  var n:AccessibilityNodeInfo=r.findAccessibilityNodeInfosByText(w).firstOrNull()?:return "I couldn't find $w on the screen"
  while(!n.isClickable){n=n.parent?:return "I couldn't tap $w"}
  n.performAction(AccessibilityNodeInfo.ACTION_CLICK);return "Tapped $w"
 }
 fun type(s:String):String{
  val n=rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)?:return "Tap a text box first"
  n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,s)})
  return "Typed it"
 }
}
