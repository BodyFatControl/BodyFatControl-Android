package bodyfatcontrol.github;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

public class ListenerService extends WearableListenerService {

    public static final String MESSAGE_PATH = "/message_path";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(MESSAGE_PATH)) {
            // broadcast the HR value to MainActivity
            LocalBroadcastManager.getInstance(MainActivity.context).sendBroadcast(
                    new Intent("HR_VALUE").putExtra(
                            "HR_VALUE", new String(messageEvent.getData())));
        }
    }
}