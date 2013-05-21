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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.util.Log;

public class BibsysApi {

	// If we use the same instance for all requests, it takes care of 
	// cookies for us - great!
	private final HttpClient httpClient;
    private final TaskListener listener;

	public BibsysApi(TaskListener listener) {
    	Log.i(BibsysApi.class.toString(), "new BibsysApi");
        httpClient = createHttpClient();
        this.listener = listener;
	}

    public void login(String user, String pass) {
        new LoginTask().execute(user, pass);
    }

    private HttpClient createHttpClient() {
        HttpParams params = new BasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params, HTTP.DEFAULT_CONTENT_CHARSET);
        HttpProtocolParams.setUseExpectContinue(params, true);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        schReg.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(params, schReg);

        return new DefaultHttpClient(conMgr, params);
    }

    // LoginTask as nested class. Reminder to self:
    // To instantiate an inner class, you must first instantiate the outer class. 
    // Then, create the inner object within the outer object with this syntax:
    // OuterClass.InnerClass innerObject = outerObject.new InnerClass();
	public class LoginTask extends AsyncTask<String, Void, String> {

        private Exception exception;

        @Override
        protected void onPostExecute(String result) {
           super.onPostExecute(result);
           Log.i(LoginTask.class.getName(), result);
           listener.onLoggedIn(result);
        }

        @Override
        protected String doInBackground(String... login) {

        	Log.i(LoginTask.class.toString(), "new LoginTask");

        	String user = login[0];
        	String pass = login[1];
        	
            // Creating HTTP Post
            HttpPost httpPost = new HttpPost("https://ask.bibsys.no/ask2/json/adgangProxy.jsp");
            List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(3);

        	nameValuePair.add(new BasicNameValuePair("gcmd", "logon"));
            nameValuePair.add(new BasicNameValuePair("uname", user));
            nameValuePair.add(new BasicNameValuePair("pw", pass));

        	try {
            	httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
            } catch (UnsupportedEncodingException e) {
                // writing error to Log
                e.printStackTrace();
            }
            
            // Making HTTP Request
        	StringBuilder lines = new StringBuilder();
            try {

            	HttpResponse response = httpClient.execute(httpPost);
            	StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                	//String cookie = response.getHeader("Cookie");
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                    	lines.append(line);
                    }
                } else {
                    Log.e(LoginTask.class.toString(), "HTTP request failed");
                }
             
            } catch (ClientProtocolException e) {
                // writing exception to log
                e.printStackTrace();
                     
            } catch (IOException e) {
                // writing exception to log
                e.printStackTrace();
            }
            //Log.i(LoginTask.class.toString(), lines.toString());
            String ticket = "";
            try {
            	JSONObject json = new JSONObject(lines.toString());
            	ticket = json.getString("ticket");
            } catch (Exception e) {
                e.printStackTrace();
            }
        	
        	return ticket;

        }

	}

	public class UserInfoTask extends AsyncTask<String, Void, JSONObject> {

        private Exception exception;

        @Override
        protected void onPostExecute(JSONObject result) {
           super.onPostExecute(result);
           listener.onReceivedUserInfo(result);
        }

        @Override
        protected JSONObject doInBackground(String... params) {

        	Log.i(UserInfoTask.class.toString(), "new UserInfoTask");

            // Creating HTTP Post
            HttpGet httpGet = new HttpGet("https://ask.bibsys.no/ask2/json/user.jsp");
            
            // Making HTTP Request
        	StringBuilder lines = new StringBuilder();
            try {

            	HttpResponse response = httpClient.execute(httpGet);
            	StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == 200) {
                	//String cookie = response.getHeader("Cookie");
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                    String line;
                    while ((line = reader.readLine()) != null) {
                    	lines.append(line);
                    }
                } else {
                    Log.e(UserInfoTask.class.toString(), "HTTP request failed");
                }
             
            } catch (ClientProtocolException e) {
                // writing exception to log
                e.printStackTrace();
                     
            } catch (IOException e) {
                // writing exception to log
                e.printStackTrace();
            }
            Log.i(UserInfoTask.class.toString(), lines.toString());
            try {
            	JSONObject json = new JSONObject(lines.toString());
            	return json.getJSONObject("user");
            } catch (Exception e) {
                e.printStackTrace();
            }
        	return null;
        }

	}
	
}