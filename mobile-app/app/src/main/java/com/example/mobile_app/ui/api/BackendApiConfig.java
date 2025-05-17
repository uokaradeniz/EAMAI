package com.example.mobile_app.ui.api;

import com.example.mobile_app.ui.utils.DeviceUtils;

public class BackendApiConfig {
    public static final String URL_PHYSICAL = "http://34.40.67.221:8080";
    public static String currentUrl = URL_PHYSICAL;
    public static boolean isEmulator = DeviceUtils.isEmulator();
    public static boolean isAuthenticated = false;
    public static Long companyId;
}
