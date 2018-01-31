package bodyfatcontrol.github;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static android.content.Context.SENSOR_SERVICE;

public class SensorHR implements SensorEventListener {

    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private BroadcastReceiver mBroadcastReceiver;

    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;

    private boolean mHRSensorEnable = false;

    private int mLastHRValue = -1;
    private boolean mFirstHRMeasurement = true;
    private int mHRValue = -1;

    public SensorHR (Context context) {
        mContext = context;
        mSensorManager = ((SensorManager) context.getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        // register the action for whe receiving the message TIMER_FIRED
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setupHRMeasurement();
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                new IntentFilter("TIMER_FIRED"));

        // prepare timer for HR measurement
        alarmMgr = (AlarmManager)mContext.getSystemService(mContext.ALARM_SERVICE);
        Intent intent = new Intent(mContext, MyReceiver.class);
        intent.setAction("bodyfatcontrol.github-timer");
        alarmIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
        setupHRMeasurement();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
            mLastHRValue = (int) event.values[0];

            if ((mFirstHRMeasurement == true) && (mLastHRValue > 0) ||
                (mFirstHRMeasurement == false)) {
                mFirstHRMeasurement = true;
                setupHRMeasurement (); // setup next HR measurement
            } else if (mFirstHRMeasurement == true) {
                mFirstHRMeasurement = false;
            }
        }
    }

    public void startHRSensor () {
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void stopHRSensor () {
        mSensorManager.unregisterListener(this);
//        alarmMgr.cancel(alarmIntent); // stop alarm
    }

    public void setupHRMeasurement () {
        if (mHRSensorEnable == false) {
            startHRSensor ();
            mLastHRValue = 0;
            mHRSensorEnable = true;
            // fire timer after 20 seconds, to disable the HR sensor
            // a correct measure may take about 12 seconds (tested on Polar M600)
            // as soon the first measure happens, the sensor will be turned off so save battery
            // the very first measure when sensor is turned on, has value 0.0
            // the next measure will be value > 0.0 if the sensor can make a measure and the first
            // value seems to be a correct one when comparing to next values, so, we can consider
            // this value as a good one.
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() +
                    (1000 * 20), alarmIntent);
        } else {
            stopHRSensor ();
            mHRValue = mLastHRValue;
            mHRSensorEnable = false;
            // fire timer on next minute, to enable the HR sensor
            long millisNextMinute = System.currentTimeMillis();
            millisNextMinute = (millisNextMinute - (millisNextMinute % 60000)) + 60000;
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, millisNextMinute , alarmIntent);

            // broadcast the HR value to MainActivity
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                    new Intent("HR_VALUE").putExtra(
                            "HR_VALUE", mHRValue));
        }
    }

    public float getHRValue () {
        return mHRValue;
    }
}
