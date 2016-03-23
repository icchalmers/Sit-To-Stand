package cc.chalmers.sittostand;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
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
		myWebView.loadUrl("file:///android_asset/www/index.html");
		myWebView.addJavascriptInterface(new JavaScriptInterface(this),
				"Android");

	}

	@Override
	protected void onDestroy() {
		JavaScriptInterface jsi = new JavaScriptInterface(this);
		jsi.sendBluetooth("LOW");
		super.onDestroy();
	}

	public class JavaScriptInterface {
		private final static String address = "98:76:B6:00:35:79";
		private final UUID MY_UUID = UUID.randomUUID();

		private BluetoothSocket btSocket = null;
		private OutputStream streamOut = null;
//		private MotorService mMotorService = null;

		Context mContext;

		public JavaScriptInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void connectBT(){
//			mMotorService = new MotorService(mContext);
			showToast("startedConnection");
			boolean failed = true;
//			ArrayList<BluetoothDevice> foundMotors = mMotorService.findMotors();
//			mMotorService.findMotors();


			// This is absolutely a sloppy way of waiting for the BLE scan to finish...but my
			// Android development experience is basically zero...
			try {
				Thread.sleep(5000);
			}catch(InterruptedException e) {}
			//mMotorService.

			if(failed){
				showToast("Connection failed");
			}
			else{
				showToast("Connection successful");
			}
		}
		
		@JavascriptInterface
		public void sendBluetooth(String value){
//			if(mMotorService == null){
//				connectBT();
//			}
//			Log.i("BLEScan", "Trying to write data...");
//			byte dataToSend[] = new byte[] {(byte)50};
//			mMotorService.setMotor("right", dataToSend);

			/*
			try{
				streamOut = btSocket.getOutputStream();
			} catch(IOException e){
				Log.d("Sending","Output stream failed");
			}
			byte[] message = value.getBytes();
			
			try{
				streamOut.write(message);
			} catch(IOException e){
				Log.d("Sending","Sending failed");
			}
			*/
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
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(sdCard.getAbsolutePath()
						+ "/ProjectLogFiles");
				dir.mkdirs();
				File file = new File(dir, filename + ".txt");
				fOS = new FileOutputStream(file);
				fOS.write(content.getBytes());
			} catch (IOException e) {
				Log.e("Exception", "File write failed: " + e.toString());
			} finally {
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
