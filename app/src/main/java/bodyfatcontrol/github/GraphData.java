package bodyfatcontrol.github;

import android.content.Context;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GraphData {
    private Context mContext;
    private double mCurrentCaloriesEER = -1;
    private double mCaloriesActive = 0;
    private double mCaloriesConsumed = 0;
    private long mInitialDate = 0;
    private long mFinalDate = 0;

    public GraphData(Context context, long initialDate, long finalDate) {
        mContext = context;
        mInitialDate = initialDate;
        mFinalDate = finalDate;
    }

    public List<Entry> prepareCalories() {
        List<Entry> graphDataEntriesList = new ArrayList<Entry>();

        // Get the measurements from midnight today
        DataBaseCalories dataBase = new DataBaseCalories(mContext);
        ArrayList<Measurement> measurementList = dataBase.DataBaseGetMeasurements(mInitialDate, mFinalDate);

        long date = 0;
        long endOfToday = (MainActivity.SECONDS_24H / 60000) - 1; // in minutes
        long graphFinalDate = (mFinalDate - mInitialDate) / 60000; // in minutes
        double caloriesEERPerMinute = dataBase.DataBaseGetLastCaloriesEERPerMinute();
        Iterator measurementListIterator = measurementList.iterator();
        Measurement measurement = null;
        double caloriesActiveSum = 0;
        double calories = 0;

        // ***************************************************
        // Now calc the active calories and prepare the graph data
        date = 0;
        for ( ; date < endOfToday; date++) { // Loop trough all the minutes starting from today midnight

            if (date <= graphFinalDate) { //  calc calories only until current date
                if (measurement == null && measurementListIterator.hasNext() ) { // read new measurement if wasn't done before
                    measurement = (Measurement) measurementListIterator.next();
                }

                if (measurement != null) {
                    calories = measurement.getCalories();
                    caloriesEERPerMinute = measurement.getCaloriesEERPerMinute();

                    // Calc the EER calories for the first time
                    if (mCurrentCaloriesEER == -1) { mCurrentCaloriesEER = caloriesEERPerMinute * (graphFinalDate + 1); }

                    if (calories > caloriesEERPerMinute) { // means that we have active calories here
                        caloriesActiveSum += calories - caloriesEERPerMinute; // subtract the EER calories value, to get only the calories active value
                    }
                    measurement = null;
                }

                graphDataEntriesList.add(new Entry((float) date / 60, (float) (caloriesActiveSum + (mCurrentCaloriesEER/10))));
            }

            if (date == endOfToday && (mInitialDate < (MainActivity.mMidNightToday/60))) { //  last point
                graphDataEntriesList.add(new Entry((float) date / 60, (float) (caloriesActiveSum + (mCurrentCaloriesEER/10))));
            }
        }

//        mCurrentCaloriesEER /= 1000;
//        mCaloriesActive = caloriesActiveSum/1000;

        mCaloriesActive = caloriesActiveSum;
        return graphDataEntriesList;
    }

    public double getCurrentCaloriesEER() {
        return mCurrentCaloriesEER;
    }

    public double getCaloriesActive() {
        return mCaloriesActive;
    }

    public double getCaloriesConsumed() {
        return mCaloriesConsumed;
    }

    public List<Entry> prepareCaloriesConsumed() {
        List<Entry> graphDataEntriesList = new ArrayList<Entry>();

        // Get the measurements from midnight today
        DataBaseLogFoods dataBaseLogFoods = new DataBaseLogFoods(mContext);
        ArrayList<Foods> foodsList = dataBaseLogFoods.DataBaseLogFoodsGetFoods(mInitialDate, mFinalDate);

        long date = 0;
        long foodDate = 0;
        long endOfToday = (MainActivity.SECONDS_24H / 1000) - 1; // in seconds
        long graphFinalDate = (mFinalDate - mInitialDate) / 1000; // in seconds
        Iterator foodsListIterator = foodsList.iterator();
        Foods food = null;
        double caloriesSum = 0;
        double previousCaloriesSum = 0;
        double previewCaloriesSum_1_10 = 0;
        double foodCalories;
        boolean moveToNextFood = true;
        mCaloriesConsumed = 0;
        // Loop trough all the minutes starting from today midnight
        for ( ; date < endOfToday; date += 60) {

            if ((moveToNextFood == true) && foodsListIterator.hasNext()) {
                food = (Foods) foodsListIterator.next();
                foodDate = (food.getDate() - mInitialDate) / 1000; // in seconds
                moveToNextFood = false;
            }

            final double caloriesEER_1_10 = mCurrentCaloriesEER/10;
            while (foodDate >= date && foodDate < (date + 60)) { // food is in this interval time
                if (food == null) break;
                foodCalories = food.getCaloriesLogged();
                mCaloriesConsumed += foodCalories;
                previewCaloriesSum_1_10 = caloriesSum + (foodCalories/10);

                if (caloriesSum > caloriesEER_1_10) { // we are already over 1/10 interval
                    caloriesSum = caloriesSum + foodCalories;

                } else if (previewCaloriesSum_1_10 <= caloriesEER_1_10) { // we will be in the 1/10 interval
                    caloriesSum += (foodCalories / 10);

                } else { // we have some part in and other over 1/10 interval
                    double value = caloriesEER_1_10 - caloriesSum;
                    foodCalories = (foodCalories/10) - value;
                    caloriesSum = caloriesEER_1_10 + foodCalories*10;
                }

                graphDataEntriesList.add(new Entry((float) date / (60 * 60), (float) (previousCaloriesSum)));
                graphDataEntriesList.add(new Entry((float) date / (60 * 60), (float) (caloriesSum)));
                previousCaloriesSum = caloriesSum;

                if (foodsListIterator.hasNext()) { // iterate on the foods in the same interval
                    food = (Foods) foodsListIterator.next();
                    foodDate = (food.getDate() - mInitialDate) / 1000; // in seconds
                } else  {
                    break;
                }
            }

            if (graphFinalDate >= date && graphFinalDate < (date + 60)) { // add a point at the current date
                graphDataEntriesList.add(new Entry((float) date / (60 * 60), (float) (caloriesSum)));
            }

            if (date == 0) { // very first value should be added to the graph
                graphDataEntriesList.add(new Entry(0, -2));
            }

            if (date == (endOfToday - 59) && (mInitialDate < MainActivity.mMidNightToday)) { //  last point
                graphDataEntriesList.add(new Entry((float) date / (60 * 60), (float) (caloriesSum)));
            }
        }

        return graphDataEntriesList;
    }
}
