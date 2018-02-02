package bodyfatcontrol.github.common;

import android.app.UiModeManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.Calendar;

import bodyfatcontrol.github.common.UserProfile;

import static android.content.Context.UI_MODE_SERVICE;

public class DataBaseUserProfile extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_DIR = "body_fat_control";
    private static final String DATABASE_NAME = "database_user_profile.db";
    private static final String TABLE_NAME = "user_profile";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_USER_NAME = "user_name";
    private static final String COLUMN_USER_BIRTH_YEAR = "user_birth_year";
    private static final String COLUMN_USER_GENDER = "user_gender";
    private static final String COLUMN_USER_HEIGH = "user_birth_height";
    private static final String COLUMN_USER_WEIGH = "user_birth_weight";
    private static final String COLUMN_USER_ACTIVITY_CLASS = "user_activity_class";

    public DataBaseUserProfile(Context context, boolean runningOnWear) {
        super(context, runningOnWear ? DATABASE_NAME :
                (Environment.getExternalStorageDirectory() +
                        File.separator + DATABASE_DIR +
                        File.separator + DATABASE_NAME), null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " integer PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_DATE + " integer UNIQUE, " + /* UNIQUE means that there will not be duplicate entries with the same date */
                COLUMN_USER_NAME + " text, " +
                COLUMN_USER_BIRTH_YEAR + " integer, " +
                COLUMN_USER_GENDER + " integer, " +
                COLUMN_USER_HEIGH + " integer, " +
                COLUMN_USER_WEIGH + " integer, " +
                COLUMN_USER_ACTIVITY_CLASS + " integer)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public UserProfile DataBaseGetLastUserProfile() {
        SQLiteDatabase db = this.getReadableDatabase();

        // build the query
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_DATE + " DESC LIMIT 1";
        // open database
        Cursor cursor = db.rawQuery(query, null);

        UserProfile userProfile = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            userProfile = new UserProfile();
            userProfile.setId(cursor.getInt(cursor.getColumnIndex(COLUMN_ID)));
            userProfile.setDate(cursor.getLong(cursor.getColumnIndex(COLUMN_DATE)));
            userProfile.setUserName(cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME)));
            userProfile.setUserBirthYear(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_BIRTH_YEAR)));
            userProfile.setUserGender(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_GENDER)));
            userProfile.setUserHeight(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_HEIGH)));
            userProfile.setUserWeight(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_WEIGH)));
            userProfile.setUserActivityClass(cursor.getInt(cursor.getColumnIndex(COLUMN_USER_ACTIVITY_CLASS)));
        } else {
            userProfile = new UserProfile();

            Calendar rightNow = Calendar.getInstance();
            long offset = rightNow.get(Calendar.ZONE_OFFSET) + rightNow.get(Calendar.DST_OFFSET);
            long rightNowMillis = rightNow.getTimeInMillis() + offset;
            userProfile.setDate(rightNowMillis);
            userProfile.setUserName("");
            userProfile.setUserBirthYear(1980);
            userProfile.setUserGender(0);
            userProfile.setUserHeight(175);
            userProfile.setUserWeight(85);
            userProfile.setUserActivityClass(0);
        }

        cursor.close();
        db.close(); // Closing database connection
        return userProfile;
    }

    public void DataBaseUserProfileWrite (UserProfile userProfile) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(COLUMN_DATE, userProfile.getDate());
        values.put(COLUMN_USER_NAME, userProfile.getUserName());
        values.put(COLUMN_USER_BIRTH_YEAR, userProfile.getUserBirthYear());
        values.put(COLUMN_USER_GENDER, userProfile.getUserGender());
        values.put(COLUMN_USER_HEIGH, userProfile.getUserHeight());
        values.put(COLUMN_USER_WEIGH, userProfile.getUserWeight());
        values.put(COLUMN_USER_ACTIVITY_CLASS, userProfile.getUserActivityClass());

        // Inserting Row: if there is one UserProfile with the same COLUMN_DATE, the old one will be replaced
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close(); // Closing database connection
    }
}

