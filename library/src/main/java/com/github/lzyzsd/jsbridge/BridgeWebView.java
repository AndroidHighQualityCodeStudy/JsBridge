package com.github.lzyzsd.jsbridge;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressLint("SetJavaScriptEnabled")
public class BridgeWebView extends WebView implements WebViewJavascriptBridge {

    private final String TAG = "BridgeWebView";

    public static final String toLoadJs = "WebViewJavascriptBridge.js";

    // 1、JS回调Native数据时候使用；key: id value: callback (通过JS返回的callbackID 可以找到相应的CallBack方法)
    Map<String, CallBackFunction> responseCallbacks = new HashMap<String, CallBackFunction>();
    // 注册处理程序,以便javascript调用 key:方法名称 value:要处理的事件
    Map<String, BridgeHandler> messageHandlers = new HashMap<String, BridgeHandler>();

    //
    BridgeHandler defaultHandler = new DefaultHandler();

    private List<Message> startupMessage = new ArrayList<Message>();

    public List<Message> getStartupMessage() {
        return startupMessage;
    }

    public void setStartupMessage(List<Message> startupMessage) {
        this.startupMessage = startupMessage;
    }

    private long uniqueId = 0;

    public BridgeWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BridgeWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public BridgeWebView(Context context) {
        super(context);
        init();
    }

    /**
     * @param handler default handler,handle messages send by js without assigned handler name,
     *                if js message has handler name, it will be handled by named handlers registered by native
     */
    public void setDefaultHandler(BridgeHandler handler) {
        this.defaultHandler = handler;
    }

    private void init() {
        this.setVerticalScrollBarEnabled(false);
        this.setHorizontalScrollBarEnabled(false);
        this.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        this.setWebViewClient(generateBridgeWebViewClient());
    }

    protected BridgeWebViewClient generateBridgeWebViewClient() {
        return new BridgeWebViewClient(this);
    }

    /**
     * 1、获取到CallBackFunction data执行调用并且从数据集移除
     * <p>
     * 2、回调Native{@link #flushMessageQueue()} Callback方法
     *
     * @param url
     */
    void handlerReturnData(String url) {
        LogUtils.d(TAG, "handlerReturnData——>url: " + url);
        // 获取js的方法名称
        // _fetchQueue
        String functionName = BridgeUtil.getFunctionFromReturnUrl(url);
        // 获取_fetchQueue 对应的回调方法
        CallBackFunction f = responseCallbacks.get(functionName);
        // 获取body Message消息体
        String data = BridgeUtil.getDataFromReturnUrl(url);

        // 回调 Native flushMessageQueue callback方法
        if (f != null) {
            LogUtils.d(TAG, "onCallBack data" + data);
            f.onCallBack(data);
            responseCallbacks.remove(functionName);
            return;
        }
    }

    @Override
    public void send(String data) {
        send(data, null);
    }

    @Override
    public void send(String data, CallBackFunction responseCallback) {
        doSend(null, data, responseCallback);
    }

    /**
     * Native 调用 JS
     * <p>
     * 保存message到消息队列
     *
     * @param handlerName      JS中注册的handlerName
     * @param data             Native传递给JS的数据
     * @param responseCallback JS处理完成后，回调到Native
     */
    private void doSend(String handlerName, String data, CallBackFunction responseCallback) {
        LogUtils.e(TAG, "doSend——>data: " + data);
        LogUtils.e(TAG, "doSend——>handlerName: " + handlerName);
        // 创建一个消息体
        Message m = new Message();
        // 添加数据
        if (!TextUtils.isEmpty(data)) {
            m.setData(data);
        }
        //
        if (responseCallback != null) {
            // 创建回调ID
            String callbackStr = String.format(BridgeUtil.CALLBACK_ID_FORMAT, ++uniqueId + (BridgeUtil.UNDERLINE_STR + SystemClock.currentThreadTimeMillis()));
            // 1、JS回调Native数据时候使用；key: id value: callback (通过JS返回的callbackID 可以找到相应的CallBack方法)
            responseCallbacks.put(callbackStr, responseCallback);
            // 1、JS回调Native数据时候使用；key: id value: callback (通过JS返回的callbackID 可以找到相应的CallBack方法)
            m.setCallbackId(callbackStr);
        }
        // JS中注册的方法名称
        if (!TextUtils.isEmpty(handlerName)) {
            m.setHandlerName(handlerName);
        }
        LogUtils.e(TAG, "doSend——>message: " + m.toJson());
        // 添加消息 或者 分发消息到JS
        queueMessage(m);
    }

    /**
     * list<message> != null 添加到消息集合否则分发消息
     *
     * @param m Message
     */
    private void queueMessage(Message m) {
        LogUtils.e(TAG, "queueMessage——>message: " + m.toJson());
        if (startupMessage != null) {
            startupMessage.add(m);
        } else {
            // 分发消息
            dispatchMessage(m);
        }
    }

