package bodyfatcontrol.github;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Calendar;

public class CreateFoodActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Create food");
        setContentView(R.layout.activity_create_food);

        final EditText editTextFoodName = (EditText) findViewById(R.id.food_name);
        final EditText editTextBrand = (EditText) findViewById(R.id.brand);
        final EditText editTextServingSizeEntry = (EditText) findViewById(R.id.serving_size_entry);
        final Spinner spinnerUnityType = (Spinner) findViewById(R.id.spinner_foods_unity_type);
        final EditText editTextCaloriesEntry = (EditText) findViewById(R.id.calories_entry);
        final Button buttonSaveCustomFood = (Button) findViewById(R.id.save_custom_food);

        buttonSaveCustomFood.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Foods food = new Foods();

                // Validate user inputs
                if (editTextFoodName.getText().toString().length() <= 0) {
                    editTextFoodName.setError("Enter food name");
                } else {
                    editTextFoodName.setError(null);

                    food.setName(editTextFoodName.getText().toString());
                    food.setBrand(editTextBrand.getText().toString());

                    if (editTextServingSizeEntry.getText().toString().length() <= 0
                            || Float.parseFloat(editTextServingSizeEntry.getText().toString()) <= 0.01) {
                        editTextServingSizeEntry.setError("Min of 0.01");
                    } else {
                        editTextServingSizeEntry.setError(null);

                        food.setUnits(Float.valueOf(editTextServingSizeEntry.getText().toString()));
                        food.setUnitType(spinnerUnityType.getSelectedItem().toString());

                        if (editTextCaloriesEntry.getText().toString().length() <= 0
                                || Float.parseFloat(editTextCaloriesEntry.getText().toString()) <= 0) {
                            editTextCaloriesEntry.setError("Min of 1");
                        } else {
                            editTextCaloriesEntry.setError(null);

                            food.setCalories(Integer.valueOf(editTextCaloriesEntry.getText().toString()));

                            Calendar rightNow = Calendar.getInstance();
                            long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
                            long rightNowMillis = rightNow.getTimeInMillis() + offset;
                            food.setDate(rightNowMillis);

                            new DataBaseFoods(getApplication().getApplicationContext()).DataBaseFoodsWriteFood(food);

                            finish(); // finish this activity
                        }
                    }
                }
            }
        });
    }
}
