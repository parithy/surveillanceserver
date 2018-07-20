package com.nec.serverunit;



import com.nec.serverunit.hosting.StreamCameraActivity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private static final String TAG = "Server Unit";
	private Button launchButton;
	SharedPreferences SP;
	EditText et;
	 
    
		/** Called when the activity is first created. */
	    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pim);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		et = (EditText) findViewById(R.id.editText1);
		SP = getSharedPreferences("spfile",0);
		
		
	    }	
        
	        
	    @Override
	    protected void onResume() {
		super.onResume();	
		if(SP.getString("Phonenumber Cache", "").isEmpty())
			et.setHint("Enter client's phonenumber here");
		else	
		et.setText(SP.getString("Phonenumber Cache", ""));
		
       launchButton = (Button) findViewById(R.id.launch_button);
        
        launchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Editor edit = SP.edit();
				edit.putString("Phonenumber Cache", et.getText().toString());
				edit.commit();
				Log.i(TAG, "Starting StreamCameraActivity");
		      	
				
				new CountDownTimer(5000,1000){
					@Override
					public void onFinish() {
					   et.setText("");
						Intent i = new Intent(MainActivity.this,StreamCameraActivity.class);
					startActivity(i);}
					@Override
					public void onTick(long arg0) {et.setText("Starting in " + Long.toString(arg0/1000) + " seconds");}
					}.start();
                  	
				}
			}
		);
	    



}}