    /**
     * 分发message 必须在主线程才分发成功
     *
     * @param m Message
     */
    void dispatchMessage(Message m) {
        LogUtils.e(TAG, "dispatchMessage——>message: " + m.toJson());
        // 转化为JSon字符串
        String messageJson = m.toJson();
        //escape special characters for json string  为json字符串转义特殊字符
        messageJson = messageJson.replaceAll("(\\\\)([^utrn])", "\\\\\\\\$1$2");
        messageJson = messageJson.replaceAll("(?<=[^\\\\])(\")", "\\\\\"");
        String javascriptCommand = String.format(BridgeUtil.JS_HANDLE_MESSAGE_FROM_JAVA, messageJson);

        LogUtils.e(TAG, "dispatchMessage——>javascriptCommand: " + javascriptCommand);
        // 必须要找主线程才会将数据传递出去 --- 划重点
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            // 调用JS中_handleMessageFromNative方法
            this.loadUrl(javascriptCommand);
        }
    }

    /**
     * 1、调用JS的 _fetchQueue方法，获取JS中处理后的消息队列。
     * JS 中_fetchQueue 方法 中将Message数据返回到Native的 {@link #BridgeWebViewClient.shouldOverrideUrlLoading}中
     * <p>
     * 2、等待{@link #handlerReturnData} 回调 Callback方法
     */
    void flushMessageQueue() {
        LogUtils.d(TAG, "flushMessageQueue");
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            // 调用JS的 _fetchQueue方法
            BridgeWebView.this.loadUrl(BridgeUtil.JS_FETCH_QUEUE_FROM_JAVA, new CallBackFunction() {

                @Override
                public void onCallBack(String data) {
                    LogUtils.d(TAG, "flushMessageQueue——>data: " + data);
                    // deserializeMessage 反序列化消息
                    List<Message> list = null;
                    try {
                        list = Message.toArrayList(data);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return;
                    }
                    if (list == null || list.size() == 0) {
                        LogUtils.e(TAG, "flushMessageQueue——>list.size() == 0");
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        Message m = list.get(i);
                        String responseId = m.getResponseId();
                        /**
                         * 完成Native向JS发送信息后的回调
                         */
                        // 是否是response  CallBackFunction
                        if (!TextUtils.isEmpty(responseId)) {
                            CallBackFunction function = responseCallbacks.get(responseId);
                            String responseData = m.getResponseData();
                            function.onCallBack(responseData);
                            responseCallbacks.remove(responseId);
                        } else {
                            CallBackFunction responseFunction = null;
                            // if had callbackId 如果有回调Id
                            final String callbackId = m.getCallbackId();
                            if (!TextUtils.isEmpty(callbackId)) {
                                // 创建一个
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        Message responseMsg = new Message();
                                        responseMsg.setResponseId(callbackId);
                                        responseMsg.setResponseData(data);
                                        queueMessage(responseMsg);
                                    }
                                };
                            } else {
                                responseFunction = new CallBackFunction() {
                                    @Override
                                    public void onCallBack(String data) {
                                        // do nothing
                                    }
                                };
                            }
                            // BridgeHandler执行
                            BridgeHandler handler;
                            if (!TextUtils.isEmpty(m.getHandlerName())) {
                                handler = messageHandlers.get(m.getHandlerName());
                            } else {
                                handler = defaultHandler;
                            }
                            if (handler != null) {
                                handler.handler(m.getData(), responseFunction);
                            }
                        }
                    }
                }
            });
        }
    }


    /**
     * 加载js脚本
     *
     * @param jsUrl
     * @param returnCallback
     */
    public void loadUrl(String jsUrl, CallBackFunction returnCallback) {
        this.loadUrl(jsUrl);
        // 添加至 Map<String, CallBackFunction>

        // JS方法名为Key  returnCallback为Value
        responseCallbacks.put(BridgeUtil.parseFunctionName(jsUrl), returnCallback);
    }

    /**
     * register handler,so that javascript can call it
     * 注册处理程序,以便javascript调用它
     *
     * @param handlerName handlerName
     * @param handler     BridgeHandler
     */
    public void registerHandler(String handlerName, BridgeHandler handler) {
        if (handler != null) {
            // 添加至 Map<String, BridgeHandler>
            messageHandlers.put(handlerName, handler);
        }
    }

    /**
     * unregister handler
     *
     * @param handlerName
     */
    public void unregisterHandler(String handlerName) {
        if (handlerName != null) {
            messageHandlers.remove(handlerName);
        }
    }

    /**
     * Native调用JS
     * <p>
     * call javascript registered handler
     * 调用javascript处理程序注册
     *
     * @param handlerName JS中注册的handlerName
     * @param data        Native传递给JS的数据
     * @param callBack    JS处理完成后，回调到Native
     */
    public void callHandler(String handlerName, String data, CallBackFunction callBack) {
        LogUtils.e(TAG, "callHandler——>handlerName: " + handlerName);
        LogUtils.e(TAG, "callHandler——>data: " + data);
        doSend(handlerName, data, callBack);
    }
}
