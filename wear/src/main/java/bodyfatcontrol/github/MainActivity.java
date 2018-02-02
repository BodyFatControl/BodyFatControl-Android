package bodyfatcontrol.github;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.apache.commons.lang3.ArrayUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;

import bodyfatcontrol.github.common.Constants;
import bodyfatcontrol.github.common.DataBaseUserProfile;
import bodyfatcontrol.github.common.UserProfile;

import static bodyfatcontrol.github.Utils.*;
import static bodyfatcontrol.github.common.Constants.*;

public final class MainActivity extends WearableActivity implements
        MessageClient.OnMessageReceivedListener,
        CapabilityClient.OnCapabilityChangedListener {

    public static Context context;
    private UserProfile mUserProfile;
    private DataBaseUserProfile mDataBaseUserProfile;

    SensorHR sensorHR;
    private BroadcastReceiver mBroadcastReceiverHR;
    private BroadcastReceiver mBroadcastReceiverReceivedCommands;

    public static final long SECONDS_24H = 24*60*60;

    private static final String MOBILE_APP_CAPABILITY_NAME = "capability_mobile_app";
    public static final String MESSAGE_PATH = "/message_path";
    private String mMobileAppNodeId = null;

    private Calories mCalories;
    private DataBaseCalories mDataBaseCalories;

    public static long currentMinute;

    private boolean runningOnWear = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Toast.makeText(this, "Starting background app", Toast.LENGTH_LONG).show();

        context = getApplicationContext();

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_WATCH) {
            runningOnWear = true;
        }

        long date = System.currentTimeMillis();
        currentMinute = date - (date % 60000); // get date at start of a minute

        mDataBaseUserProfile = new DataBaseUserProfile(context, runningOnWear);
        mUserProfile = mDataBaseUserProfile.DataBaseGetLastUserProfile();

        mCalories = new Calories(mUserProfile);
        mDataBaseCalories = new DataBaseCalories();

        // will setup and detect if the app is installed and the wear is connected to Android Wear mobile app
        setupDetectMobileApp();

        // start HR sensor
        sensorHR = new SensorHR(context);

        // receive the value of HR sensor
        mBroadcastReceiverHR = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int HRValue = intent.getIntExtra("HR_VALUE", 0);

                mCalories.StoreCalories(currentMinute, HRValue, mUserProfile);

                // every hour
                if ((currentMinute % (60*60*1000)) == 0) {
                    manageDataBasesSizes();
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiverHR,
                new IntentFilter("HR_VALUE"));

        // receive the data from received commands
        mBroadcastReceiverReceivedCommands = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] message = intent.getByteArrayExtra("MESSAGE");

                int index = 0;

                long command = ByteArrayToLong(ArrayUtils.subarray(message, index, (index += longBytesNumber)));
                if (command == Constants.HISTORIC_CALS_COMMAND) {
                    long date = ByteArrayToLong(ArrayUtils.subarray(message, index, (index += longBytesNumber)));
                    long finalDate = MainActivity.currentMinute - 60000; // get date in minutes (previous minute)

                    // A byte[] of data, which Google recommends be no larger than 100KB in size
                    // each measure = 8bytes * 4 = 32 bytes
                    // 24h = 46080 bytes, limit interval to be no more than 24h
                    long maxRange = 24*60*60*1000;
                    if ((finalDate - date) > maxRange) {
                        finalDate = date + maxRange;
                    }

                    if (date > finalDate) {
                        // date must be previous, otherway just skip
                        return;
                    }

                    // send result for HISTORIC_CALS_COMMAND
                    ArrayList<Measurement> measurementList = mDataBaseCalories.DataBaseGetMeasurements(date, finalDate);

                    byte[] messageBytes = ByteBuffer.allocate(longBytesNumber).putLong(Constants.HISTORIC_CALS_COMMAND).array();

                    for (Measurement measurement : measurementList) {

                        long date_measurement = measurement.getDate();
                        int HR = measurement.getHR();
                        double caloriesPerMinute = measurement.getCaloriesPerMinute();
                        double caloriesEERPerMinute = measurement.getCaloriesEERPerMinute();

                        messageBytes = ArrayUtils.addAll(messageBytes, LongToByteArray(date_measurement));
                        messageBytes = ArrayUtils.addAll(messageBytes, IntToByteArray(HR));
                        messageBytes = ArrayUtils.addAll(messageBytes, DoubleToByteArray(caloriesPerMinute));
                        messageBytes = ArrayUtils.addAll(messageBytes, DoubleToByteArray(caloriesEERPerMinute));
                    }

                    // send the message to the mobile app
                    if (mMobileAppNodeId != null) {
                        Task<Integer> sendTask = Wearable.getMessageClient(MainActivity.this).sendMessage(
                                mMobileAppNodeId, MESSAGE_PATH, messageBytes);
                    }

                } else if (command == Constants.USER_PROFILE_COMMAND) {
                    // write the received UserProfile to the database
                    // also the global static mUserProfile is updated
                    Calendar rightNow = Calendar.getInstance();
                    long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
                    long rightNowMillis = rightNow.getTimeInMillis() + offset;
                    mUserProfile.setDate(rightNowMillis);
                    mUserProfile.setUserBirthYear(
                            ByteArrayToInt(ArrayUtils.subarray(
                                    message, index, (index += intBytesNumber))));
                    mUserProfile.setUserGender(
                            ByteArrayToInt(ArrayUtils.subarray(
                                    message, index, (index += intBytesNumber))));
                    mUserProfile.setUserHeight(
                            ByteArrayToInt(ArrayUtils.subarray(
                                    message, index, (index += intBytesNumber))));
                    mUserProfile.setUserWeight(
                            ByteArrayToInt(ArrayUtils.subarray(
                                    message, index, (index += intBytesNumber))));

                    mDataBaseUserProfile.DataBaseUserProfileWrite(mUserProfile);
                    mCalories = new Calories(mUserProfile); // this is needed to update caloriesEERPerMinute
                }
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiverReceivedCommands,
                new IntentFilter("RECEIVED_COMMAND"));

        manageDataBasesSizes();
        mDataBaseCalories.DataBaseFillEmptyMeasurements ();

        // Enables Always-on
        setAmbientEnabled();

        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wearable.getMessageClient(this).addListener(this);
        Wearable.getCapabilityClient(this)
                .addListener(this, MOBILE_APP_CAPABILITY_NAME);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Wearable.getMessageClient(this).removeListener(this);
        Wearable.getCapabilityClient(this).removeListener(this);
    }


    @Override
    protected void onStop()
    {
        super.onStop();
        sensorHR.stopHRSensor(); // stop HR sensor
    }

    public Context getContext() {
        return context;
    }

    public void setupDetectMobileApp() {
        Task<Map<String, CapabilityInfo>> capabilitiesTask =
                Wearable.getCapabilityClient(this)
                        .getAllCapabilities(CapabilityClient.FILTER_REACHABLE);

        capabilitiesTask.addOnSuccessListener(new OnSuccessListener<Map<String, CapabilityInfo>>() {
            @Override
            public void onSuccess(Map<String, CapabilityInfo> capabilityInfoMap) {
                if (capabilityInfoMap.isEmpty()) {
                    return;
                }

                CapabilityInfo capabilityInfo = capabilityInfoMap.get(MOBILE_APP_CAPABILITY_NAME);
                if (capabilityInfo != null) {
                    Set<Node> connectedNodes = capabilityInfo.getNodes();

                    // Find a nearby node
                    mMobileAppNodeId = null;
                    for (Node node : connectedNodes) {
                        if (node.isNearby()) {
                            mMobileAppNodeId = node.getId();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onCapabilityChanged(@NonNull CapabilityInfo capabilityInfo) {
        if (capabilityInfo != null) {
            Set<Node> connectedNodes = capabilityInfo.getNodes();

            // Find a nearby node
            mMobileAppNodeId = null;
            for (Node node : connectedNodes) {
                if (node.isNearby()) {
                    mMobileAppNodeId = node.getId();
                }
            }
        }
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {

    }

    private void manageDataBasesSizes () {
        long date = currentMinute - (48*60*60*1000); // go 48h backwards
        mDataBaseCalories.DataBaseCleanBeforeDate(date);
    }
}


