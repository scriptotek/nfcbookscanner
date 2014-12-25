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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.rfid.libdanrfid.DDMTag;

import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends Activity {

    private static final String TAG = "NFCBookReader"; // for logging

    private NfcAdapter nfc;
    private PendingIntent mPendingIntent;

    private AlertDialog mDialog;

    String url = "file:///android_asset/www/index.html";
    
    String currentBarcode = "";
    Intent i = new Intent(Intent.ACTION_VIEW);
    Uri u = Uri.parse(url);
    Context context = this;
    WebView webview;
    boolean isWorking = true;

    private Menu optionsMenu;
    
    //Wrapping in its own function to prevent warnings
    @TargetApi(16)
    protected void enableUniversalAccess(WebSettings settings) {
         settings.setAllowUniversalAccessFromFileURLs(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "Called onCreate");
        setContentView(R.layout.main);
        
        resolveIntent(getIntent());

        webview = (WebView) findViewById(R.id.web_engine);
        WebSettings settings = webview.getSettings();
        settings.setJavaScriptEnabled(true);
        //webview.addJavascriptInterface(this, "JSInterface");
        
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            // Allow JavaScript running in the context of a file scheme URL 
        	// to access content from any origin.
        	// The default value is true for API level ICE_CREAM_SANDWICH_MR1 and below, 
        	// and false for API level JELLY_BEAN and above
            enableUniversalAccess(settings);
        }

        final Activity activity = this;
        webview.setWebViewClient(new WebViewClient() {
          public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Toast.makeText(activity, "Oh no! " + description, Toast.LENGTH_SHORT).show();
          }
        });
        webview.loadUrl(url);  

        Button refreshButton = (Button)findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new OnClickListener() {         
            @Override
            public void onClick(View v){
            	webview.reload();
            } 
        });

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc == null) {
        	// Telefonen har ikke NFC-støtte
            showMessage(R.string.error, R.string.no_nfc);
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.i(TAG, "Called onNewIntent");
        setIntent(intent);
        resolveIntent(intent);
    }

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

        if (nfc != null) {
        	// Telefonen har NFC-støtte

            if (!nfc.isEnabled()) {
            	// NFC er avskrudd
                showMessage(R.string.error, R.string.nfc_disabled);
            }

            // Hvis en tag skannes mens programmet er åpent vil vi ha 
            // intent-en direkte uten at "Velg en handling" skal vises
            nfc.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Called onPause");

        if (nfc != null) {
        	nfc.disableForegroundDispatch(this);
        }

    }
    
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Called onStop");   
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Called onStart");
    }
    
    @Override
    protected void onRestart() {
        super.onStop();
        Log.i(TAG, "Called onRestart");
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }


    /******************************************************************
     * RFID stuff
     */

    private Tag currentTag;
    private DDMTag danishTag;

    private void resolveIntent(Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, action);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            currentTag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            readTagData(currentTag);

        }
    }
    
    private void readTagData(Tag tag) {
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
        	
        	// Biblioteksbrikkene er NfcV
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

                    currentBarcode = danishTag.Barcode();

                    webview.loadUrl("javascript:barcodeScanned('" + currentBarcode + "'); return false;");

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

    }

}
