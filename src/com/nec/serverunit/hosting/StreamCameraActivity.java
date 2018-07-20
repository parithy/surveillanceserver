

package com.nec.serverunit.hosting;


import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Context;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.nec.serverunit.R;





import com.nec.serverunit.hosting.StreamCameraActivity;

import org.apache.http.conn.util.InetAddressUtils;


public final class StreamCameraActivity extends Activity implements SurfaceHolder.Callback  
{
    private static final String TAG = StreamCameraActivity.class.getSimpleName();
    private static final boolean PREF_FLASH_LIGHT_DEF = false;
    private static final int PREF_JPEG_QUALITY_DEF = 50;
    private boolean streaming = false;
    public  boolean mPreviewDisplayCreated = false;
       private SurfaceHolder holderPreviewDisplay = null;
    private CameraStreamer mCameraStreamer = null;
    
    private String mIpAddress = "";
    private boolean mUseFlashLight = PREF_FLASH_LIGHT_DEF;
    private String mPort = "9200";
    private int mJpegQuality = PREF_JPEG_QUALITY_DEF;
    private TextView mIpAddressView = null;

    
    SharedPreferences SP;
	SharedPreferences.OnSharedPreferenceChangeListener listener;

	public Camera mCamera;
	public Context mContext;
	public boolean mMotionDetectionActive = true;
	
    

    @Override
    protected void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        		
      setContentView(R.layout.main);
      
    } // onCreate(Bundle)

    
	@Override
    protected void onResume()
    {
        super.onResume();
       
        holderPreviewDisplay = ((SurfaceView) findViewById(R.id.camera)).getHolder();
       holderPreviewDisplay.addCallback(this);
        mContext = this;
        mMotionDetectionActive = true;
		mIpAddress = tryGetIpAddress();
        mIpAddressView = (TextView) findViewById(R.id.ip_address);
    	SP = mContext.getSharedPreferences("portspfile", 0);
    	 mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
    	 
    	listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
	        public void onSharedPreferenceChanged(
	            SharedPreferences prefs, String key) {
	        	Log.d("mainactivity","change detected");
	        	mPort=prefs.getString(key, "").toString();
	        	 mIpAddressView.setText("http://" + mIpAddress + ":" + mPort + "/");
	        }
	    };
	    SP.registerOnSharedPreferenceChangeListener(listener);
        
    } // onResume()

    @Override
    protected void onPause()
    {
        super.onPause();
    } // onPause()
    
    public void onDestroy(Bundle savedInstanceState) {
		super.onDestroy();
		SP.unregisterOnSharedPreferenceChangeListener(listener);
	}

    @Override
    
    public void surfaceChanged(final SurfaceHolder holder, final int format, final int width,
            final int height)
    {
    	 mPreviewDisplayCreated = true;
		try{mCamera.startPreview();}
		catch(Exception e){Toast.makeText(mContext, "Camera Inaccessible or not found. Please Try again", Toast.LENGTH_LONG).show();}
		tryStartCameraStreamer();
    } // surfaceChanged(SurfaceHolder, int, int, int)

    @Override
    public void surfaceCreated(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = true;
        //Log.i(TAG, "Entered into surfaceCreated");
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) 
            	try {mCamera = Camera.open(i);i=10;} catch (Exception e) {Toast.makeText(mContext, "Camera Inaccessible. Try closing other camera applications.", Toast.LENGTH_LONG).show();
                finish();}
        }
        
        if(mCamera == null)
        {
    		Toast.makeText(mContext, "Camera Inaccessible.Try closing other camera applications.", Toast.LENGTH_SHORT).show();
            finish();
           
	    }
        else if(mMotionDetectionActive) {
        		try {mCamera.setPreviewDisplay(holder);} 
				catch (IOException exception) {
				Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
				closeCamera();
				Toast.makeText(mContext, "Unable to get buffer images from camera.", Toast.LENGTH_SHORT).show();
				finish();
				}
			} 
       
       
		 
    }// surfaceCreated(SurfaceHolder) 
    @Override
    public void surfaceDestroyed(final SurfaceHolder holder)
    {
        mPreviewDisplayCreated = false;
       ensureCameraStreamerStopped();
       
    } // surfaceDestroyed(SurfaceHolder)

    
    
    public void tryStartCameraStreamer()
    {
        if (mPreviewDisplayCreated&&!streaming)
        {
            mCameraStreamer = new CameraStreamer(mUseFlashLight, mJpegQuality,
            		holderPreviewDisplay,mCamera,mContext);
            mCameraStreamer.start();
            streaming = true;
       } // if
    } // tryStartCameraStreamer()

    public void ensureCameraStreamerStopped()
    {
    	Log.i(TAG+"ensurecamerastreamerstopped method", "Closing camerastreamer");
    	if (mCameraStreamer != null)
        {
            mCameraStreamer.stop();
            mCameraStreamer = null;
        } // if
    	streaming = false;
    	closeCamera();
    } // stopCameraStreamer()
    


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
    	    

        
     
    
    } // tryGetIpAddress()
    
    
   
    
    
    public void closeCamera() {
		Log.i(TAG, "Closing camera and freeing its resources close camera method");

		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
        	
		}
	}
    
}


