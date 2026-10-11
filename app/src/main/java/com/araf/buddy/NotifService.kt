package com.araf.buddy
import android.service.notification.*

class NotifService:NotificationListenerService(){
 override fun onNotificationPosted(s:StatusBarNotification?){
  s?:return
  if(s.packageName==packageName||s.isOngoing)return
  Hub.svc?.onNotif()
 }
}
