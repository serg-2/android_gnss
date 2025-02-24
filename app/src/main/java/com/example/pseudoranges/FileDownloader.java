package com.example.pseudoranges;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;

public class FileDownloader {

    private final MainActivity activity;

    public FileDownloader(MainActivity mainActivity) {
        this.activity = mainActivity;
    }

    private static final String TAG = "DOWNLOADER";

    public boolean isFileAlreadyDownloaded(String fileName) {
        File file = new File(activity.getExternalFilesDir(null), fileName);
        return file.exists();
    }

    public File downloadZipFile(String fileUrl, String fileName) {
        Log.d(TAG, "Network available: " + isNetworkAvailable());

        if (!isNetworkAvailable()) {
            return null;
        }
        OkHttpClient bootstrapClient = new OkHttpClient.Builder().build();
        DnsOverHttps dns = new DnsOverHttps.Builder()
            .client(bootstrapClient)
            //.url(HttpUrl.get("https://1.1.1.1/dns-query"))
            .url(HttpUrl.get("https://8.8.8.8/dns-query"))
            .includeIPv6(false)
            .build();

        OkHttpClient client = bootstrapClient.newBuilder().dns(dns).build();

        Request request = new Request.Builder().url(fileUrl).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Response unsuccessful.");
                return null;
            }

            File file = new File(activity.getExternalFilesDir(null), fileName);
            if (response.body() == null) {
                Log.e(TAG, "Response body is null.");
                return null;
            }

            try (InputStream inputStream = response.body().byteStream();
                 OutputStream outputStream = new FileOutputStream(file)) {
                copyStream(inputStream, outputStream);
            }
            Log.d(TAG, "File downloaded!");
            return file;
        } catch (Exception e) {
            Log.e(TAG, "Can't download file: " + e);
            // e.printStackTrace();
            return null;
        }
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws Exception {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network nw = cm.getActiveNetwork();
            if (nw == null) return false;
            NetworkCapabilities actNw = cm.getNetworkCapabilities(nw);
            return actNw != null && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) || actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH));
        }
        return false;
    }
}
