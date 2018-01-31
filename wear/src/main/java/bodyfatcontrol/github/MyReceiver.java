package bodyfatcontrol.github;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equalsIgnoreCase("bodyfatcontrol.github-timer")) {
            long date = System.currentTimeMillis();
            MainActivity.currentMinute = date - (date % 60000); // get date at start of a minute

            // send the message TIMER_FIRED
            LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("TIMER_FIRED"));

        } else if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
