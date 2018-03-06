package com.github.lzyzsd.jsbridge;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONTokener;

/**
 * data of bridge
 * <p>
 * 1、Native发送给JS的消息数据
 *
 * @author haoqing
 */
public class Message {

    //
    //
    private final static String CALLBACK_ID_STR = "callbackId";
    private final static String DATA_STR = "data";
    private final static String HANDLER_NAME_STR = "handlerName";
    //
    //
    private final static String RESPONSE_ID_STR = "responseId";
    private final static String RESPONSE_DATA_STR = "responseData";

    /**
     * Native发送消息给JS时，用着三个参数
     */
    // JS 回调 Native用，由Native创建的ID
    private String callbackId; //callbackId
    private String data; //data of message
    private String handlerName; //name of handler


    /**
     * JS回调Native数据时，用以下两个参数
     */
    private String responseId; //responseId
    private String responseData; //responseData


    public String getResponseId() {
        return responseId;
    }


    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }


    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }


    /**
     * 转化为Json字符串
     *
     * @return
     */
    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CALLBACK_ID_STR, getCallbackId());
            jsonObject.put(DATA_STR, getData());
            jsonObject.put(HANDLER_NAME_STR, getHandlerName());
            String data = getResponseData();
            if (TextUtils.isEmpty(data)) {
                jsonObject.put(RESPONSE_DATA_STR, data);
            } else {
                jsonObject.put(RESPONSE_DATA_STR, new JSONTokener(data).nextValue());
            }
            jsonObject.put(RESPONSE_DATA_STR, getResponseData());
            jsonObject.put(RESPONSE_ID_STR, getResponseId());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Message toObject(String jsonStr) {
        Message m = new Message();
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            m.setHandlerName(jsonObject.has(HANDLER_NAME_STR) ? jsonObject.getString(HANDLER_NAME_STR) : null);
            m.setCallbackId(jsonObject.has(CALLBACK_ID_STR) ? jsonObject.getString(CALLBACK_ID_STR) : null);
            m.setResponseData(jsonObject.has(RESPONSE_DATA_STR) ? jsonObject.getString(RESPONSE_DATA_STR) : null);
            m.setResponseId(jsonObject.has(RESPONSE_ID_STR) ? jsonObject.getString(RESPONSE_ID_STR) : null);
            m.setData(jsonObject.has(DATA_STR) ? jsonObject.getString(DATA_STR) : null);
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return m;
    }

    public static List<Message> toArrayList(String jsonStr) {
        List<Message> list = new ArrayList<Message>();
        try {
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                Message m = new Message();
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                m.setHandlerName(jsonObject.has(HANDLER_NAME_STR) ? jsonObject.getString(HANDLER_NAME_STR) : null);
                m.setCallbackId(jsonObject.has(CALLBACK_ID_STR) ? jsonObject.getString(CALLBACK_ID_STR) : null);
                m.setResponseData(jsonObject.has(RESPONSE_DATA_STR) ? jsonObject.getString(RESPONSE_DATA_STR) : null);
                m.setResponseId(jsonObject.has(RESPONSE_ID_STR) ? jsonObject.getString(RESPONSE_ID_STR) : null);
                m.setData(jsonObject.has(DATA_STR) ? jsonObject.getString(DATA_STR) : null);
                list.add(m);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }
}
