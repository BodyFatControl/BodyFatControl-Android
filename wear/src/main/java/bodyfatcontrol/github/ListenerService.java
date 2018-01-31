package bodyfatcontrol.github;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.common.primitives.Longs;

import org.apache.commons.lang3.ArrayUtils;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        byte[] message = messageEvent.getData();

        // broadcast the HR value to MainActivity
        LocalBroadcastManager.getInstance(MainActivity.context).sendBroadcast(
                new Intent("RECEIVED_COMMAND").putExtra("MESSAGE", message));
    }
}