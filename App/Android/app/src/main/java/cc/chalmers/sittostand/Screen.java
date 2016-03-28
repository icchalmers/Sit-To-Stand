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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class Screen extends Activity {

	private WebView myWebView;
	private BLEMotorService mBLEMotorService;
	private final String TAG = "Screen";

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBLEMotorService = ((BLEMotorService.LocalBinder) service).getService();
//			if (!mBLEMotorService.initialize()) {
//				Log.e(TAG, "Unable to initialize Bluetooth");
//				finish();
//			}
//			// Automatically connects to the device upon successful start-up initialization.
//			mBLEMotorService.connect(mDeviceLeftAddress, mDeviceRightAddress);
			Log.d(TAG, "Bound to BLEMotorService in Screen");
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBLEMotorService = null;
		}
	};

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
		Intent gattServiceIntent = new Intent(this, BLEMotorService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

	}

	@Override
	protected void onDestroy() {
		JavaScriptInterface jsi = new JavaScriptInterface(this);
		jsi.sendBluetooth("LOW");
		super.onDestroy();
	}

	public class JavaScriptInterface {
//		private final static String address = "98:76:B6:00:35:79";
//		private final UUID MY_UUID = UUID.randomUUID();
//
//		private BluetoothSocket btSocket = null;
//		private OutputStream streamOut = null;
//		private MotorService mMotorService = null;

		Context mContext;

		public JavaScriptInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public void connectBT(){
			BLEMotorService.State currentState = mBLEMotorService.getState();
			if (currentState != BLEMotorService.State.READY) {
				Log.e(TAG, "Motor service not ready! Current state:" + currentState);
			}
		}
		
		@JavascriptInterface
		public void sendBluetooth(String value){
			//TODO
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
