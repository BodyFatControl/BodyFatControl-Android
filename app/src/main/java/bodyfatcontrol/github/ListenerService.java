package bodyfatcontrol.github;

import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

public class ListenerService extends WearableListenerService {

    public static final String MESSAGE_PATH = "/message_path";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(MESSAGE_PATH)) {
            Log.i("hr_value", "HR value: " + new String(messageEvent.getData()));
        }
    }
}