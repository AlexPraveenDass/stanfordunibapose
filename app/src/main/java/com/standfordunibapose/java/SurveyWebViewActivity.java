package com.standfordunibapose.java;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;

import com.standfordunibapose.R;

public class SurveyWebViewActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_web_view);

        String id = getIntent().getStringExtra("id");

        WebView surveyWebView = findViewById(R.id.surveyWebView);
        WebViewUtils.setWebView(surveyWebView, "https://qfreeaccountssjc1.az1.qualtrics.com/jfe/form/SV_d5bAcZGhzJPFjT0?sessionid=" + id);

    }
}