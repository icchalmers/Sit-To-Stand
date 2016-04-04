package cc.chalmers.sittostand;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
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

	private Sensor mAccelerometer;

	private static SensorManager mSensorManager;

	private boolean accelerometerSensorRunning = false;

	private double yaw = 0;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mBLEMotorService = ((BLEMotorService.LocalBinder) service).getService();
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

		// Setup the sensor manager for calculating yaw (rotation about device Z).
		// If the device doesn't have acceleromter sensors the app quits.
		// It would make sense to do this MUCH earlier in the user workflow but this will have to do
		// for now.
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if(mAccelerometer != null){
			mSensorManager.registerListener(mSensorEventListener, mAccelerometer,
					SensorManager.SENSOR_DELAY_NORMAL);
			accelerometerSensorRunning = true;
		}
		else{
			Toast.makeText(this, "No ORIENTATION Sensor!", Toast.LENGTH_LONG).show();
			accelerometerSensorRunning = false;
			finish();
		}

		// Bind to the existing BLEMotor service (at least, it should already exist...)
		Intent gattServiceIntent = new Intent(this, BLEMotorService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

	}

	@Override
	protected void onDestroy() {
		JavaScriptInterface jsi = new JavaScriptInterface(this);
		jsi.disableMotors();
		unbindService(mServiceConnection);
		mBLEMotorService = null;
		if (accelerometerSensorRunning) {
			mSensorManager.unregisterListener(mSensorEventListener);
			accelerometerSensorRunning = false;
		}
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		JavaScriptInterface jsi = new JavaScriptInterface(this);
		jsi.disableMotors();
		if (accelerometerSensorRunning) {
			mSensorManager.unregisterListener(mSensorEventListener);
			accelerometerSensorRunning = false;
		}
		super.onPause();
	}

	@Override
    protected void onResume() {
		mSensorManager.registerListener(mSensorEventListener, mAccelerometer,
				SensorManager.SENSOR_DELAY_NORMAL);
		accelerometerSensorRunning = true;
        super.onResume();

    }

	private SensorEventListener mSensorEventListener = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] g;

			g = event.values.clone();
			double norm_Of_g = Math.sqrt(g[0] * g[0] + g[1] * g[1] + g[2] * g[2]);
			g[0] = g[0] / (float)norm_Of_g;
			g[1] = g[1] / (float)norm_Of_g;
			g[2] = g[2] / (float)norm_Of_g;

			yaw = Math.round(Math.toDegrees(Math.atan2(g[0], g[1])));
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
		}
	};


	public class JavaScriptInterface {
		Context mContext;

		public JavaScriptInterface(Context c) {
			mContext = c;
		}

		@JavascriptInterface
		public double getYaw() {
			return yaw;
		}

		@JavascriptInterface
		public void connectBT(){
			BLEMotorService.State currentState = mBLEMotorService.getState();
			if (currentState != BLEMotorService.State.READY) {
				Log.e(TAG, "Motor service not ready! Current state:" + currentState);
			}
		}
		
		@JavascriptInterface
		public void sendBluetooth(String motor, int value){
            Log.d(TAG, "Sending " + value + " to " + motor + " motor.");
			mBLEMotorService.writeMotor(motor, value);
		}

        @JavascriptInterface
        public void disableMotors(){
            if(mBLEMotorService != null) {
                mBLEMotorService.writeMotor("left", 0);
                mBLEMotorService.writeMotor("right", 0);
            }
        }

        @JavascriptInterface
        public void enableMotors(){
            if(mBLEMotorService != null) {
                mBLEMotorService.writeMotor("left", 50);
                mBLEMotorService.writeMotor("right", 50);
            }
        }

		@JavascriptInterface
		public void showToast(String toast) {
			Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
		}

        @JavascriptInterface
        public void addLog(String message) {
            Log.d(TAG + "JS", message);
        }

		@JavascriptInterface
		public void makeFile(String content) {
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
                Toast.makeText(mContext, "File Saved", Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				Log.e("Exception", "File write failed: " + e.toString());
                Toast.makeText(mContext, "File save failed!", Toast.LENGTH_SHORT).show();
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
