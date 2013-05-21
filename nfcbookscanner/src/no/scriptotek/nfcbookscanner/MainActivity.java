/*
 * Copyright (C) 2013 Dan Michael O. Heggø
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.scriptotek.nfcbookscanner;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import android.util.Log;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.JavascriptInterface;
import android.view.KeyEvent;
import android.view.Menu;
import android.widget.SearchView;
import android.app.SearchManager;

import no.scriptotek.nfcbookscanner.R;
import no.scriptotek.nfcbookscanner.R.id;
import no.scriptotek.nfcbookscanner.R.layout;
import no.scriptotek.nfcbookscanner.R.string;

import org.json.JSONException;
import org.json.JSONObject;
import org.rfid.libdanrfid.DDMTag;


/**
 * An {@link Activity} which handles a broadcast of a new tag that the device just discovered.
 */
public class MainActivity extends Activity implements TaskListener {

    private static final String TAG = "NFCBookReader";

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;

    private AlertDialog mDialog;

    String url = "http://labs.biblionaut.net/basic_info/";
    String currentBarcode = "";
    Intent i = new Intent(Intent.ACTION_VIEW);
    Uri u = Uri.parse(url);
    Context context = this;
    WebView webview;
    boolean isWorking = true;

    private BibsysApi api;
    private Menu optionsMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Called onCreate");
        setContentView(R.layout.main);
        
        // Add dropdown to actionbar
        OnNavigationListener mOnNavigationListener = new OnNavigationListener() {
        	String[] strings = getResources().getStringArray(R.array.action_list);

        	@Override
        	public boolean onNavigationItemSelected(int position, long itemId) {
        		Log.i(TAG, strings[position]);
        		return true;
        	}
        };
        SpinnerAdapter spinnerAdp = ArrayAdapter.createFromResource(this, 
				R.array.action_list,
		        android.R.layout.simple_spinner_dropdown_item);
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        AccountManager am = AccountManager.get(this); // "this" references the current Context
    	String mPassword;
        Account[] accounts = am.getAccountsByType("no.scriptotek.bibsys");

        //Spinner spinner = (Spinner) findViewById();

        for (int i=0; i < accounts.length; i++) {
        	Log.i(TAG, accounts[i].name);
        	mPassword = am.getPassword(accounts[i]);
        	Log.i(TAG, mPassword);
        
        	ArrayList<String> arrayList1 = new ArrayList<String>();
            arrayList1.add(accounts[i].name);
            spinnerAdp = new ArrayAdapter<String> (this,android.R.layout.simple_spinner_dropdown_item,arrayList1);
            //spinner.setAdapter(adp);

        //mTagContent = (LinearLayout) findViewById(R.id.list);
        }
		actionBar.setListNavigationCallbacks(spinnerAdp, mOnNavigationListener);
		actionBar.setDisplayShowTitleEnabled(false);

      	api = new BibsysApi(MainActivity.this);
    	if (accounts.length > 0) {
    		setWorking(true);
    		api.login(accounts[0].name, am.getPassword(accounts[0]));
    	} else {
    		setWorking(false);    		
    	}
        
        resolveIntent(getIntent());

        webview = (WebView) findViewById(R.id.web_engine);
        webview.getSettings().setJavaScriptEnabled(true);
        final Activity activity = this;

        webview.addJavascriptInterface(this, "JSInterface");

