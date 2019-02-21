package io.github.stevenzack.wv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;
import org.json.JSONTokener;

import wvdroid.Bridge;
import wvdroid.Wvdroid;


public class MyActivity extends Activity  {
    protected static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private WebView mWebView;
    private View customView;
    private FrameLayout fullscreenContainer;
    private WebChromeClient.CustomViewCallback customViewCallback;
    private String base;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView =  new WebView(this);
        setupWebView();
        setContentView(mWebView);
        mWebView.setWebChromeClient(new WebChromeClient() {
            /*** 视频播放相关的方法 **/
            @Override
            public View getVideoLoadingProgressView() {
                FrameLayout frameLayout = new FrameLayout(MyActivity.this);
                frameLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
                return frameLayout;
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                showCustomView(view, callback);
            }

            @Override
            public void onHideCustomView() {
                hideCustomView();
            }
        });
        String wv_action = getIntent().getStringExtra("WV_ACTION");
        if (wv_action == null || wv_action.isEmpty()) {
            base="file:///android_asset/index.html";
            mWebView.loadUrl(base);
        }else if (wv_action.equals("OPEN_URL")){
            base=getIntent().getStringExtra("URL");
            mWebView.loadUrl(base);
        } else if (wv_action.equals("LOAD_HTML")) {
            String value=getIntent().getStringExtra("HTML");
            base=value;
            value = value.replace("#", "%23");
            mWebView.loadData(value,"text/html; charset=UTF-8", null);
        }
        Wvdroid.init(new Bridge() {
            @Override
            public void eval(String s) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mWebView.evaluateJavascript(s, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                        }
                    });
                }
            }
        },base);
    }

    private void setupWebView() {
        WebSettings webSettings =mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true); // 关键点
        webSettings.setAllowFileAccess(true); // 允许访问文件
        webSettings.setSupportZoom(true); // 支持缩放
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            webSettings.setAllowFileAccessFromFileURLs(true);
            webSettings.setAllowUniversalAccessFromFileURLs(true);
        }
        mWebView.setWebViewClient(new MyWebViewClient());
    }

    private String parseHTML(String string) {
        try {
            JSONObject object = (JSONObject) (new JSONTokener(string).nextValue());
            return object.getString("Data");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /** 视频播放全屏 **/
    private void showCustomView(View view, WebChromeClient.CustomViewCallback callback) {
        // if a view already exists then immediately terminate the new one
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }

        this.getWindow().getDecorView();

        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        fullscreenContainer = new FullscreenHolder(this);
        fullscreenContainer.addView(view, COVER_SCREEN_PARAMS);
        decor.addView(fullscreenContainer, COVER_SCREEN_PARAMS);
        customView = view;
        setStatusBarVisibility(false);
        customViewCallback = callback;
    }

    /** 隐藏视频全屏 */
    private void hideCustomView() {
        if (customView == null) {
            return;
        }

        setStatusBarVisibility(true);
        FrameLayout decor = (FrameLayout) getWindow().getDecorView();
        decor.removeView(fullscreenContainer);
        fullscreenContainer = null;
        customView = null;
        customViewCallback.onCustomViewHidden();
        mWebView.setVisibility(View.VISIBLE);
    }

    /** 全屏容器界面 */
    static class FullscreenHolder extends FrameLayout {

        public FullscreenHolder(Context ctx) {
            super(ctx);
            setBackgroundColor(ctx.getResources().getColor(android.R.color.black));
        }
        @Override
        public boolean onTouchEvent(MotionEvent evt) {
            return true;
        }
    }

    private void setStatusBarVisibility(boolean visible) {
        int flag = visible ? 0 : WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setFlags(flag, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (visible) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        }else{
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        }
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
        // ...
    }

    @SuppressLint("NewApi")
    @Override
    protected void onPause() {
        mWebView.onPause();
        // ...
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if( mWebView!=null) {
            // 如果先调用destroy()方法，则会命中if (isDestroyed()) return;这一行代码，需要先onDetachedFromWindow()，再
            // destory()
            ViewParent parent = mWebView.getParent();
            if (parent != null) {
                ((ViewGroup) parent).removeView(mWebView);
            }
            mWebView.stopLoading();
            // 退出时调用此方法，移除绑定的服务，否则某些特定系统会报错
            mWebView.getSettings().setJavaScriptEnabled(false);
            mWebView.clearHistory();
            mWebView.clearView();
            mWebView.removeAllViews();
            mWebView.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        }else{
            super.onBackPressed();
        }
    }

    public static final String TAG = "MyActivity";
    class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String url=request.getUrl().toString();
                if (parseFuncs(url)) {
                    return true;
                }
                if (Wvdroid.onLoadURL(url,base)) {
                    return true;
                }
                startUrl(url);
                return true;
            }
            return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (parseFuncs(url)) {
                return true;
            }
            if (Wvdroid.onLoadURL(url,base)) {
                return true;
            }
            startUrl(url);
            return true;
        }
    }

    private void startUrl(String url) {
        Intent intent = new Intent(MyActivity.this, MyActivity.class);
        intent.putExtra("WV_ACTION","OPEN_URL");
        intent.putExtra("URL", url);
        startActivity(intent);
    }
    private boolean parseFuncs(String url) {
        switch (removeQuery(url)) {
            case "wv://finish":
                finish();
                break;
            default:
                return false;
        }
        return true;
    }
    public static String removeQuery(String url) {
        int i = url.indexOf("?");
        if (i < 0) {
            return url;
        }
        return url.substring(0, i);
    }
}