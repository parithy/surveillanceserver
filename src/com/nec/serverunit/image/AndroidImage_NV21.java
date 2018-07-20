package com.nec.serverunit.image;

import android.util.Log;

/**
 * @author Marco Dinacci <marco.dinacci@gmail.com>
 */
public class AndroidImage_NV21 extends AbstractAndroidImage {

	private static final String TAG = "AndroidImage_NV21";

	public AndroidImage_NV21(byte[] data) {
		super(data);
	}

	@Override
	public boolean isDifferent(AndroidImage other, int pixel_threshold, 
			int threshold) {
		
		if(!assertImage(other))
			return false;
		
		byte[] otherData = other.get();
		int totDifferentPixels = 0;
		
		// FIXME for the sake of making it working
		// 640x480 = 307200
		
		for (int i = 0, ij=0; i < 480; i++) {
			for (int j = 0; j < 640; j++,ij++) {
				int pix = (0xff & ((int) mData[ij])) - 16;
				int otherPix = (0xff & ((int) otherData[ij])) - 16;
				
				if (pix < 0) pix = 0;
				if (pix > 255) pix = 255;
				if (otherPix < 0) otherPix = 0;
				if (otherPix > 255) otherPix = 255;

				if(Math.abs(pix - otherPix) >= pixel_threshold)
					totDifferentPixels++;
			}
		}
		
		if(totDifferentPixels == 0) totDifferentPixels = 1;
		//Log.d(TAG, "Number of different pixels: " + totDifferentPixels + " -> " 
		//		+ (100 / ( size / totDifferentPixels) ) + "%");
		
		return totDifferentPixels > threshold;
	}
	
	@Override
	public AndroidImage toGrayscale() {
		// FIXME this is wrong
		return this;
	}

}
