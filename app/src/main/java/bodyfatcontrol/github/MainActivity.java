package bodyfatcontrol.github;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.ChartTouchListener;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import android.Manifest;

import org.apache.commons.lang3.ArrayUtils;

import bodyfatcontrol.github.common.Constants;
import bodyfatcontrol.github.common.DataBaseUserProfile;
import bodyfatcontrol.github.common.UserProfile;

import static bodyfatcontrol.github.Utils.*;
import static bodyfatcontrol.github.common.Constants.*;

public class MainActivity extends AppCompatActivity implements OnChartValueSelectedListener,
        OnChartGestureListener, NavigationView.OnNavigationItemSelectedListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private Tracker mTracker; // for google analytics

    NavigationView navigationView;
    private CustomDrawerLayout mDrawerLayout;
    private TextView mTextViewCaloriesCalc;
    private ListView listViewLogFoodList;
    private TextView mDateTitle;
    private TextView textViewLastUpdateDate;
    public static long mMidNightToday;
    private long mGraphInitialDate;
    private long mGraphFinalDate;
    private long mLastUpdateDate = 0;
    public static final long SECONDS_24H = 24*60*60*1000;
    private double mCurrentCaloriesEER = 0.0;
    private double mCaloriesActive = 0.0;
    private double mCaloriesConsumed = 0.0;
    DataBaseLogFoods mDataBaseLogFoods = new DataBaseLogFoods(this);
    ArrayList<Object> mArrayListLogFood;
    private float x1,x2;
    static final int MIN_DISTANCE = 150;
    static boolean mIsToday = true;
    public static Context context;
    public static SharedPreferences sharedPreferences;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0;
    private BroadcastReceiver mBroadcastReceiver;
    private AlarmManager alarmMgr;
    private PendingIntent alarmIntent;
    public static final String MESSAGE_PATH = "/notification";
    private double mLastSynchDate = System.currentTimeMillis();
    private boolean mSendMessageIsEnable = false;
    private static boolean UpdateUserProfile = true;
    public static boolean permissionWriteExternalStorageEnable = false;
    public static boolean runningOnWear = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();

        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_WATCH) {
            runningOnWear = true;
        }

        setTitle("Body Fat Control");
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDrawerLayout = (CustomDrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // For google analytics
        // [START shared_tracker]
        // Obtain the shared Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();
        // [END shared_tracker]

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("onCreate")
                .build());

        // Verify and ask if needed permissions to write database on external memory
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_WRITE_EXTERNAL_STORAGE);
        } else {
            permissionWriteExternalStorageEnable = true;
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Action")
                        .setAction("LogFoodMainActivity")
                        .build());

                Intent intent = new Intent(context, LogFoodMainActivity.class);
                startActivity(intent);
            }
        });

        mDateTitle = (TextView) findViewById(R.id.date_title);
        mTextViewCaloriesCalc = (TextView) findViewById(R.id.calories_calc);
        listViewLogFoodList = (ListView) findViewById(R.id.log_food_list);
        listViewLogFoodList.setLongClickable(true);
        textViewLastUpdateDate = (TextView) findViewById(R.id.last_update_date);
        textViewLastUpdateDate.setText("syncing");

        // Button for edit or delete a food from the list
        listViewLogFoodList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
                final int position = pos;

                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Manage logged food")
                        .setMessage("You can edit or delete this food.")
                        .setPositiveButton("Edit", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(MainActivity.this, EditLoggedFoodActivity.class);
                                Foods food = (Foods) listViewLogFoodList.getItemAtPosition(position);
                                intent.putExtra("FOOD_ID", food.getId());
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Delete", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Get the food object pointed by position, get the food ID to delete it
                                Foods food = (Foods) listViewLogFoodList.getItemAtPosition(position);
                                mDataBaseLogFoods.DataBaseLogFoodsDeleteFood(food.getId());

                                onResume(); // refresh the view by calling the onResume()
                            }
                        })
                        .setNeutralButton("Cancel", null)
                        .show();

                return true;
            }
        });

        // receive the message that was sent from wear app
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte[] message = intent.getByteArrayExtra("MESSAGE");
                long command = ByteArrayToLong(ArrayUtils.subarray(message, 0, 8));
                if (command == Constants.HISTORIC_CALS_COMMAND) {
                    textViewLastUpdateDate.setText("updated");
                    mLastSynchDate = System.currentTimeMillis();

                    ArrayList<Measurement> measurementList = new ArrayList<Measurement>();

                    int measurementByteSize = longBytesNumber + intBytesNumber + doubleBytesNumber + doubleBytesNumber;
                    int messageNumberOfMeasurements = (message.length - (longBytesNumber)) / measurementByteSize;

                    int i = 8;
                    for ( ; messageNumberOfMeasurements > 0; messageNumberOfMeasurements--) {
                        Measurement measurement = new Measurement();

                        long date = ByteArrayToLong(ArrayUtils.subarray(message, i, i += 8));
                        measurement.setDate(date);

                        int HR = ByteArrayToInt(ArrayUtils.subarray(message, i, i += 4));
                        measurement.setHR(HR);

                        double calories = ByteArrayToDouble(ArrayUtils.subarray(message, i, i += 8));
                        measurement.setCalories(calories);

                        double caloriesEER = ByteArrayToDouble(ArrayUtils.subarray(message, i, i += 8));
                        measurement.setCaloriesEERPerMinute(caloriesEER);

                        measurementList.add(measurement);
                    }

                    new DataBaseCalories(context).DataBaseWriteMeasurement(measurementList);
                }

                if (UpdateUserProfile == true) {
                    UpdateUserProfile = false;
                    sendCommandUserProfile();
                }

                drawGraphs();
                drawListConsumedFoods();
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                new IntentFilter("RECEIVED_COMMAND"));

        setupTimer1Minute ();
    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Action")
                .setAction("onResume")
                .build());

        // Calc and set graph initial and final dates (midnight today and rightnow)
        Calendar rightNow = Calendar.getInstance();
        long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
        long rightNowMillis = rightNow.getTimeInMillis() + offset;
        long sinceMidnightToday = rightNowMillis % (24*60*60*1000);
        long midNightToday = rightNowMillis - sinceMidnightToday;
        mMidNightToday = midNightToday;
        mGraphInitialDate = midNightToday;
        mGraphFinalDate = rightNowMillis;

        mSendMessageIsEnable = true;

        drawGraphs();
        drawListConsumedFoods();
        sendCommandHistoricCalories ();
    }

    @Override
    public void onPause() {
        super.onPause();

        mSendMessageIsEnable = false;
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    //@SuppressWarnings("StatementWithEmptyBody")
    //@Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.ic_menu_home){

        } else if (id == R.id.ic_menu_connect){
            // Handle the connect action
            Intent intent = new Intent(this, UserProfileActivity.class);
            startActivity(intent);

            mTracker.send(new HitBuilders.EventBuilder()
                    .setCategory("Action")
                    .setAction("ConnectActivity")
                    .build());
        } else if (id == R.id.ic_menu_about){

            // Handle the connect action
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    void drawGraphs() {
        GraphData graphDataObj = new GraphData(context, mGraphInitialDate, mGraphFinalDate);
        List<Entry> graphDataCalories = graphDataObj.prepareCalories();
        mCurrentCaloriesEER = graphDataObj.getCurrentCaloriesEER();
        List<Entry> graphDataCaloriesConsumed = graphDataObj.prepareCaloriesConsumed();

        if (graphDataCalories != null && graphDataCaloriesConsumed != null) {
            final int caloriesSpentColor = ContextCompat.getColor(context, R.color.graphCaloriesSpent);
            final int caloriesSpentLineColor = ContextCompat.getColor(context, R.color.graphCaloriesSpentLine);
            final int caloriesConsumedColor = ContextCompat.getColor(context, R.color.graphCaloriesConsumed);
            final int caloriesConsumedLineColor = ContextCompat.getColor(context, R.color.graphCaloriesConsumedLine);

            mCaloriesActive = graphDataObj.getCaloriesActive();
            mCaloriesConsumed = graphDataObj.getCaloriesConsumed();

            /** Draw the various calories sizes */
            SpannableStringBuilder builder = new SpannableStringBuilder();

            int caloriesSpent = (int) (mCurrentCaloriesEER + mCaloriesActive);
            SpannableString spannableString = new SpannableString(Integer.toString((int) mCaloriesActive));
            spannableString.setSpan(new ForegroundColorSpan(caloriesSpentLineColor), 0, spannableString.length(), 0);
            builder.append(spannableString);
            builder.append(" + ");
            spannableString = new SpannableString(Integer.toString((int) mCurrentCaloriesEER));
            spannableString.setSpan(new ForegroundColorSpan(caloriesSpentLineColor), 0, spannableString.length(), 0);
            builder.append(spannableString);
            builder.append(" - ");
            spannableString = new SpannableString(String.valueOf((int) mCaloriesConsumed));
            spannableString.setSpan(new ForegroundColorSpan(caloriesConsumedLineColor), 0, spannableString.length(), 0);
            builder.append(spannableString);
            int caloriesResult = (int) (caloriesSpent - mCaloriesConsumed);
            if (caloriesResult < 0) {
                spannableString = new SpannableString(String.valueOf(caloriesResult));
                spannableString.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(context, R.color.caloriesResultValue)), 0, spannableString.length(), 0);
                builder.append(" = ");
                builder.append(spannableString);
            } else {
                builder.append(" = " + caloriesResult);
            }
            mTextViewCaloriesCalc.setText(builder, TextView.BufferType.SPANNABLE);

            // add entries to Calories Active dataset
            LineDataSet dataSetCaloriesActive = new LineDataSet(graphDataCalories, "Burned calories");
            dataSetCaloriesActive.setColor(caloriesSpentLineColor);
            dataSetCaloriesActive.setCubicIntensity(1f);
            dataSetCaloriesActive.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            dataSetCaloriesActive.setFillColor(caloriesSpentColor);
            dataSetCaloriesActive.setFillAlpha(127);
            dataSetCaloriesActive.setDrawFilled(true);
            dataSetCaloriesActive.setDrawHighlightIndicators(false);
            dataSetCaloriesActive.setHighlightLineWidth(2f);
            dataSetCaloriesActive.setDrawValues(false);
            dataSetCaloriesActive.setLineWidth(2f);
            dataSetCaloriesActive.setDrawCircles(false);

            // add entries to Calories consumed dataset
            LineDataSet dataSetCaloriesConsumed = new LineDataSet(graphDataCaloriesConsumed, "Consumed calories");
            dataSetCaloriesConsumed.setColor(caloriesConsumedLineColor);
            dataSetCaloriesConsumed.setMode(LineDataSet.Mode.LINEAR);
            dataSetCaloriesConsumed.setFillColor(caloriesConsumedColor);
            dataSetCaloriesConsumed.setFillAlpha(255);
            dataSetCaloriesConsumed.setDrawFilled(true);
            dataSetCaloriesConsumed.setHighlightEnabled(false);
            dataSetCaloriesConsumed.setDrawValues(false);
            dataSetCaloriesConsumed.setLineWidth(2f);
            dataSetCaloriesConsumed.setDrawCircles(false);

            LineData lineData = new LineData(dataSetCaloriesActive, dataSetCaloriesConsumed);

            // in this example, a LineChart is initialized from xml
            LineChart mChart = (LineChart) findViewById(R.id.chart_calories_active);

            Legend l = mChart.getLegend();
            l.setEnabled(true);

            // enable touch gestures
//            mChart.setTouchEnabled(true);
            mChart.setTouchEnabled(false);
//            mChart.setOnChartGestureListener(this);
//            mChart.setOnChartValueSelectedListener(this);

            mChart.setBackgroundColor(Color.WHITE);
            mChart.setDrawGridBackground(false);
            mChart.setDrawBorders(true);

            mChart.setDoubleTapToZoomEnabled(false);

            // enable scaling and dragging
//            chart.setDragEnabled(true);
//            chart.setScaleEnabled(true);
            mChart.setScaleXEnabled(false);
            mChart.setScaleYEnabled(false);

            XAxis xAxis = mChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextColor(Color.GRAY);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);
            xAxis.setAxisMinimum(0f);
            xAxis.setAxisMaximum(23.983f); //23h59m
            xAxis.setGranularity(0.016666667f); //1m
            xAxis.setValueFormatter(new IAxisValueFormatter() {

                private SimpleDateFormat mFormat = new SimpleDateFormat("H'h'mm");
                public String getFormattedValue(float value, AxisBase axis) {
                    if ((value % 1) == 0) {
                        mFormat = new SimpleDateFormat("H'h'");
                    } else {
                        mFormat = new SimpleDateFormat("H'h'mm");
                    }
                    return mFormat.format(new Date((long) ((value-1) *60*60*1000)));
                }
            });

            YAxis leftAxis = mChart.getAxisLeft();
            leftAxis.setEnabled(true);
            leftAxis.setGranularity(1);
            leftAxis.setAxisMinimum(0);
            leftAxis.setDrawTopYLabelEntry(true);

            // adjust max y axis value
