package bodyfatcontrol.github;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
//import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import java.util.Calendar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditLoggedFoodActivity extends AppCompatActivity {
    private Foods mFood;
    private EditText mEditTextServingSizeEntry = null;
    private TextView mTextViewCalories = null;
    private TextView mTextViewMealTime = null;
    private EditText mEditTextDate = null;
    private EditText mEditTextTime = null;
    private Button mButtonLogThis = null;
    Calendar mCalendarDate = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Edit logged food");
        setContentView(R.layout.activity_log_food);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final TextView textViewFoodName = (TextView) findViewById(R.id.food_name);
        final TextView textViewBrand = (TextView) findViewById(R.id.brand);
        final TextView textViewFoodUnityType = (TextView) findViewById(R.id.food_unity_type);
        mTextViewCalories = (TextView) findViewById(R.id.calories);
        mTextViewMealTime = (TextView) findViewById(R.id.meal_time);
        mEditTextDate = (EditText) findViewById(R.id.date);
        mEditTextTime = (EditText) findViewById(R.id.time);
        mEditTextServingSizeEntry = (EditText) findViewById(R.id.serving_size_entry);
        mButtonLogThis = (Button) findViewById(R.id.button_log_this);

        // Get the food from the database and populate the view fields with the food data
        Bundle extras = getIntent().getExtras();
        DataBaseLogFoods dataBaseLogFoods = new DataBaseLogFoods(this);
        mFood = dataBaseLogFoods.DataBaseLogFoodsGetFood(extras.getInt("FOOD_ID"));

        mCalendarDate.setTimeInMillis(mFood.getDate()); // save the food date

        textViewFoodName.setText(mFood.getName());
        textViewBrand.setText(mFood.getBrand());
        mEditTextServingSizeEntry.setText(Float.toString(mFood.getUnitsLogged()));
        textViewFoodUnityType.setText(mFood.getUnitType());
        mTextViewCalories.setText(Integer.toString(mFood.getCaloriesLogged()));

        mTextViewMealTime.setText(mFood.getMealTime());

        mEditTextDate.setText(mCalendarDate.get(Calendar.DAY_OF_MONTH) + "/" +
                (mCalendarDate.get(Calendar.MONTH)+1)  + "/" +
                mCalendarDate.get(Calendar.YEAR));

        mEditTextTime.setText(mCalendarDate.get(Calendar.HOUR_OF_DAY) + "h" +
                (mCalendarDate.get(Calendar.MINUTE)));

        mEditTextServingSizeEntry.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String string = s.toString();
                if (!string.isEmpty()) {
                    float value = Float.valueOf(s.toString());
                    float calories = mFood.getCalories();
                    float unity = mFood.getUnits();
                    value = (1/unity) * calories * value;
                    mTextViewCalories.setText(Integer.toString((int) value));
                } else {
                    mTextViewCalories.setText("");
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        mEditTextDate.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus){
                    DatePickerDialog dialog = new DatePickerDialog(EditLoggedFoodActivity.this,
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
                    mTimePicker = new TimePickerDialog(EditLoggedFoodActivity.this, new TimePickerDialog.OnTimeSetListener() {
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

        // Action for log this food
        // Get all the data of this food to save on the database, etc
        mButtonLogThis.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                // Validate user inputs
                if (mEditTextServingSizeEntry.getText().toString().length() <= 0
                        || Float.parseFloat(mEditTextServingSizeEntry.getText().toString()) <= 0.01) {
                    mEditTextServingSizeEntry.setError("Min of 0.01");
                } else {
                    mEditTextServingSizeEntry.setError(null);

                    mFood.setUnitsLogged(Float.valueOf(mEditTextServingSizeEntry.getText().toString()));

                    mFood.setCaloriesLogged(Integer.parseInt(mTextViewCalories.getText().toString()));

                    mFood.setMealTime(mTextViewMealTime.getText().toString());

                    long date = 0;
                    String givenDateString = mEditTextDate.getText().toString() + " " + mEditTextTime.getText().toString();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH'h'mm");
                    try {
                        Date mDate = sdf.parse(givenDateString);
                        date = mDate.getTime();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    mFood.setDate(date);

                    // overwrite the exiting food with this new one
                    new DataBaseLogFoods(getApplication().getApplicationContext()).DataBaseLogFoodsWriteFood(mFood, true);

                    //                // update stats
                    //                originalFood.setLastUsageDate(mCalendarDate.getTimeInMillis());
                    //                originalFood.setUsageFrequency(originalFood.getUsageFrequency() + 1);
                    //                dataBaseFoods.DataBaseFoodsWriteFood(originalFood);

                    finish(); // finish this activity
                }
            }
        });
    }
}