        webview.setWebChromeClient(new WebChromeClient() {
          public void onProgressChanged(WebView view, int progress) {
            // Activities and WebViews measure progress with different scales.
            // The progress meter will automatically disappear when we reach 100%
        	  if (progress == 100) {
        		  setWorking(false);
        	  } else {
        		  setWorking(true);        		
        	  }
          }
        });
        webview.setWebViewClient(new WebViewClient() {
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
          }
        });
        webview.loadUrl(url);  

        ImageView lockIcon = (ImageView)findViewById(R.id.lock_icon);
        lockIcon.setClickable(true);
        lockIcon.setOnClickListener(new OnClickListener() {
        	@Override
        	public void onClick(View v) {
            	Log.i(TAG, "Clicked icon");
                toggleAfiState(currentTag);
        	}
        });

        Button loginButton = (Button)findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new OnClickListener() {         
            @Override
            public void onClick(View v){
            	
            	webview.reload();
                /*url = "http://labs.biblionaut.net/basic_info/?strekkode=" + currentBarcode;
                u = Uri.parse(url);
                
                try {
                    // Start the activity
                    i.setData(u);
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    // Raise on activity not found
                    Toast.makeText(context, "Browser not found.", Toast.LENGTH_SHORT);
                }
                
                
                WebView engine = (WebView) findViewById(R.id.web_engine);  
                engine.loadUrl(url);  
                */
            } 
        });
        loginButton.setVisibility(View.GONE);

        ImageView iv1 = (ImageView) findViewById(R.id.lock_icon);
        iv1.setVisibility(View.GONE);
        
        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
    }
    
    @JavascriptInterface
    public void loanBook(String hello) {
    	Toast.makeText(this, hello, Toast.LENGTH_SHORT).show();
    }

    /******************************************************************
     * Action bar
     */

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
    	
    	this.optionsMenu = menu;

    	// Inflate the menu; this adds items to the action bar if it is present.
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
		
		// Get the SearchView and set the searchable configuration
		/*SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();
		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));*/
		// searchView.setIconifiedByDefault(false);

    	if (!isWorking) {
    		// hide progressbar
    		setWorking(false);    		
    	}
		return true;
	}

    /* When the user selects an action item, your activity receives a call to 
     * onOptionsItemSelected(), passing the ID supplied by the android:id 
     * attribute—the same callback received for all items in the options menu.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings:

                final Intent preferencesActivity = new Intent(getBaseContext(), SettingsActivity.class);
                startActivity(preferencesActivity);

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setWorking(boolean isWorking) {
    	if (isWorking == this.isWorking) {
    		return;
    	}
        this.isWorking = isWorking;
        if (optionsMenu != null) {
            final MenuItem refreshItem = optionsMenu.findItem(R.id.progressbar);
            if (refreshItem != null) {
                if (isWorking) {
                    refreshItem.setActionView(R.layout.actionbar_progress);  // from the filename actionbar_progress.xml
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }

        
        /*
        if(isLoading)
            refreshMenuItem.setActionView(R.layout.actionbar_refresh_progress);
        else
            refreshMenuItem.setActionView(null);*/
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Called onActivityResult");
        /*if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
        if (data.hasExtra("returnKey1")) {
          Toast.makeText(this, data.getExtras().getString("returnKey1"),
              Toast.LENGTH_SHORT).show();
        }*/
    }

    /******************************************************************
     * Callbacks from BibsysApi
     */

    @Override
    public void onLoggedIn(String result) {
        Log.i(TAG, "onLoggedIn");
        api.new UserInfoTask().execute();
        /*if (progressDialog != null) {
            progressDialog.dismiss();
        }*/
    }

    @Override
    public void onReceivedUserInfo(JSONObject result) {
        Log.i(TAG, "onReceivedUserInfo");
		setWorking(false);
        String patronid = "";
        try {
        	patronid = result.getString("patronid");
        } catch (JSONException e) {
            e.printStackTrace();        	
        }
        Log.i(TAG, patronid);
        /*if (progressDialog != null) {
            progressDialog.dismiss();
        }*/
    }

    /******************************************************************
     * Some standard callbacks
     */

