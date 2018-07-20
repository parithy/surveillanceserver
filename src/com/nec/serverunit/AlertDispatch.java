package com.nec.serverunit;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;


public class AlertDispatch{

	
	private String phonenumber;
    private	String timestamp = new SimpleDateFormat("dd-MMM-yyyy h:mm:ssa").format(new Date());
    private String ipaddr = tryGetIpAddress(); 
    private String message;
    SharedPreferences SP;
	
	 public AlertDispatch (Context context){
		 
		 if (ipaddr.equals(""))
		 {message = R.string.cdnnonetworkalertdispatch + " Intrusion detected at " + timestamp;}
		 else
		 message = R.string.cdnnetworkokalertdispatch+" http://" + ipaddr +":9200" + " Intrusion detected at " + timestamp;
		 SP = context.getSharedPreferences("spfile",0);    
		 phonenumber= SP.getString("Phonenumber Cache", "");
		 SmsManager sms = SmsManager.getDefault();
	       try{
	    	   sms.sendTextMessage(phonenumber, null, message, null,null);
	    	   Toast.makeText(context, "Alert Dispatched to client:"+phonenumber+"Message" + message, Toast.LENGTH_SHORT).show();
	    	   }
	       catch(Exception e){Toast.makeText(context, "No valid phonenumber to alert", Toast.LENGTH_SHORT).show();}
	        
	        
	       
	}
	
	 public static String tryGetIpAddress()
	    {
	 try {
         List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
         for (NetworkInterface intf : interfaces) {
             List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
             for (InetAddress addr : addrs) {
                 if (!addr.isLoopbackAddress()) {
                     String sAddr = addr.getHostAddress().toUpperCase();
                     boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr); 
                     boolean useIPv4 = true;
                     if (useIPv4) {
                         if (isIPv4) 
                             return sAddr;
                     } else {
                         if (!isIPv4) {
                             int delim = sAddr.indexOf('%'); // drop ip6 port suffix
                             return delim<0 ? sAddr : sAddr.substring(0, delim);
                         }
                     }
                 }
             }
         }
     } catch (Exception ex) { } 
     return "";
	}
 

}
