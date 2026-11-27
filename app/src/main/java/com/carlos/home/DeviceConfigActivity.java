package com.carlos.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.carlos.common.ui.adapter.bean.DeviceData;
import com.lody.virtual.client.ipc.VDeviceManager;
import com.lody.virtual.remote.VDeviceConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 自己写的伪造设备页，替代原版打不开的 DeviceDetailActiivty。
 * 引擎按虚拟用户 userId 读配置，所以这里直接改 userId 对应的那份配置。
 */
public class DeviceConfigActivity extends AppCompatActivity {

    public static void open(Activity activity, DeviceData data, int position) {
        Intent intent = new Intent(activity, DeviceConfigActivity.class);
        intent.putExtra("title", data.name);
        intent.putExtra("packageName", data.packageName);
        intent.putExtra("userId", data.userId);
        intent.putExtra("position", position);
        activity.startActivityForResult(intent, 1001);
    }

    private static final String[] PROP_KEYS = {
            "BRAND", "MODEL", "PRODUCT", "DEVICE", "BOARD",
            "DISPLAY", "ID", "MANUFACTURER", "FINGERPRINT"
    };
    private static final String[] PROP_LABELS = {
            "品牌", "型号", "产品名", "设备", "主板",
            "显示版本", "编译 ID", "制造商", "指纹"
    };

    private int mUserId;
    private VDeviceConfig mConfig;
    private Switch mEnableSwitch;
    private EditText mImeiEdit;
    private EditText mAndroidIdEdit;
    private EditText mImsiEdit;
    private EditText mWifiMacEdit;
    private EditText mBtMacEdit;
    private EditText mSerialEdit;
    private final List<EditText> mPropEdits = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        mUserId = intent.getIntExtra("userId", -1);
        String title = intent.getStringExtra("title");
        if (mUserId < 0) {
            Toast.makeText(this, "分身信息不对", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title == null ? "伪造设备" : title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        loadConfig();
        buildUi();
        refreshUi();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadConfig() {
        try {
            mConfig = VDeviceManager.get().getDeviceConfig(mUserId);
        } catch (Throwable e) {
            mConfig = null;
        }
        if (mConfig == null) {
            mConfig = new VDeviceConfig();
            Toast.makeText(this, "设备服务暂不可用，只能先看看", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);

        TextView hint = new TextView(this);
        hint.setText("开关打开后，这个分身看到的 IMEI、Android ID、MAC 等都是下面填的值；点随机生成会自动打开开关；保存后重启分身生效。每个分身各用各的配置。");
        hint.setPadding(0, 0, 0, dp(12));
        root.addView(hint);

        mEnableSwitch = new Switch(this);
        mEnableSwitch.setText("启用伪造设备");
        mEnableSwitch.setPadding(0, 0, 0, dp(8));
        root.addView(mEnableSwitch);

        mImeiEdit = addField(root, "IMEI");
        mAndroidIdEdit = addField(root, "Android ID");
        mImsiEdit = addField(root, "IMSI / SIM 卡号");
        mWifiMacEdit = addField(root, "WiFi MAC");
        mBtMacEdit = addField(root, "蓝牙 MAC");
        mSerialEdit = addField(root, "序列号");
        for (int i = 0; i < PROP_KEYS.length; i++) {
            mPropEdits.add(addField(root, PROP_LABELS[i]));
        }

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, dp(16), 0, 0);
        Button randomBtn = new Button(this);
        randomBtn.setText("随机生成");
        randomBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRandom();
            }
        });
        Button saveBtn = new Button(this);
        saveBtn.setText("保存");
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSave();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMargins(0, 0, dp(8), 0);
        btnRow.addView(randomBtn, lp);
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp2.setMargins(dp(8), 0, 0, 0);
        btnRow.addView(saveBtn, lp2);
        root.addView(btnRow);
    }

    private EditText addField(LinearLayout root, String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setPadding(0, dp(10), 0, dp(2));
        root.addView(tv);
        EditText et = new EditText(this);
        et.setSingleLine(true);
        root.addView(et, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return et;
    }

    private String def(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private void refreshUi() {
        mEnableSwitch.setChecked(mConfig.enable);
        mImeiEdit.setText(def(mConfig.deviceId, ""));
        mAndroidIdEdit.setText(def(mConfig.androidId, androidId()));
        mImsiEdit.setText(def(mConfig.iccId, ""));
        mWifiMacEdit.setText(def(mConfig.wifiMac, ""));
        mBtMacEdit.setText(def(mConfig.bluetoothMac, ""));
        mSerialEdit.setText(def(mConfig.serial, Build.SERIAL));
        String[] fallbacks = {Build.BRAND, Build.MODEL, Build.PRODUCT, Build.DEVICE, Build.BOARD,
                Build.DISPLAY, Build.ID, Build.MANUFACTURER, Build.FINGERPRINT};
        for (int i = 0; i < PROP_KEYS.length; i++) {
            mPropEdits.get(i).setText(def(mConfig.getProp(PROP_KEYS[i]), fallbacks[i]));
        }
    }

    private String androidId() {
        try {
            String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            return id == null ? "" : id;
        } catch (Throwable e) {
            return "";
        }
    }

    private void onRandom() {
        VDeviceConfig random = VDeviceConfig.random();
        random.enable = true;
        mEnableSwitch.setChecked(true);
        for (int i = 0; i < PROP_KEYS.length; i++) {
            random.setProp(PROP_KEYS[i], mConfig.getProp(PROP_KEYS[i]));
        }
        mConfig = random;
        refreshUi();
        Toast.makeText(this, "已随机生成，点保存才生效", Toast.LENGTH_SHORT).show();
    }

    private String textOf(EditText et) {
        CharSequence cs = et.getText();
        if (cs == null) return null;
        String s = cs.toString().trim();
        return s.length() == 0 ? null : s;
    }

    private void onSave() {
        mConfig.enable = mEnableSwitch.isChecked();
        mConfig.deviceId = textOf(mImeiEdit);
        mConfig.androidId = textOf(mAndroidIdEdit);
        mConfig.iccId = textOf(mImsiEdit);
        mConfig.wifiMac = textOf(mWifiMacEdit);
        mConfig.bluetoothMac = textOf(mBtMacEdit);
        mConfig.serial = textOf(mSerialEdit);
        for (int i = 0; i < PROP_KEYS.length; i++) {
            String v = textOf(mPropEdits.get(i));
            if (v == null) {
                mConfig.buildProp.remove(PROP_KEYS[i]);
            } else {
                mConfig.setProp(PROP_KEYS[i], v);
            }
        }
        try {
            if (VDeviceManager.get().getService() == null) {
                Toast.makeText(this, "设备服务不可用，保存失败", Toast.LENGTH_LONG).show();
                return;
            }
            VDeviceManager.get().updateDeviceConfig(mUserId, mConfig);
            VDeviceConfig check = VDeviceManager.get().getDeviceConfig(mUserId);
            if (check == null || check.enable != mConfig.enable
                        || !TextUtils.equals(check.deviceId, mConfig.deviceId)) {
                Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Throwable e) {
            Toast.makeText(this, "保存失败", Toast.LENGTH_LONG).show();
            return;
        }
        setResult(RESULT_OK);
        Toast.makeText(this, "已保存，重启分身后生效", Toast.LENGTH_SHORT).show();
        finish();
    }
}
