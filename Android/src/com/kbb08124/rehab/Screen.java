package com.kbb08124.rehab;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class Screen extends Activity {
	
	private WebView myWebView;

	@SuppressLint({ "SetJavaScriptEnabled", "JavascriptInterface" })
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_screen);
		myWebView = (WebView) findViewById(R.id.webView);
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		myWebView.loadUrl("https://devweb2014.cis.strath.ac.uk/~kbb08124/");
		myWebView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
	}
	
	
	public class JavaScriptInterface{
		Context mContext;
		
		public JavaScriptInterface(Context c) {
			mContext=c;
		}
		
		@JavascriptInterface
		public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
		
		@JavascriptInterface
		public void makeFile(String content) {
            Toast.makeText(mContext, "File Sent", Toast.LENGTH_SHORT).show();
            String filename = new Date().toString();
            FileOutputStream fOS = null;
            try {
            	String path = mContext.getExternalFilesDir(null).getAbsolutePath();
            	File file = new File(path + "/" + filename +".txt");
            	fOS = new FileOutputStream(file);
            	fOS.write(content.getBytes());
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
            finally{
            	try {
					fOS.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
		
		public boolean isExternalStorageWritable() {
		    String state = Environment.getExternalStorageState();
		    if (Environment.MEDIA_MOUNTED.equals(state)) {
		        return true;
		    }
		    return false;
		}
		
	}
}
