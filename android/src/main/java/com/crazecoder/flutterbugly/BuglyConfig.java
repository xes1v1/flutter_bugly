package com.crazecoder.flutterbugly;

import android.content.Context;

import androidx.annotation.NonNull;

import com.tencent.bugly.BuglyStrategy;

public class BuglyConfig {
    public Context context;
    public BuglyStrategy strategy;

    public BuglyConfig(@NonNull Context context, BuglyStrategy strategy) {
        this.context = context;
        this.strategy = strategy;
    }

    public BuglyConfig(@NonNull Context context) {
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(@NonNull Context context) {
        this.context = context;
    }

    public BuglyStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(BuglyStrategy strategy) {
        this.strategy = strategy;
    }
}