/*
    @Override
    public void onBackPressed() {
        // do something on back.
        Log.i(TAG, "back button pressed");
        return;
    }*/

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            switch(keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if(webview.canGoBack() == true){
                    webview.goBack();
                } else {
                    finish();
                }
                return true;
            }

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Called onResume");
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showMessage(R.string.error, R.string.nfc_disabled);
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            //mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Called onPause");
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            //mAdapter.disableForegroundNdefPush(this);
        }
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }


    /******************************************************************
     * RFID stuff that we should eventually put into its own class
     */
    private Tag currentTag;
    private DDMTag danishTag;

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, action);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            
            //NdefMessage[] msgs;
            
            currentTag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

           //byte[] empty = new byte[0];
            //byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            dumpTagData(currentTag);

            /*NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
            NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
            msgs = new NdefMessage[] { msg };
*/

        }
    }
    
    private void toggleAfiState(Tag tag) {
    	// TODO: check if is valid tag and nfcv..

        // Get an instance of NfcV for the given tag:
        NfcV nfcvTag = NfcV.get(tag);

        try {                   
            nfcvTag.connect();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Klarte ikke åpne en tilkobling!", Toast.LENGTH_SHORT).show();                    
            return;
        }

        try {

            byte afiState;
        	if (danishTag.isAFIsecured()) {
        		afiState = DDMTag.AFI_OFF;
        		Log.i(TAG, "Set AFI OFF");
            } else {
        		afiState = DDMTag.AFI_ON;            	
        		Log.i(TAG, "Set AFI ON");
            }
        	// Write AFI (0x27)
            byte[] cmd = new byte[] {
                (byte)0x00, // Flags
                (byte)0x27, // Set AFI
                afiState
            };
            byte[] response = nfcvTag.transceive(cmd);

        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Det oppsto en feil under skriving!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            nfcvTag.close();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Klarte ikke å lukke tilkoblingen!", Toast.LENGTH_SHORT).show();
            return;
        }

    	dumpTagData(tag);
    }
    
    private void dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        //sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
        //sb.append("Tag ID (hex): ").append(toHex(id)).append("\n");
        //sb.append("Tag ID (dec): ").append(getDec(id)).append("\n");

        final TextView textView1 = (TextView) findViewById(R.id.textView12);
        final TextView textView2 = (TextView) findViewById(R.id.textView22);
        final TextView textView23 = (TextView) findViewById(R.id.textView23);
        final TextView textView3 = (TextView) findViewById(R.id.textView32);
        final TextView textView33 = (TextView) findViewById(R.id.textView33);

        boolean techFound = false;
        for (String tech : tag.getTechList()) {

            if (tech.equals(NfcV.class.getName())) {
            	techFound = true;

                sb.append('\n');

                // Get an instance of NfcV for the given tag:
                NfcV nfcvTag = NfcV.get(tag);

                try {                   
                    nfcvTag.connect();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Klarte ikke åpne en tilkobling!", Toast.LENGTH_SHORT).show();                    
                    return;
                }
                
                try {

                    // Get system information (0x2B)
                    byte[] cmd = new byte[] {
                        (byte)0x00, // Flags
                        (byte)0x2B // Command: Get system information
                    };
                    byte[] systeminfo = nfcvTag.transceive(cmd);

                    // Chop off the initial 0x00 byte:
                    systeminfo = Arrays.copyOfRange(systeminfo, 1, 15);

                    // Read first 8 blocks containing the 32 byte of userdata defined in the Danish model
                    cmd = new byte[] { 
                        (byte)0x00, // Flags
                        (byte)0x23, // Command: Read multiple blocks
                        (byte)0x00, // First block (offset)
                        (byte)0x08  // Number of blocks
                    };
                    byte[] userdata = nfcvTag.transceive(cmd);

                    // Chop off the initial 0x00 byte:
                    userdata = Arrays.copyOfRange(userdata, 1, 32);

                    // Parse the data using the DDMTag class:
                    danishTag = new DDMTag();
                    danishTag.addSystemInformation(systeminfo);
                    danishTag.addUserData(userdata);
                    
                    ImageView iv1 = (ImageView) findViewById(R.id.lock_icon);
                    iv1.setVisibility(View.VISIBLE);

                    if (danishTag.isAFIsecured()) {
                        iv1.setImageResource(R.drawable.lock_closed);
                    } else {
                        iv1.setImageResource(R.drawable.lock_open);
                    }

                    textView1.setText(danishTag.Country());
                    textView2.setText(danishTag.ISIL());
                    textView3.setText(danishTag.Barcode());
                    textView33.setText("Del " + danishTag.getPartNum() + " av " + danishTag.getofParts());

                    // Show the go button
                    //goButton.setVisibility(View.VISIBLE);
                    currentBarcode = danishTag.Barcode();

                    url = "http://labs.biblionaut.net/basic_info/?strekkode=" + currentBarcode;
                    webview.loadUrl(url);

                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Det oppsto en feil under lesing!", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    nfcvTag.close();
                } catch (IOException e) {
                    Toast.makeText(getApplicationContext(), "Klarte ikke å lukke tilkoblingen!", Toast.LENGTH_SHORT).show();
                    return;
                }
                sb.append("Tilkobling lukket\n");
            }
        }
        
        if (techFound == false) {
            showMessage(R.string.error, R.string.unknown_tech);
        }

        //sb.toString();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "Called onNewIntent");
        setIntent(intent);
        resolveIntent(intent);
    }

}
