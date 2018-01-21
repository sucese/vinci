package com.guoxiaoxing.vinci.common;

import com.facebook.react.ReactPackage;

import java.util.List;

/**
 * The config for Vinci
 * <p>
 * For more information, you can visit https://github.com/guoxiaoxing or contact me by
 * guoxiaoxingse@163.com.
 *
 * @author guoxiaoxing
 * @since 2017/9/7 下午4:00
 */
public final class VinciConfig {

    private boolean dev;
    private String mainComponentName;
    private List<ReactPackage> reactPackageList;

    public boolean isDev() {
        return dev;
    }

    public void setDev(boolean dev) {
        this.dev = dev;
    }

    public String getMainComponentName() {
        return mainComponentName;
    }

    public void setMainComponentName(String mainComponentName) {
        this.mainComponentName = mainComponentName;
    }

    public List<ReactPackage> getReactPackageList() {
        return reactPackageList;
    }

    public void setReactPackageList(List<ReactPackage> reactPackageList) {
        this.reactPackageList = reactPackageList;
    }

    private VinciConfig(Builder builder) {
        dev = builder.dev;
        mainComponentName = builder.mainComponentName;
        reactPackageList = builder.reactPackageList;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    public static final class Builder {
        private boolean dev;
        private String mainComponentName;
        private List<ReactPackage> reactPackageList;

        private Builder() {
        }

        public Builder dev(boolean val) {
            dev = val;
            return this;
        }

        public Builder mainComponentName(String val) {
            mainComponentName = val;
            return this;
        }

        public Builder reactPackageList(List<ReactPackage> val) {
            reactPackageList = val;
            return this;
        }

        public VinciConfig build() {
            return new VinciConfig(this);
        }
    }
}