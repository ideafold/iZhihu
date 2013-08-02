package com.gracecode.iZhihu.api;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.gracecode.iZhihu.util.Helper;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * User: mingcheng
 * Date: 13-4-27
 */
public class Requester {

    private static final String URL_REQUEST = "http://z.ihu.im/?method=sync&timestamp=%s&sign=%s&start=%d&device=%s";
    private static final String DEVICE_UUID = android.os.Build.SERIAL;
    private static final String APP_KEY = "133ff1e10a8b244767ef734fb86f37fd";
    public static final int DEFAULT_START_OFFSET = -1;
    private static final int TIME_STAMP_LENGTH = 10;
    private static final String KEY_LAST_QUERY_TIMESTAMP = "last_query_timestamp";
    private static final int HTTP_STATUS_OK = 200;
    private static final int TIMEOUT_SECONDS = 5;

    private static Context context;
    private final SharedPreferences sharedPreferences;

    public Requester(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(getClass().getName(), Context.MODE_PRIVATE);
    }

    void saveSharedPreference(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    String getSharedPreference(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public Long getLastRequestTimeStamp() {
        String value = getSharedPreference(KEY_LAST_QUERY_TIMESTAMP, String.valueOf(0));
        return Long.parseLong(value);
    }

    private static String md5(String string) throws RuntimeException {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            int i = (b & 0xFF);
            if (i < 0x10) hex.append('0');
            hex.append(Integer.toHexString(i));
        }

        return hex.toString();
    }

    synchronized public JSONArray fetch(Integer offset) throws IOException, NetworkErrorException, JSONException {
        String requestUrl = getRequestUrl(offset);
        Log.i(context.getPackageName(), "The request URL is " + requestUrl);

        HttpGet httpGet = new HttpGet(requestUrl);
        httpGet.addHeader("Platform", "Android");
        httpGet.addHeader("Accept-Encoding", "gzip, deflate");

        DefaultHttpClient defaultHttpClient = new DefaultHttpClient();

        HttpParams httpParams = defaultHttpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_SECONDS * 1000);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_SECONDS * 1000);

        HttpResponse httpResponse = defaultHttpClient.execute(httpGet);
        if (httpResponse.getStatusLine().getStatusCode() == HTTP_STATUS_OK) {

            InputStream instream = httpResponse.getEntity().getContent();
            Header contentEncoding = httpResponse.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                instream = new GZIPInputStream(instream);
            }

            String responseString = Helper.inputStream2String(instream);
            Log.v(context.getPackageName(), responseString);

            JSONObject jsonObject = new JSONObject(responseString);
            if (jsonObject.getInt("success") != 1) {
                throw new JSONException(jsonObject.getString("message"));
            }

            markRequestTimestamp(System.currentTimeMillis());
            return jsonObject.getJSONArray("data");
        } else {
            throw new NetworkErrorException(httpResponse.getStatusLine().getStatusCode() + "");
        }
    }

    synchronized public JSONArray fetch() throws JSONException, IOException, NetworkErrorException {
        return fetch(DEFAULT_START_OFFSET);
    }

    void markRequestTimestamp(Long timestamp) {
        saveSharedPreference(KEY_LAST_QUERY_TIMESTAMP, String.valueOf(timestamp));
    }

    public void clearRequestTimestamp() {
        markRequestTimestamp(0l);
    }

    private String getRequestUrl(int offset) {
        String timeStampString = String.valueOf(System.currentTimeMillis()).substring(0, TIME_STAMP_LENGTH);
        String signString = getSignString(timeStampString);

        return String.format(URL_REQUEST,
                timeStampString,
                signString,
                offset,
                DEVICE_UUID);
    }

    private String getSignString(String stamp) {
        return md5(APP_KEY + stamp + "sync");
    }
}