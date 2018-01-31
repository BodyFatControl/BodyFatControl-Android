package bodyfatcontrol.github;
;
import java.util.Calendar;

public class Calories {
    UserProfile mUserProfile;
    DataBaseCalories mDataBaseCalories;
    Measurement mMeasurement;
    static public double caloriesEERPerMinute;

    Calories() {
        mUserProfile = MainActivity.userProfile;
        mDataBaseCalories = new DataBaseCalories();
        mMeasurement = new Measurement();
        caloriesEERPerMinute = calcDailyCaloriesEER() / (24*60);
    }

    public void StoreCalories(long date, int HR)
    {
        mMeasurement.setDate(date);
        mMeasurement.setHR(HR);
        mMeasurement.setCaloriesPerMinute(calcCalories(HR));
        mMeasurement.setCaloriesEERPerMinute(caloriesEERPerMinute);
        mDataBaseCalories.DataBaseWriteMeasurement(mMeasurement);
    }

    /*
    EER:
    Your EER (Estimated Energy Requirements) are the number of estimated  calories that you burn
    based on your BMR plus calories from a typical  non-exercise day, such as getting ready for
    work, working at a desk job  for 8 hours, and stopping by the store on the way home. EER is
    based on a  formula published by the FDA and used by other government agencies to  estimate the
    calories required by an individual based on their age,  height, weight, and gender. Your EER is
    greater than your BMR since your  BMR only takes into account the calories burned by your body
    just for  it to exist.
    MALE: EER = 864 - 9.72 x age(years) + 1.0 x (14.2 x weight(kg) + 503 x height(meters))
    FEMALE: EER = 387 - 7.31 x age(years) + 1.0 x (10.9 x weight(kg) + 660.7 x height(meters))

    Calories over and including 90 HR:
    This is the Formula when you don't know the VO2max (Maximal oxygen consumption):
    Male:((-55.0969 + (0.6309 x HR) + (0.1988 x W) + (0.2017 x A))/4.184) x 60 x T
    Female:((-20.4022 + (0.4472 x HR) - (0.1263 x W) + (0.074 x A))/4.184) x 60 x T
    HR = Heart rate (in beats/minute)
    W = Weight (in kilograms)
    A = Age (in years)
    T = Exercise duration time (in hours)

    With VO2max known you can calculate the calories burned like this:
﻿   Male:((-95.7735 + (0.634 x HR) + (0.404 x VO2max) + (0.394 x W) + (0.271 x A))/4.184) x 60 x T
    Female:((-59.3954 + (0.45 x HR) + (0.380 x VO2max) + (0.103 x W) + (0.274 x A))/4.184) x 60 x T
    */

    public double calcDailyCaloriesEER() {
        int birthYear = mUserProfile.getUserBirthYear();
        int age = (Calendar.getInstance().get(Calendar.YEAR)) - birthYear;
        int gender = mUserProfile.getUserGender();
        double height = (double) mUserProfile.getUserHeight();
        double weight = (double) mUserProfile.getUserWeight();
        double calories;

        if (gender == 0) { // female
            calories = ((387 - (7.31*age) + (1.0*(10.9*weight)) + (660.7*height/100))); // daily value
        } else { // male
            calories = ((864 - (9.72*age) + (1.0*(14.2*weight)) + (503*height/100)));
        }

        return calories;
    }

    public double calcDailyCaloriesEER(long initialDate, long finalDate) {
        int birthYear = mUserProfile.getUserBirthYear();
        int age = (Calendar.getInstance().get(Calendar.YEAR)) - birthYear;
        int gender = mUserProfile.getUserGender();
        double height = (double) mUserProfile.getUserHeight();
        double weight = (double) mUserProfile.getUserWeight();

        double calories;
        if (gender == 0) { // female
            calories = ((387 - (7.31*age) + (1.0*(10.9*weight)) +
                    (660.7*height/100))); // daily value
            double temp = (finalDate - initialDate);
            temp = temp / MainActivity.SECONDS_24H;
            calories = calories * temp;

        } else { // male
            calories = ((864 - (9.72*age) + (1.0*(14.2*weight)) +
                    (503*height/100)));
            double temp = (finalDate - initialDate);
            temp = temp / MainActivity.SECONDS_24H;
            calories = calories * temp;
        }

        return calories;
    }

    public double calcCalories(int HRValue) {
        int birthYear = mUserProfile.getUserBirthYear();
        int age = (Calendar.getInstance().get(Calendar.YEAR)) - birthYear;
        int gender = mUserProfile.getUserGender();
        double weight = (double) mUserProfile.getUserWeight();

        double HR = (double) HRValue;
        double calories;
        if (HR >= 90 && HR < 255) { // calculation based on formula without VO2max
            if (gender == 0) { // female
                calories = (-20.4022 + (0.4472*HR) - (0.1263*weight) +
                        (0.074*age));
                calories = calories / 4.184;

            } else { // male
                calories = (-55.0969 + (0.6309*HR) + (0.1988*weight) +
                        (0.2017*age));
                calories = calories / 4.184;
            }
        } else { // here, calculation based on Estimated Energy Requirements
            calories = caloriesEERPerMinute;
        }

        return calories;
    }
}
