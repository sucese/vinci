package com.guoxiaoxing.vinci.widget.zxing.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.guoxiaoxing.vinci.Vinci;
import com.guoxiaoxing.vinci.util.RNUtils;
import com.guoxiaoxing.vinci.util.VinciLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JumpActivity extends AppCompatActivity {
    public static final String REQUEST_TYPE = "REQUEST_TYPE";

    public static final int REQUEST_QR_CODE = 0x000001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int code = getIntent().getIntExtra(REQUEST_TYPE, 0);
        switch (code) {
            case REQUEST_QR_CODE: {
                startActivityForResult(new Intent(this, CaptureActivity.class), REQUEST_QR_CODE);
                break;
            }
            default:
                finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_QR_CODE) {
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    VinciLog.log("Scan result：" + result);
                    Map<String, Object> props = new HashMap<>(10);
                    try {
                        JSONObject bundleObj = new JSONObject(result);
                        if (bundleObj.has("bundleProps")) {
                            JSONObject propsObj = bundleObj.getJSONObject("bundleProps");
                            if (propsObj != null) {
                                Iterator<String> keys = propsObj.keys();
                                while (keys.hasNext()) {
                                    String key = keys.next();
                                    props.put(key, propsObj.getString(key));
                                }
                            }
                        }
                        // TODO:添加清空更改代码后的bundle缓存逻辑
                        RNUtils.saveServer(this, (bundleObj.getString("bundleAddress").replace("http://", "")));

                        String bundleName = bundleObj.getString("bundleName");
                        VinciLog.log("bundleName：" + bundleName);
                        Vinci.loadServerBundle(this, bundleName);
                    } catch (JSONException e) {
                        showMessage("二维码解析失败" + e.getMessage());
                    }

                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    showMessage("二维码解析失败");
                }
            }
        }
        finish();
    }

    private void showMessage(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
