package com.nec.serverunit;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.http.conn.util.InetAddressUtils;

import android.os.Bundle;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsListener  extends BroadcastReceiver{
	 
	private String phonenumber = "9840186146";
    private String ipaddr = tryGetIpAddress(); 
    private String message;
    SharedPreferences SP;


    public void onReceive(Context context, Intent intent) {

		 if (ipaddr.equals(""))
		 {message = "@+id/cdnnetworkiprequest";}
		 else
		 message = "@+id/cdnnetworkokiprequest"+"http://" + ipaddr +":8080";
    	SP = context.getSharedPreferences("spfile",0);  
        if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
            SmsMessage[] msgs = null;
            String msg_from= "";
            String str = "";
            if (bundle != null){
               try{   
            	Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                      for(int i=0; i<msgs.length; i++){
                        msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
                        msg_from = msgs[i].getOriginatingAddress();
                        str ="Message from client :"+ msgs[i].getMessageBody().toString();
                        
                                       }
                }
                    catch(Exception e){}
                if (SP.getString("Phonenumber Cache", "").matches(msg_from)||msg_from.matches(SP.getString("Phonenumber Cache", "")))
        		{                    	
                	Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
        	 SmsManager sms = SmsManager.getDefault();
             try{
        	 sms.sendTextMessage(phonenumber, null, message, null,null);
             Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
             }
             catch(Exception e){Toast.makeText(context, "Not a valid number or message", Toast.LENGTH_SHORT).show();}
        }

                    }
                   
                                                       }
        
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
   } catch (Exception ex) { } // for now eat exceptions
   return "";
   }
        }
    



