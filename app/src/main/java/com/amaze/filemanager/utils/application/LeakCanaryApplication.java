package com.amaze.filemanager.utils.application;

import androidx.multidex.MultiDexApplication;

/**
 * @author Emmanuel
 *         on 28/8/2017, at 18:12.
 */

public class LeakCanaryApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
    }

}