package com.lody.virtual.client.ipc;

import com.lody.virtual.helper.utils.IInterfaceUtils;
import com.lody.virtual.helper.utils.Reflect;
import com.lody.virtual.helper.utils.ReflectException;
import com.lody.virtual.remote.VDeviceConfig;
import com.lody.virtual.server.interfaces.IDeviceManager;

import java.util.Map;

import mirror.android.os.Build;

/**
 * @author Lody
 */

public class VDeviceManager {

    private static final VDeviceManager sInstance = new VDeviceManager();

    public static VDeviceManager get() {
        return sInstance;
    }

    private IDeviceManager mService;

    public IDeviceManager getService() {
        if (!IInterfaceUtils.isAlive(mService)) {
            synchronized (this) {
                Object binder = getRemoteInterface();
                if (binder == null) {
                    return null;
                }
                mService = LocalProxyUtils.genProxy(IDeviceManager.class, binder);
            }
        }
        return mService;
    }

    private Object getRemoteInterface() {
        return IDeviceManager.Stub
                .asInterface(ServiceManagerNative.getService(ServiceManagerNative.DEVICE));
    }


    public VDeviceConfig getDeviceConfig(int userId) {
        try {
            IDeviceManager service = getService();
            if (service == null) {
                return new VDeviceConfig();
            }
            VDeviceConfig config = service.getDeviceConfig(userId);
            return config != null ? config : new VDeviceConfig();
        } catch (Throwable e) {
            return new VDeviceConfig();
        }
    }

    public void updateDeviceConfig(int userId, VDeviceConfig config) {
        try {
            IDeviceManager service = getService();
            if (service != null) {
                service.updateDeviceConfig(userId, config);
            }
        } catch (Throwable ignored) {
        }
    }

    public boolean isEnable(int userId) {
        try {
            IDeviceManager service = getService();
            return service != null && service.isEnable(userId);
        } catch (Throwable e) {
            return false;
        }
    }

    public void setEnable(int userId, boolean enable) {
        try {
            IDeviceManager service = getService();
            if (service != null) {
                service.setEnable(userId, enable);
            }
        } catch (Throwable ignored) {
        }
    }

    public void applyBuildProp(VDeviceConfig config) {
        for (Map.Entry<String, String> entry : config.buildProp.entrySet()) {
            try {
                Reflect.on(Build.TYPE).set(entry.getKey(), entry.getValue());
            } catch (ReflectException e) {
                e.printStackTrace();
            }
        }
        if (config.serial != null) {
            Reflect.on(Build.TYPE).set("SERIAL", config.serial);
        }
    }
}
