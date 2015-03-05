package com.kbb08124.rehab;

import java.io.IOException;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

public class JsHandler {
	
	Context mContext;
	Activity myActivity;
	WebView webView;

	public JsHandler(Activity screen, WebView myWebView, Context c) {
		myActivity = screen;
		webView = myWebView;
		mContext = c;
	}
	
	
	
	public void writeFile(String output){
		Log.d("File", "File Write Started");
		try{
			OutputStreamWriter oSW = new OutputStreamWriter(myActivity.openFileOutput("test.txt",Context.MODE_WORLD_READABLE));
			oSW.write(output);
			Log.d("File", "File Write Completed");
			oSW.close();
		}
		catch(IOException e){
			Log.e("Exception", "File write failed: " + e.toString());
		}
	}
	
	public void showToast(String toast){
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
	}

}
