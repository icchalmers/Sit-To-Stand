package com.kbb08124.rehab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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
		myWebView.loadUrl("https://devweb2014.cis.strath.ac.uk/~kbb08124/");
		myWebView.addJavascriptInterface(new JavaScriptInterface(this), "Android");
	}
	
	
	public class JavaScriptInterface{
		Context mContext;
		
		public JavaScriptInterface(Context c) {
			mContext=c;
		}
		
		@JavascriptInterface
		public String showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            return toast;
        }
		
	}
}
