package com.example.absensipribadi
import android.content.Context
import java.security.MessageDigest
object Security {
 fun hash(s:String)=MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString(""){"%02x".format(it)}
 fun verify(s:String,h:String)=hash(s)==h
 fun prefs(c:Context)=c.getSharedPreferences("secure",Context.MODE_PRIVATE)
}
