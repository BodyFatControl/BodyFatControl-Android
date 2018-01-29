package bodyfatcontrol.github;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class LogCaloriesFoodActivity extends AppCompatActivity {
    private EditText mEditTextServingSizeEntry = null;
    private TextView mEditTextCalories = null;
    private TextView mTextViewMealTime = null;
    private EditText mEditTextDate = null;
    private EditText mEditTextTime = null;
    private Button mButtonLogThis = null;
    java.util.Calendar mCalendarDate = java.util.Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTitle("Log quick calories");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_calories_food);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final EditText editTextFoodName = (EditText) findViewById(R.id.food_name);
        final EditText editTextBrand = (EditText) findViewById(R.id.brand);
        final Spinner spinnerUnityType = (Spinner) findViewById(R.id.spinner_foods_unity_type);
        mEditTextCalories = (EditText) findViewById(R.id.calories);
        mTextViewMealTime = (TextView) findViewById(R.id.meal_time);
        mEditTextDate = (EditText) findViewById(R.id.date);
        mEditTextTime = (EditText) findViewById(R.id.time);
        mEditTextServingSizeEntry = (EditText) findViewById(R.id.serving_size_entry);
        mButtonLogThis = (Button) findViewById(R.id.button_log_this);

        int hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        mTextViewMealTime.setText(Utils.returnMealTime(hourOfDay));

        // Set a custom spinner default value
        ArrayAdapter myAdap = (ArrayAdapter) spinnerUnityType.getAdapter(); //cast to an ArrayAdapter
        int spinnerPosition = myAdap.getPosition("piece");
        spinnerUnityType.setSelection(spinnerPosition);

        mEditTextDate.setText(mCalendarDate.get(Calendar.DAY_OF_MONTH) + "/" +
                (mCalendarDate.get(Calendar.MONTH)+1)  + "/" +
                mCalendarDate.get(Calendar.YEAR));

        mEditTextTime.setText(mCalendarDate.get(Calendar.HOUR_OF_DAY) + "h" +
                (mCalendarDate.get(Calendar.MINUTE)));

        mEditTextDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    DatePickerDialog dialog = new DatePickerDialog(LogCaloriesFoodActivity.this,
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year,
                                                      int monthOfYear, int dayOfMonth) {

                                    mCalendarDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    mCalendarDate.set(Calendar.MONTH, monthOfYear);
                                    mCalendarDate.set(Calendar.YEAR, year);
                                    mCalendarDate.set(Calendar.SECOND, 0);
                                    mCalendarDate.set(Calendar.MILLISECOND, 0);

                                    mEditTextDate.setText(mCalendarDate.get(Calendar.DAY_OF_MONTH) + "/" +
                                            (mCalendarDate.get(Calendar.MONTH)+1) + "/" +
                                            mCalendarDate.get(Calendar.YEAR));
                                }
                            },
                            mCalendarDate.get(Calendar.YEAR),
                            mCalendarDate.get(Calendar.MONTH),
                            mCalendarDate.get(Calendar.DAY_OF_MONTH));
                    dialog.show();
                }else {

                }
            }
        });

        mEditTextTime.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    TimePickerDialog mTimePicker;
                    mTimePicker = new TimePickerDialog(LogCaloriesFoodActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                            mCalendarDate.set(Calendar.HOUR_OF_DAY, selectedHour);
                            mCalendarDate.set(Calendar.MINUTE, selectedMinute);
                            mCalendarDate.set(Calendar.SECOND, 0);
                            mCalendarDate.set(Calendar.MILLISECOND, 0);

                            mEditTextTime.setText(mCalendarDate.get(Calendar.HOUR_OF_DAY) + "h" +
                                    (mCalendarDate.get(Calendar.MINUTE)));

                            mTextViewMealTime.setText(Utils.returnMealTime(mCalendarDate.get(Calendar.HOUR_OF_DAY)));
                        }
                    }, mCalendarDate.get(Calendar.HOUR_OF_DAY), mCalendarDate.get(Calendar.MINUTE), true);//Yes 24 hour time
                    mTimePicker.setTitle("Select Time");
                    mTimePicker.show();
                } else {

                }
            }
        });

        mButtonLogThis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Foods food = new Foods();

                if (editTextFoodName.getText().toString().length() <= 0) {
                    editTextFoodName.setError("Enter food name");
                } else {
                    editTextFoodName.setError(null);

                    food.setName(editTextFoodName.getText().toString());
                    food.setBrand(editTextBrand.getText().toString());

                    // Validate user inputs
                    if (mEditTextServingSizeEntry.getText().toString().length() <= 0
                            || Float.parseFloat(mEditTextServingSizeEntry.getText().toString()) <= 0.01) {
                        mEditTextServingSizeEntry.setError("Min of 0.01");
                    } else {
                        mEditTextServingSizeEntry.setError(null);
                        food.setUnits(Float.valueOf(mEditTextServingSizeEntry.getText().toString()));
                        food.setUnitType(spinnerUnityType.getSelectedItem().toString());

                        if (mEditTextCalories.getText().toString().length() <= 0
                                || Float.parseFloat(mEditTextCalories.getText().toString()) <= 0) {
                            mEditTextCalories.setError("Min of 1");
                        } else {
                            mEditTextCalories.setError(null);
                            food.setCaloriesLogged(Integer.parseInt(mEditTextCalories.getText().toString()));

                            food.setMealTime(mTextViewMealTime.getText().toString());

                            long date = 0;
                            String givenDateString = mEditTextDate.getText().toString() + " " + mEditTextTime.getText().toString();
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH'h'mm");
                            try {
                                Date mDate = sdf.parse(givenDateString);
                                date = mDate.getTime();
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            java.util.Calendar rightNow = java.util.Calendar.getInstance();
                            long offset = rightNow.get(java.util.Calendar.DST_OFFSET);
                            food.setDate(date + offset);

                            food.setIsCustomCalories(true);

                            new DataBaseLogFoods(getApplication().getApplicationContext()).DataBaseLogFoodsWriteFood(food, false);

                            finish(); // finish this activity
                        }
                    }
                }
            }
        });
    }
}

