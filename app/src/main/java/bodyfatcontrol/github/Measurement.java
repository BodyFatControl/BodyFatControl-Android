package bodyfatcontrol.github;

public class Measurement {
    private long date; // UTC Unix BUT in minutes
    private int HR;
    private double calories;
    private double caloriesEERPerMinute;

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public double getCaloriesEERPerMinute() {
        return caloriesEERPerMinute;
    }

    public void setCaloriesEERPerMinute(double caloriesEERPerMinute) {
        this.caloriesEERPerMinute = caloriesEERPerMinute;
    }

    public int getHR() {
        return HR;
    }

    public void setHR(int HR) {
        this.HR = HR;
    }
}
