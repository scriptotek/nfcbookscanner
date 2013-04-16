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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.io.IOException;
import java.util.Arrays;

import android.util.Log;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.webkit.WebView;
import android.view.KeyEvent;

import no.scriptotek.nfcbookscanner.R;
import no.scriptotek.nfcbookscanner.R.id;
import no.scriptotek.nfcbookscanner.R.layout;
import no.scriptotek.nfcbookscanner.R.string;

import org.rfid.libdanrfid.DDMTag;


/**
 * An {@link Activity} which handles a broadcast of a new tag that the device just discovered.
 */
public class NfcBookReader extends Activity {

	private static final String TAG = "TagViewer";

    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    //private NdefMessage mNdefPushMessage;

    private AlertDialog mDialog;

    private Button goButton;
    String url = "http://labs.biblionaut.net/basic_info/";
    String currentBarcode = "";
    Intent i = new Intent(Intent.ACTION_VIEW);
    Uri u = Uri.parse(url);
    Context context = this;
    WebView webview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //mTagContent = (LinearLayout) findViewById(R.id.list);
        resolveIntent(getIntent());

        webview = (WebView) findViewById(R.id.web_engine);  
        webview.loadUrl(url);  
        webview.reload();

        // Let's display the progress in the activity title bar, like the
        // browser app does.
       // getWindow().requestFeature(Window.FEATURE_PROGRESS);

        webview.getSettings().setJavaScriptEnabled(true);
        
        goButton = (Button)findViewById(R.id.goButton);
        goButton.setOnClickListener(new OnClickListener() {         
            @Override
            public void onClick(View v){
                url = "http://labs.biblionaut.net/basic_info/?strekkode=" + currentBarcode;
                u = Uri.parse(url);
            	/*
                try {
                	// Start the activity
                    i.setData(u);
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                	// Raise on activity not found
                    Toast.makeText(context, "Browser not found.", Toast.LENGTH_SHORT);
                }
                */
                
                WebView engine = (WebView) findViewById(R.id.web_engine);  
                engine.loadUrl(url);  
            } 
        });
        goButton.setVisibility(View.GONE);

		ImageView iv1 = (ImageView) findViewById(R.id.imageView1);
		iv1.setVisibility(View.GONE);
        
        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
    }
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

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
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
        Log.i(TAG, "on pause");
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            //mAdapter.disableForegroundNdefPush(this);
        }
    }

    private void resolveIntent(Intent intent) {
    	String action = intent.getAction();
        //Log.i(TAG, action);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
            
            NdefMessage[] msgs;

            byte[] empty = new byte[0];
            byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
            Parcelable tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            byte[] payload = dumpTagData(tag).getBytes();
            
            NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
            NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
            msgs = new NdefMessage[] { msg };


        }
    }

    private String dumpTagData(Parcelable p) {
        StringBuilder sb = new StringBuilder();
        Tag tag = (Tag) p;
        byte[] id = tag.getId();
        //sb.append("Tag ID (hex): ").append(getHex(id)).append("\n");
        //sb.append("Tag ID (hex): ").append(toHex(id)).append("\n");
        //sb.append("Tag ID (dec): ").append(getDec(id)).append("\n");

        final TextView textView1 = (TextView) findViewById(R.id.textView12);
        final TextView textView2 = (TextView) findViewById(R.id.textView22);
        final TextView textView3 = (TextView) findViewById(R.id.textView32);
                
        for (String tech : tag.getTechList()) {
            
        	if (tech.equals(NfcV.class.getName())) {
        		
        		sb.append('\n');
        		
        		// Get an instance of NfcV for the given tag:
        		NfcV nfcvTag = NfcV.get(tag);

        		try {        			
            		nfcvTag.connect();
        		} catch (IOException e) {
            		sb.append("Klarte ikke åpne en tilkobling!\n");
            		return sb.toString();
        		}
    			
    			try {

    				// Get system information (0x2B)
    				byte[] cmd = new byte[] {
    					(byte)0x00, // Flags
    					(byte)0x2B // Command: Get system information
    				};
    				byte[] systeminfo = nfcvTag.transceive(cmd);

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
    				DDMTag danishTag = new DDMTag();
    				danishTag.addSystemInformation(systeminfo);
    				danishTag.addUserData(userdata);
    				
					ImageView iv1 = (ImageView) findViewById(R.id.imageView1);
					iv1.setVisibility(View.VISIBLE);

    				if (danishTag.isAFIsecured()) {
    	    			iv1.setImageResource(R.drawable.lock_closed);
    				} else {
    	    			iv1.setImageResource(R.drawable.lock_open);
    				}

    				textView1.setText(danishTag.Barcode());
    		        textView2.setText(danishTag.Country());
    		        textView3.setText(danishTag.ISIL());

    		        // Show the go button
    		        //goButton.setVisibility(View.VISIBLE);
    		        currentBarcode = danishTag.Barcode();
    		        
                    url = "http://labs.biblionaut.net/basic_info/?strekkode=" + currentBarcode;
                    webview.loadUrl(url);  


    		        
    			} catch (IOException e) {
        			sb.append('\n');
            		sb.append("Feil ved lesing");
            		return sb.toString();
        		}
        		
        		try {
        			nfcvTag.close();
        		} catch (IOException e) {
        			sb.append('\n');
            		sb.append("Klarte ikke lukke");
            		return sb.toString();
        		}
        		sb.append("Tilkobling lukket\n");
        		
        	}

        }

        return sb.toString();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }
}
