package com.github.lzyzsd.jsbridge.example;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.Button;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;
import com.github.lzyzsd.jsbridge.DefaultHandler;
import com.github.lzyzsd.jsbridge.LogUtils;

public class MainActivity extends Activity implements OnClickListener {

    private final String TAG = "MainActivity";


    int RESULT_CODE = 0;

    /**
     *
     */
    //
    Button mButton;
    //
    BridgeWebView mWebView;
    /**
     *
     */
    ValueCallback<Uri> mUploadMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //
        mButton = (Button) findViewById(R.id.button);
        mButton.setOnClickListener(this);
        //
        mWebView = (BridgeWebView) findViewById(R.id.webView);
        mWebView.setDefaultHandler(new DefaultHandler());
        mWebView.setWebChromeClient(new WebChromeClient() {

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType, String capture) {
                this.openFileChooser(uploadMsg);
            }

            @SuppressWarnings("unused")
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String AcceptType) {
                this.openFileChooser(uploadMsg);
            }

            public void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                pickFile();
            }
        });

        mWebView.loadUrl("file:///android_asset/demo.html");
        // 注册处理程序,以便javascript调用它
        mWebView.registerHandler("submitFromWeb", new BridgeHandler() {

            @Override
            public void handler(String data, CallBackFunction function) {
                LogUtils.e(TAG, "handler = submitFromWeb, data from web = " + data);
                // 该数据将返回给JS
                function.onCallBack("submitFromWeb exe, response data 中文 from Java");
            }

        });


        mWebView.callHandler("functionInJs", "中国心", new CallBackFunction() {
            @Override
            public void onCallBack(String data) {

            }
        });
    }


    @Override
    public void onClick(View v) {
        LogUtils.e(TAG, "---onClick---");

        /**
         * Native调用JS的 functionInJs 方法
         */
        if (mButton.equals(v)) {
            mWebView.callHandler("functionInJs", "onClick: data from Native", new CallBackFunction() {

                /**
                 * @param data JS回调回来的数据
                 */
                @Override
                public void onCallBack(String data) {
                    // TODO Auto-generated method stub
                    LogUtils.e(TAG, "onClick: reponse data from js: " + data);
                }

            });
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage) {
                return;
            }
            Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }
    }


    /**
     * 选择图片文件
     */
    public void pickFile() {
        Intent chooserIntent = new Intent(Intent.ACTION_GET_CONTENT);
        chooserIntent.setType("image/*");
        startActivityForResult(chooserIntent, RESULT_CODE);
    }
}
