package bodyfatcontrol.github;

import java.nio.ByteBuffer;
import java.util.Calendar;

public class Utils {

    static public int returnMealTimeRadioButtonNumber () {
        int hourOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int radioButtonNumber;
        
        if      (hourOfDay >= 0  && hourOfDay < 10) radioButtonNumber = 0; // breakfast
        else if (hourOfDay >= 10 && hourOfDay < 12) radioButtonNumber = 1; // morning snack
        else if (hourOfDay >= 12 && hourOfDay < 15) radioButtonNumber = 2; // lunch
        else if (hourOfDay >= 15 && hourOfDay < 19) radioButtonNumber = 3; // afternoon snack
        else if (hourOfDay >= 19 && hourOfDay < 22) radioButtonNumber = 4; // dinner
        else                                        radioButtonNumber = 5; // evening snack

        return radioButtonNumber;
    }

    public static byte[] IntToByteArray(int value) {
        byte[] bytes = new byte[4];
        ByteBuffer.wrap(bytes).putInt(value);
        return bytes;
    }

    public static int ByteArrayToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] DoubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    public static double ByteArrayToDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static byte[] LongToByteArray(long value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putLong(value);
        return bytes;
    }

    public static long ByteArrayToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
    }
}

