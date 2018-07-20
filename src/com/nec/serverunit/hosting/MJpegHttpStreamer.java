
package com.nec.serverunit.hosting;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;









import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;


/* package */ final class MJpegHttpStreamer
{
    private static final String TAG = MJpegHttpStreamer.class.getSimpleName();

    private static final String BOUNDARY = "--gc0p4Jq0M2Yt08jU534c0p--";
    private static final String BOUNDARY_LINES = "\r\n" + BOUNDARY + "\r\n";
    DataOutputStream stream;
    ServerSocket serverSocket;
    Socket socket;
    SharedPreferences forport;
    private static final String HTTP_HEADER =
        "HTTP/1.0 200 OK\r\n"
        + "Server: Peepers\r\n"
        + "Connection: close\r\n"
        + "Max-Age: 0\r\n"
        + "Expires: 0\r\n"
        + "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, "
            + "post-check=0, max-age=0\r\n"
        + "Pragma: no-cache\r\n"
        + "Content-Type: multipart/x-mixed-replace; "
            + "boundary=" + BOUNDARY + "\r\n"
        + BOUNDARY_LINES;
   
    private int mPort;
    private boolean mNewJpeg = false;
    private boolean mStreamingBufferA = true;
    private final byte[] mBufferA;
    private final byte[] mBufferB;
    private int mLengthA = Integer.MIN_VALUE;
    private int mLengthB = Integer.MIN_VALUE;
    private long mTimestampA = Long.MIN_VALUE;
    private long mTimestampB = Long.MIN_VALUE;
    private final Object mBufferLock = new Object();
    final Editor editbkf;
    
    private Thread mWorker = null;
    private volatile boolean mRunning = false;

    /* package */ MJpegHttpStreamer(final int bufferSize,Context temp)
    {
        super();
        
        mBufferA = new byte[bufferSize];
        mBufferB = new byte[bufferSize];
        forport = temp.getSharedPreferences("portspfile", 0);
		editbkf = forport.edit();
       
    } // constructor(int, int)

    /* package */ void start()
    { System.err.println("Yep i got into this 1");
        if (mRunning)
        {
            throw new IllegalStateException("MJpegHttpStreamer is already running");
        } // if

        mRunning = true;
        mWorker = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                workerRun();
            } // run()
        });
        mWorker.start();
    } // start()

    /* package */ void stop()
    {
        if (!mRunning)
        {
            throw new IllegalStateException("MJpegHttpStreamer is already stopped");
        } // if

        mRunning = false;
        mWorker.interrupt();
    } // stop()

    /* package */ void streamJpeg(final byte[] jpeg, final int length, final long timestamp)
    {
    	//System.err.println("I entered streamJpeg");
    	synchronized (mBufferLock)
        {
            final byte[] buffer;
            if (mStreamingBufferA)
            {
                buffer = mBufferB;
                mLengthB = length;
                mTimestampB = timestamp;
            } // if
            else
            {
                buffer = mBufferA;
                mLengthA = length;
                mTimestampA = timestamp;
            } // else
            System.arraycopy(jpeg, 0 , buffer, 0 , length); 
            mNewJpeg = true;
            //System.err.println("new jpeg");
            mBufferLock.notify();
        } // synchronized
   } // streamJpeg(byte[], int, long)

    private void workerRun()
    {
        while (mRunning)
        {
            try{ System.err.println("Yep i got into this 2");
            	acceptandstream();}
            catch (final IOException exceptionWhileStreaming)
            {System.err.println(exceptionWhileStreaming);}
            
        } // while
    } // mainLoop()

    private void acceptandstream() throws IOException
    {
        serverSocket = null;
        socket = null;
        stream = null;
        System.err.println("Yep i got into this 3");
    serverSocket = new ServerSocket(9200);
    serverSocket.setSoTimeout(3000 /* milliseconds */);
    mPort=serverSocket.getLocalPort();
   
    do
    {
        try{socket = serverSocket.accept();
        System.err.println("Yep socket accpted");} 
        catch (final SocketTimeoutException e)
        {if (!mRunning) return;}
    } while (socket == null);
    editbkf.putString("portstring", Integer.toString(mPort));
	 editbkf.commit();
    //serverSocket.close();
    //serverSocket = null;
    stream = new DataOutputStream(socket.getOutputStream());
    stream.writeBytes(HTTP_HEADER);
    stream.flush();
    	
//		final EditText tv= (EditText) findViewById(R.id.);

        try
        {
            

            while (mRunning)
            {
                final byte[] buffer;
                final int length;
                final long timestamp;
                System.err.println("this is port number"+Integer.toString(mPort));
               
                synchronized (mBufferLock)
                {
                    while (!mNewJpeg)
                    {
                        try{mBufferLock.wait();
                        System.err.println("object locked");}
                        catch (final InterruptedException stopMayHaveBeenCalled)
                        {/* stop() may have been called*/ return;} // catch
                    } // while
                    System.err.println("object not locked iguess but proceeding");
                    mStreamingBufferA = !mStreamingBufferA;

                    if (mStreamingBufferA)
                    {
                        buffer = mBufferA;
                        length = mLengthA;
                        timestamp = mTimestampA;
                    } // if
                    else
                    {
                        buffer = mBufferB;
                        length = mLengthB;
                        timestamp = mTimestampB;
                    } // else

                    mNewJpeg = false;
                } // synchronized

                stream.writeBytes(
                    "Content-type: image/jpeg\r\n"
                    + "Content-Length: " + length + "\r\n"
                    + "X-Timestamp:" + timestamp + "\r\n"
                    + "\r\n"
                );
                stream.write(buffer, 0 /* offset */, length);
                stream.writeBytes(BOUNDARY_LINES);
                stream.flush();
                System.err.println("buffering image");
            } // while
        } // try
        finally
        {
            if (stream != null)
            {
                try{stream.close();}
                catch (final IOException closingStream)
                {System.err.println(closingStream);}
            } //strem
            if (socket != null)
            {
                try{socket.close();}
                catch (final IOException closingSocket)
                {System.err.println(closingSocket);}
            } // socket
            if (serverSocket != null)
            { try{serverSocket.close();}
              catch (final IOException closingServerSocket)
              {System.err.println(closingServerSocket);}
            } // if
        } // finally
    } // accept()


} // class MJpegHttpStreamer