//            if ((mCurrentCaloriesEER + mCaloriesActive) > 3000) {
//                leftAxis.resetAxisMaximum();
//            } else {
//                leftAxis.setAxisMaximum(800);
//            }

            leftAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value < (mCurrentCaloriesEER /10)) {
                        return "";
                    } else {
                        value = value - ((float) mCurrentCaloriesEER / 10);
                    }

                    return Integer.toString((int) value);
                }
            });


            YAxis rightAxis = mChart.getAxisRight();
            rightAxis.setEnabled(true);
            rightAxis.setGranularity(1);
            rightAxis.setAxisMinimum(0);
            rightAxis.setDrawTopYLabelEntry(true);

//            LimitLine ll1 = new LimitLine((float) mCurrentCaloriesEER/10, "");
//            ll1.setLineWidth(2f);
//            ll1.disableDashedLine();
//            leftAxis.addLimitLine(ll1);

            rightAxis.setValueFormatter(new IAxisValueFormatter() {
                @Override
                public String getFormattedValue(float value, AxisBase axis) {
                    if (value < (mCurrentCaloriesEER /10)) {
                        value *= 10;
                    } else {
                        value = (value - (float) (mCurrentCaloriesEER /10)) + (float) mCurrentCaloriesEER;
                    }
                    return Integer.toString((int) value);
                }
            });

            // adjust max y axis value
