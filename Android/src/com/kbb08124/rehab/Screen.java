package com.kbb08124.rehab;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Screen extends Activity {

	@SuppressLint("SetJavaScriptEnabled")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_screen);
		WebView myWebView = (WebView) findViewById(R.id.webView);
		myWebView.loadUrl("http://www.example.com");
		WebSettings webSettings = myWebView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		myWebView.addJavascriptInterface(
				new WebAppInterface(this.getApplicationContext()), "Android");

		myWebView.setWebViewClient(new WebViewClient());
	}
}
