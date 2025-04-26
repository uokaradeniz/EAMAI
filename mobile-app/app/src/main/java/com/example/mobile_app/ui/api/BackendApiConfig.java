package com.example.mobile_app.ui.api;

import com.example.mobile_app.ui.utils.DeviceUtils;

public class BackendApiConfig {
    public static final String URL_VIRTUAL = "http://10.0.2.2:8080";
    public static final String URL_PHYSICAL = "http://";
    public static String currentUrl = URL_PHYSICAL;
    public static boolean isEmulator = DeviceUtils.isEmulator();
    public static boolean customDomain = true;
}