//            if ((mCurrentCaloriesEER + mCaloriesActive) > 3000) {
//                rightAxis.resetAxisMaximum();
//            } else {
//                rightAxis.setAxisMaximum(800);
//            }w


            // no description text
            mChart.getDescription().setEnabled(false);

            mChart.setAutoScaleMinMaxEnabled(false);
            mChart.setData(lineData);
            mChart.invalidate(); // refresh

            // Send the calories balance value (but only for today)
            if (mGraphInitialDate == mMidNightToday) { // today
                ArrayList<Integer> command = new ArrayList<>();
                command.add(Constants.CALORIES_CONSUMED_COMMAND);
                command.add((int) mCaloriesConsumed);
//                sendMessage(command);
            }
        }
    }

    void drawListConsumedFoods () {
        // Populate the listview of logged foods
        // Start by getting the data from the database and then put on the array adapter, finally to the list
        mArrayListLogFood = mDataBaseLogFoods.DataBaseLogFoodsGetFoodsAndMeals(mGraphInitialDate, mGraphFinalDate);
        ArrayAdapter<Object> arrayAdapterLogFoods = new LogFoodAdapter(this, mArrayListLogFood);
        listViewLogFoodList.setAdapter(arrayAdapterLogFoods);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        // write the selected value of Y value (calories on the point selected)

//        float date = e.getX();
//        SimpleDateFormat mFormat;
//        mFormat = new SimpleDateFormat("H'h'mm");
//        String dateString = mFormat.format(new Date((long) ((date-1) *60*60*1000)));
//
//        float value = 0;
//        if (e.getY() < (mCurrentCaloriesEER/10)) {
//            value = e.getY()*10;
//        } else {
////            value = (float) (e.getY()*10 - mCurrentCaloriesEER);
//            value = (float) (mCurrentCaloriesEER + (e.getY() - (mCurrentCaloriesEER / 10)));
////            value = value + (float) mCurrentCaloriesEER;
//        }
//
//        textViewCalories1.setText(dateString + " total calories " + Integer.toString((int) (value)));
//        textViewCalories2.setText("active calories " + Integer.toString(((int) value) - (int) (mCurrentCaloriesEER)));
    }

    @Override
    public void onChartGestureStart(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {

    }

    @Override
    public void onChartGestureEnd(MotionEvent me, ChartTouchListener.ChartGesture lastPerformedGesture) {
//        textViewCalories1.setText("total calories: " + Integer.toString((int) (mCurrentCaloriesEER + mCaloriesActive)));
//        textViewCalories2.setText("active calories: " + Integer.toString((int) mCaloriesActive));
    }

    @Override
    public void onChartLongPressed(MotionEvent me) {

    }

    @Override
    public void onChartDoubleTapped(MotionEvent me) {

    }

    @Override
    public void onChartSingleTapped(MotionEvent me) {

    }

    @Override
    public void onChartFling(MotionEvent me1, MotionEvent me2, float velocityX, float velocityY) {

    }

    @Override
    public void onChartScale(MotionEvent me, float scaleX, float scaleY) {

    }

    @Override
    public void onChartTranslate(MotionEvent me, float dX, float dY) {

    }

    @Override
    public void onNothingSelected() {}

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        switch(event.getAction())
        {
            case MotionEvent.ACTION_DOWN:
                x1 = event.getX();
                break;
            case MotionEvent.ACTION_UP:
                x2 = event.getX();
                float deltaX = x2 - x1;

                if (Math.abs(deltaX) > MIN_DISTANCE)
                {
                    // Left to Right swipe action
                    if (x2 < x1)
                    {
                        if (mIsToday == true) break;

                        mGraphInitialDate += SECONDS_24H; // seconds

                        if (mGraphInitialDate == mMidNightToday) { // today
                            mDateTitle.setText("today");

                            Calendar rightNow = Calendar.getInstance();
                            long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
                            mGraphFinalDate = rightNow.getTimeInMillis() + offset;

                            mIsToday = true;

                        } else if (mGraphInitialDate == (mMidNightToday - SECONDS_24H)) { // yesterday
                            mDateTitle.setText("yesterday");
                            mGraphFinalDate = mGraphInitialDate + SECONDS_24H - 60000; // 23h59m

                        } else { // other days
                            SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
                            String dateString = formatter.format(new Date(mGraphInitialDate));
                            mDateTitle.setText(dateString);
                            mGraphFinalDate = mGraphInitialDate + SECONDS_24H - 60000; // 23h59m
                        }

                        if (permissionWriteExternalStorageEnable == true) {
                            drawGraphs();
                            drawListConsumedFoods();
                        }
                    }

                    // Right to left swipe action
                    else
                    {
                        mIsToday = false;

                        mGraphInitialDate -= SECONDS_24H; // seconds

                        if (mGraphInitialDate == (mMidNightToday - SECONDS_24H)) { // yesterday
                            mDateTitle.setText("yesterday");
                            mGraphFinalDate = mGraphInitialDate + SECONDS_24H - 60000; // 23h59m

                        } else { // other days
                            SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
                            String dateString = formatter.format(new Date(mGraphInitialDate));
                            mDateTitle.setText(dateString);
                            mGraphFinalDate = mGraphInitialDate + SECONDS_24H - 60000; // 23h59m
                        }

                        if (permissionWriteExternalStorageEnable == true) {
                            drawGraphs();
                            drawListConsumedFoods();
                        }
                    }

                }
                else
                {
                    // consider as something else - a screen tap for example
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startAppThatDependsPermissions ();
                    permissionWriteExternalStorageEnable = true;
                } else {
                    // permission denied
                    finish();
                }
                return;
            }
        }
    }

    private void startAppThatDependsPermissions () {
        drawGraphs();
        drawListConsumedFoods();
    }

    private void setupTimer1Minute () {
        // register the action for when receiving the message TIMER_FIRED
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // schedule timer again
                long millisNextMinute = System.currentTimeMillis();
                millisNextMinute = (millisNextMinute - (millisNextMinute % 60000)) + 60000;
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, millisNextMinute , alarmIntent);

                sendCommandHistoricCalories();
                updateTextViewLastUpdate();
            }
        };
        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                new IntentFilter("TIMER_FIRED"));

        // prepare timer
        alarmMgr = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        Intent intent = new Intent(context, TimerReceiver.class);
        intent.setAction("bodyfatcontrol.github-timer");
        alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        long millisNextMinute = System.currentTimeMillis();
        millisNextMinute = (millisNextMinute - (millisNextMinute % 60000)) + 60000;
        alarmMgr.setExact(AlarmManager.RTC_WAKEUP, millisNextMinute , alarmIntent);
    }

    void sendCommandHistoricCalories () {
        // send command to ask for historic HR, calories
        byte[] byteArrayCommand = ByteBuffer.allocate(longBytesNumber).putLong(Constants.HISTORIC_CALS_COMMAND).array();

        long date = new DataBaseCalories(context).DataBaseGetLastMeasurementDate();
        // if date == 0, then database is empty
        if (date == 0) {
            // ask for values from last 48h
            date = System.currentTimeMillis();
            date = date - (date % 60000); // get date at start of a minute
            date = date - (48*60*60*1000); // go 48h backwards
        }
        date += 60000; // so we need to add 1 minute, because we want next minute value

        byte[] byteArrayDate = ByteBuffer.allocate(Long.SIZE/Byte.SIZE).putLong(date).array();
        byte[] messageBytes = ArrayUtils.addAll(byteArrayCommand, byteArrayDate);
        sendMessage(messageBytes);
    }

    void sendCommandUserProfile() {
        // send command with User Profile
        byte[] byteArrayCommand = ByteBuffer.allocate(longBytesNumber).putLong(
                Constants.USER_PROFILE_COMMAND).array();

        DataBaseUserProfile mDataBaseUserProfile = new DataBaseUserProfile(context, runningOnWear);
        UserProfile userProfile = mDataBaseUserProfile.DataBaseGetLastUserProfile();

        byte[] byteArrayBirthYear = ByteBuffer.allocate(
                intBytesNumber).putInt(userProfile.getUserBirthYear()).array();
        byte[] messageBytes = ArrayUtils.addAll(byteArrayCommand, byteArrayBirthYear);
        byte[] byteArrayGender = ByteBuffer.allocate(
                intBytesNumber).putInt(userProfile.getUserGender()).array();
        messageBytes = ArrayUtils.addAll(messageBytes, byteArrayGender);
        byte[] byteArrayHeight = ByteBuffer.allocate(
                intBytesNumber).putInt(userProfile.getUserHeight()).array();
        messageBytes = ArrayUtils.addAll(messageBytes, byteArrayHeight);
        byte[] byteArrayWeight = ByteBuffer.allocate(
                intBytesNumber).putInt(userProfile.getUserWeight()).array();
        messageBytes = ArrayUtils.addAll(messageBytes, byteArrayWeight);
        sendMessage(messageBytes);
    }

    void sendMessage(byte[] messageBytes) {
        if (mSendMessageIsEnable == true) {
            new SendMessageThread(messageBytes).start();
        }
    }

    // code from: https://github.com/JimSeker/wearable/blob/master/WearableDataLayer/wear/src/main/java/edu/cs4730/wearabledatalayer/MainActivity.java
    class SendMessageThread extends Thread {
        byte[] message;

        //constructor
        SendMessageThread(byte[] msg) {
            message = msg;
        }

        //sends the message via the thread.  this will send to all wearables connected, but
        //since there is (should only?) be one, no problem.
        public void run() {
            //first get all the nodes, ie connected wearable devices.
            Task<List<Node>> nodeListTask =
                    Wearable.getNodeClient(getApplicationContext()).getConnectedNodes();
            try {
                // Block on a task and get the result synchronously (because this is on a background
                // thread).
                List<Node> nodes = Tasks.await(nodeListTask);

                //Now send the message to each device.
                for (Node node : nodes) {
                    Task<Integer> sendMessageTask =
                            Wearable.getMessageClient(MainActivity.this).sendMessage(node.getId(), MESSAGE_PATH, message);
                }
            } catch (ExecutionException exception) {
                Log.e("SendMessageThread", "Node Task failed: " + exception);
            } catch (InterruptedException exception) {
                Log.e("SendMessageThread", "Node Interrupt occurred: " + exception);
            }
        }
    }

    void updateTextViewLastUpdate() {
        int minutesSinceLastUpdate = (int) ((System.currentTimeMillis() - mLastSynchDate) / 60000);
        if (minutesSinceLastUpdate > 0) {
            textViewLastUpdateDate.setText(Integer.toString(minutesSinceLastUpdate) + " min ago");
        } else {
            textViewLastUpdateDate.setText("syncing");
        }
    }

    public static void setUpdateUserProfile() {
        MainActivity.UpdateUserProfile = true;
    }
}

