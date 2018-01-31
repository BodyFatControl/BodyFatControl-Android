package bodyfatcontrol.github;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.Calendar;

import static bodyfatcontrol.github.MainActivity.context;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Create food");
        setContentView(R.layout.activity_user_profile);

        DataBaseUserProfile mDataBaseUserProfile = new DataBaseUserProfile(context);
        UserProfile mUserProfile = mDataBaseUserProfile.DataBaseUserProfileLast();
        if (mUserProfile == null) { // in the case there is no data on the database
            mUserProfile = new UserProfile();
            mUserProfile.setDate(System.currentTimeMillis());
            mUserProfile.setUserName("");
            mUserProfile.setUserBirthYear(1980);
            mUserProfile.setUserGender(0);
            mUserProfile.setUserHeight(175);
            mUserProfile.setUserWeight(85);
            mUserProfile.setUserActivityClass(0);
        }

        final EditText editTextUserName = (EditText) findViewById(R.id.user_name_entry);
        final Spinner spinnerGender = (Spinner) findViewById(R.id.spinner_gender);
        final EditText editTextBirthYear = (EditText) findViewById(R.id.birth_year_entry);
        final EditText editTextHeight = (EditText) findViewById(R.id.height_entry);
        final EditText editTextWeight = (EditText) findViewById(R.id.weight_entry);
        final Button buttonSave = (Button) findViewById(R.id.button_save);

        editTextUserName.setText(mUserProfile.getUserName());
        if (mUserProfile.getUserGender() == 0) {
            spinnerGender.setSelection(0);
        } else {
            spinnerGender.setSelection(1);
        }
        editTextBirthYear.setText(String.valueOf(mUserProfile.getUserBirthYear()));
        editTextHeight.setText(String.valueOf(((float) mUserProfile.getUserHeight())/100)); // in cm
        editTextWeight.setText(String.valueOf(mUserProfile.getUserWeight())); // in kgs

        buttonSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DataBaseUserProfile mDataBaseUserProfile = new DataBaseUserProfile(context);
                UserProfile mUserProfile = new UserProfile();

                // Validate user inputs
                if (editTextUserName.getText().toString().length() <= 0) {
                    editTextUserName.setError("Enter user name");
                } else {
                    editTextUserName.setError(null);

                    if (editTextBirthYear.getText().toString().length() <= 3) {
                        editTextBirthYear.setError("Set year, example: 1980");
                    } else {
                        editTextBirthYear.setError(null);

                        if (editTextHeight.getText().toString().length() <= 0
                                || Float.parseFloat(editTextHeight.getText().toString()) < 1.0) {
                            editTextHeight.setError("Min of 1 meter, example: 1.00");
                        } else {
                            editTextHeight.setError(null);

                            if (editTextWeight.getText().toString().length() <= 1) {
                                editTextWeight.setError("Min of 10 kg, example: 85");
                            } else {
                                editTextWeight.setError(null);

                                Calendar rightNow = Calendar.getInstance();
                                long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
                                long rightNowMillis = rightNow.getTimeInMillis() + offset;
                                mUserProfile.setDate(rightNowMillis);
                                mUserProfile.setUserName(editTextUserName.getText().toString());
                                mUserProfile.setUserGender(spinnerGender.getSelectedItemPosition());
                                mUserProfile.setUserBirthYear(
                                        (Integer.valueOf(editTextBirthYear.getText().toString())));
                                mUserProfile.setUserHeight(Math.round(
                                        Float.valueOf(editTextHeight.getText().toString()) * 100));
                                mUserProfile.setUserWeight(
                                        (Integer.valueOf(editTextWeight.getText().toString())));

                                mDataBaseUserProfile.DataBaseUserProfileWrite(mUserProfile);

                                finish(); // finish this activity
                            }
                        }
                    }
                }
            }
        });
    }
}
