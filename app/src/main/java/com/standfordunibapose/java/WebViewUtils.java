package com.standfordunibapose.java;

import android.annotation.SuppressLint;
import android.webkit.WebView;

public class WebViewUtils {

    @SuppressLint("SetJavaScriptEnabled")
    public static void setWebView(WebView webView, String link) {
        webView.setWebViewClient(new WebViewActivity.MyBrowser());
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(link);

    }
}
