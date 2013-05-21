package no.scriptotek.nfcbookscanner;

import org.json.JSONObject;

public interface TaskListener {
    void onLoggedIn(String result);
    void onReceivedUserInfo(JSONObject json);
}