class LogFoodAdapter extends ArrayAdapter<Object> {
    public static final int TYPE_FOOD  = 0;
    public static final int TYPE_MEALTIME  = 1;

    private ArrayList<Object> mFoodsArrayList;

    public LogFoodAdapter(Context context, ArrayList<Object> foodsArrayList) {
        super(context, 0, foodsArrayList);

        this.mFoodsArrayList = foodsArrayList;
    }

    @Override
    public int getItemViewType(int position) {
        Object object = mFoodsArrayList.get(position);
        if (object instanceof Foods) return TYPE_FOOD;
        else return TYPE_MEALTIME;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        Object object = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            if (getItemViewType(position) == TYPE_FOOD) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.logged_food, parent, false);
            } else {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.logged_food_mealtime, parent, false);
            }
        }

        if (getItemViewType(position) == TYPE_FOOD) {
            // Lookup view for data population
            TextView textViewFoodName = (TextView) convertView.findViewById(R.id.food_name);
            TextView textViewFoodCaloriesLogged = (TextView) convertView.findViewById(R.id.food_calories_logged);
            // Populate the data into the template view using the data object
            // Get the data item for this position
            Foods food = (Foods) object;
            textViewFoodName.setText(food.getName());
            textViewFoodCaloriesLogged.setText(Integer.toString(food.getCaloriesLogged()));

            // remove trailing zeros of units logged
            DecimalFormat df = new DecimalFormat();
            String units = df.format(food.getUnitsLogged());
            convertView.setClickable(false);
            convertView.setFocusable(false);
        } else {
            // Lookup view for data population
            TextView textViewMealTime = (TextView) convertView.findViewById(R.id.mealtime);
            TextView textViewMealTimeCalories = (TextView) convertView.findViewById(R.id.calories);
            // Populate the data into the template view using the data object
            // Get the data item for this position
            MealTime mealTime = (MealTime) object;
            textViewMealTime.setText(mealTime.getMealTimeName());
            // remove trailing zeros of units logged
            DecimalFormat df = new DecimalFormat();
            String units = df.format(mealTime.getCalories());
            textViewMealTimeCalories.setText(units);
            convertView.setClickable(true);
            convertView.setFocusable(true);
        }

        // Return the completed view to render on screen
        return convertView;
    }
}

