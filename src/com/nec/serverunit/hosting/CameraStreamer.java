

package com.nec.serverunit.hosting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;




import com.nec.serverunit.AlertDispatch;
//import com.nec.serverunit.AlertDispatch;
import com.nec.serverunit.MotionDetection;
import com.nec.serverunit.io.DataSink;
import com.nec.serverunit.io.DataWriter;

import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

/* package */ final class CameraStreamer 
{
    private static final String TAG = CameraStreamer.class.getSimpleName();
   
    private static final int MESSAGE_TRY_START_STREAMING = 0;
    private static final int MESSAGE_SEND_PREVIEW_FRAME = 1;

    private static final long OPEN_CAMERA_POLL_INTERVAL_MS = 1000L;

    private final Object mLock = new Object();
   

    private final boolean mUseFlashLight;
    private final int mJpegQuality;
    

    private boolean mRunning = false;
    private Looper mLooper = null;
    private Handler mWorkHandler = null;
    private Camera mCamera = null;
    private int mPreviewFormat = Integer.MIN_VALUE;
    private int mPreviewWidth = Integer.MIN_VALUE;
    private int mPreviewHeight = Integer.MIN_VALUE;
    private Rect mPreviewRect = null;
    private int mPreviewBufferSize = Integer.MIN_VALUE;
    private MemoryOutputStream memoryOutputStream = null;
   private MJpegHttpStreamer mMJpegHttpStreamer = null;
    public Context SCact;

    

    /* package */ CameraStreamer(final boolean useFlashLight, 
            final int jpegQuality, final SurfaceHolder previewDisplay, Camera cameraarg,Context CSGL )
    {
    	super();
    	Log.i(TAG,"Started Camera Stream contructor");
    	SCact = CSGL;
        if (previewDisplay == null)
        {throw new IllegalArgumentException("previewDisplay must not be null");} // if
        mCamera = cameraarg;
        mUseFlashLight = useFlashLight;
        mJpegQuality = jpegQuality;
    } // constructor(SurfaceHolder)

    private  final class WorkHandler extends Handler
    {
        private WorkHandler(final Looper looper)
        {super(looper);}

        @Override
        public void handleMessage(final Message message)
        {
            switch (message.what)
            {
                case MESSAGE_TRY_START_STREAMING:
                    tryStartStreaming();
                    break;
                case MESSAGE_SEND_PREVIEW_FRAME:
                    final Object[] args = (Object[]) message.obj;
                    sendPreviewFrame((byte[]) args[0], (Camera) args[1], (Long) args[2]);
                    break;
                default:
                    throw new IllegalArgumentException("cannot handle message");
            } // switch
        } // handleMessage(Message)
    } // class WorkHandler

    /* package */ void start()
    {
        synchronized (mLock)
        {
            if (mRunning)
            {throw new IllegalStateException("CameraStreamer is already running");}
            mRunning = true;
        } // synchronized

        final HandlerThread worker = new HandlerThread(TAG, Process.THREAD_PRIORITY_MORE_FAVORABLE);
        worker.setDaemon(true);
        worker.start();
        mLooper = worker.getLooper();
        mWorkHandler = new WorkHandler(mLooper);
        mWorkHandler.obtainMessage(MESSAGE_TRY_START_STREAMING).sendToTarget();
    } // start()

    /**
     *  Stop the image streamer. The camera will be released during the
     *  execution of stop() or shortly after it returns. stop() should
     *  be called on the main thread.
     */
    /* package */ void stop()
    {
        synchronized (mLock)
        {
            if (!mRunning){throw new IllegalStateException("CameraStreamer is already stopped");}
            mRunning = false;
            if (mMJpegHttpStreamer != null)
            {mMJpegHttpStreamer.stop();mMJpegHttpStreamer = null;}
         
        } // synchronized
        mLooper.quit();
    } // stop()

    private void tryStartStreaming()
    {
    	CameraCallback	mCameraCallback = new CameraCallback(SCact);
    	  	mCamera.setPreviewCallback(mCameraCallback);
		 try
        {
            while (true)
            {
                try{startStreamingIfRunning();}
                catch (final RuntimeException openCameraFailed)
                {
                    Log.d(TAG, "Open camera failed, retying in " + OPEN_CAMERA_POLL_INTERVAL_MS
                            + "ms", openCameraFailed);
                    Thread.sleep(OPEN_CAMERA_POLL_INTERVAL_MS);
                    continue;
                } // catch
               break;
            } // while
        } // try
        catch (final Exception startPreviewFailed)
        {
            // Captures the IOException from startStreamingIfRunning and
            // the InterruptException from Thread.sleep.
            Log.w(TAG, "Failed to start camera preview", startPreviewFailed);
        } // catch
    	
    } // tryStartStreaming()

    private void startStreamingIfRunning() throws IOException
    {//final Camera camera = Camera.open();
      if (mCamera == null){throw new IllegalStateException("CameraStreamer is already running");}
    	final Camera.Parameters params = mCamera.getParameters();
       if (mUseFlashLight){params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);}
       final int[] range = params.getSupportedPreviewFpsRange().get(0);
      params.setPreviewFpsRange(range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
              range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
      mCamera.setParameters(params);
        mPreviewFormat = params.getPreviewFormat();
        final Camera.Size previewSize = params.getPreviewSize();
        mPreviewWidth = previewSize.width;
        mPreviewHeight = previewSize.height;
        final int bytesPerPixel = ImageFormat.getBitsPerPixel(mPreviewFormat) / 8;
        mPreviewBufferSize = mPreviewWidth * mPreviewHeight * bytesPerPixel*20;
        mCamera.addCallbackBuffer(new byte[mPreviewBufferSize]);
        mPreviewRect = new Rect(0, 0, mPreviewWidth, mPreviewHeight);
        memoryOutputStream = new MemoryOutputStream(mPreviewBufferSize);
        
        mMJpegHttpStreamer = new MJpegHttpStreamer(mPreviewBufferSize, SCact);
        mMJpegHttpStreamer.start();
       
     
    } // startStreamingIfRunning()

   private void sendPreviewFrame(final byte[] data, final Camera camera, final long timestamp)
   {
   // Create JPEG
       try{
        final YuvImage image = new YuvImage(data, mPreviewFormat, mPreviewWidth, mPreviewHeight,null);
        image.compressToJpeg(mPreviewRect, mJpegQuality, memoryOutputStream);

        mMJpegHttpStreamer.streamJpeg(memoryOutputStream.getBuffer(), memoryOutputStream.getLength(),
                timestamp);
        memoryOutputStream.seek(0);
        
        
        
       }catch(Exception e){} 
   } // sendPreviewFrame(byte[], camera, long)


   final class CameraCallback implements Camera.PreviewCallback, 
   Camera.PictureCallback {

   
   private static final int PICTURE_DELAY = 10000;

   private static final String TAG = "CameraCallback";
   private MotionDetection mMotionDetection;
   private Context mContextcb;

   private long mReferenceTime;
   private DataWriter mDataWriter;


   public CameraCallback(Context ct) {
   	mDataWriter = new DataWriter();
   	mContextcb = ct;
   	//new Recorder(mCamera);
   	mMotionDetection = new MotionDetection();
   }

   @Override
   public void onPictureTaken(byte[] data, Camera camera) {
	   camera.startPreview();
	   String pictureName = System.currentTimeMillis()+".jpg";
   	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Surveillance Storage");
      

      if (! mediaStorageDir.exists()){
          if (! mediaStorageDir.mkdirs()){
              Log.d("Surveillance Storage", "failed to create directory");
              
          }
      }
   	
   	
   	File f = new File(mediaStorageDir.getPath(),pictureName);
   	FileOutputStream fos = null;
   	try {
   		fos = new FileOutputStream(f);
   		Toast.makeText(mContextcb, "Picture captured", Toast.LENGTH_SHORT).show();
   	} catch (IOException e) {
   		Log.e(TAG, "Cannot write picture to disk");
   		e.printStackTrace();
   	}
   	
   	DataSink<FileOutputStream>df = new DataSink<FileOutputStream>(data,fos);
   	mDataWriter.writeAsync(df);
   
   }

   @Override
   public void onPreviewFrame(byte[] data, Camera camera) {
   	 

	   final Long timestamp = SystemClock.elapsedRealtime();
       final Message message = mWorkHandler.obtainMessage();
       message.what = MESSAGE_SEND_PREVIEW_FRAME;
       message.obj = new Object[]{ data, camera, timestamp };
       message.sendToTarget();
	   Context adContext = SCact;
   	  	 if (mMotionDetection.detect(data)) {
   		long now = System.currentTimeMillis();
   		if (now > mReferenceTime + PICTURE_DELAY) {
   			mReferenceTime = now + PICTURE_DELAY;
   			camera.takePicture(null, null, this);
   			AlertDispatch ad = new AlertDispatch(adContext);
   			 
   		} else {
   			Log.i(TAG, "Not taking picture because not enough time has "
   					+ "passed since the creation of the Surface");
   		}//else
   	}//if
   	  	 //camera.addCallbackBuffer(data);
   }//onPreviewframe
   }
} // class CameraStreamer

