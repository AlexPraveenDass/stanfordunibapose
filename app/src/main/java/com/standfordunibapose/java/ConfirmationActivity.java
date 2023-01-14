package com.standfordunibapose.java;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.standfordunibapose.R;

public class ConfirmationActivity extends AppCompatActivity {

    public static final String WEB_VIEW_INTERFACE_NAME = "WEB_VIEW_INTERFACE_NAME";
    private boolean isFirstTime = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmation);

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_language))
                .setMessage(getString(R.string.choose_language_message))
                .setPositiveButton(getString(R.string.english), (dialog, which) -> {
                    dialog.dismiss();
                    setWebView("https://docs.google.com/forms/d/e/1FAIpQLScKToQlX9o06CETFfXjXBRO2r61AveRH37hC019WKeE4GPW-Q/viewform?usp=share_link");
                })
                .setNegativeButton(getString(R.string.italian), (dialog, which) -> {
                    dialog.dismiss();
                    setWebView("https://docs.google.com/forms/d/e/1FAIpQLSdVd-Fs_3i9ErSPIR0BhU88MQ1Ncuwpd3eFbvwcJPagmmLeAA/viewform?usp=share_link");
                })
                .show();

    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setWebView(String link) {

        WebView webView = findViewById(R.id.webView);
        ProgressBar pbLoading = findViewById(R.id.pbLoading);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    pbLoading.setVisibility(View.GONE);
                    Log.d("TAG", "onProgressChanged: " + webView.getUrl());
                    if (webView.getUrl().contains("formResponse")) {
                        if (isFirstTime){
                            isFirstTime = false;
                            Log.d("TAG", "onProgressChanged: success - " + webView.getUrl());
                            startActivity(new Intent(ConfirmationActivity.this, CameraXLivePreviewActivity.class));
                            finish();
                        }
                    }
                }
                super.onProgressChanged(view, newProgress);
            }

        });

        webView.setWebViewClient(new WebViewClient());
        webView.clearCache(true);
        webView.clearHistory();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.loadUrl(link);

    }


}