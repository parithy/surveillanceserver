package com.nec.serverunit;

import android.content.SharedPreferences;
import android.util.Log;

import com.nec.serverunit.image.AndroidImage;
import com.nec.serverunit.image.AndroidImage_NV21;


/* TODO analyse the scene and automatically set the values for the motion detection */
/* TODO evaluate performance, consider using NDK for optimal speed */
public class MotionDetection {

	private static final String TAG = "MotionDetection";

	private AndroidImage mBackground;

	// The image that is used for motion detection
	private AndroidImage mAndroidImage;

	
	public boolean detect(byte[] data) {
		if(mBackground == null) {
			//mBackground = AndroidImageFactory.createImage(data, mSize.value, 
			//		mPixelFormat.value);//.erode(mErosionLevel.value);
			mBackground= new AndroidImage_NV21(data);
//			Log.i(TAG, "Creating background image");
			return false;
		}
		
		boolean motionDetected = false;
		
		// TODO avoid creating every time a new image, reuse an existing one
		
		mAndroidImage = new AndroidImage_NV21(data);
		
		motionDetected = mAndroidImage.isDifferent(mBackground, 
				25, 9216);

	//	Log.i(TAG, "Image is different ? " + motionDetected);
		
		mBackground = mAndroidImage;
		
		return motionDetected;
	}
}