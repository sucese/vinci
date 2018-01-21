# React Native for Android原理分析与实践：实现原理

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

从2016年中开始，我司开始筹措推进React Native在全公司的推广使用，从最基础的基础框架搭建开始，到各种组件库、开发工具的完善，经历了诸多波折，也累积了很多经验。今年的工作也
马上接近尾声，打算写几篇文章来对这一年多的实践经验做个总结。读者有什么问题或者想要交流的地方，可以去[vinci](https://github.com/guoxiaoxing/vinci)提issue。

预先善其事，必先利其器。开篇第一篇文章我们还是从React Native实现原理讲起，这样过渡到后续的内容，才更加容易理解。

## 一 原理概览

源码地址：https://github.com/facebook/react-native

源码版本：[![Build Status](https://travis-ci.org/facebook/react-native.svg?branch=master)](https://travis-ci.org/facebook/react-native) [![Circle CI](https://circleci.com/gh/facebook/react-native.svg?style=shield)](https://circleci.com/gh/facebook/react-native) [![npm version](https://badge.fury.io/js/react-native.svg)](https://badge.fury.io/js/react-native)

当你拿到React Native的源码的时候，它的目录结构是这样的：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/source_code_structure_package.png"/>

- jni：ReactNative的好多机制都是由C、C++实现的，这部分便是用来载入SO库。
- perftest：测试配置
- proguard：混淆
- quicklog：log输出
- react：ReactNative源码的主要内容，也是我们分析的主要内容。
- systrace：system trace
- yoga：瑜伽？哈哈，并不是，是facebook开源的前端布局引擎

总体来看，整套React Native框架分为三层，如下图所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_system_strcuture.png"/>

- Java层：该层主要提供了Android的UI渲染器UIManager（将JavaScript映射成Android Widget）以及一些其他的功能组件（例如：Fresco、Okhttp）等。
- C++层：该层主要完成了Java与JavaScript的通信以及执行JavaScript代码两件工作。
- JavaScript层：该层提供了各种供开发者使用的组件以及一些工具库。

>注：JSCore，即JavaScriptCore，JS解析的核心部分，IOS使用的是内置的JavaScriptCore，Androis上使用的是https://webkit.org/家的jsc.so。

通过上面的分析，我们理解了React Native的框架结构，除此之外，我们还要理解整套框架里的一些重要角色，如下所示：

- ReactContext：ReactContext继承于ContextWrapper，是ReactNative应用的上下文，通过getContext()去获得，通过它可以访问ReactNative核心类的实现。
- ReactInstanceManager：ReactInstanceManager是ReactNative应用总的管理类，创建ReactContext、CatalystInstance等类，解析ReactPackage生成映射表，并且配合ReactRootView管理View的创建与生命周期等功能。
- CatalystInstance：CatalystInstance是ReactNative应用Java层、C++层、JS层通信总管理类，总管Java层、JS层核心Module映射表与回调，三端通信的入口与桥梁。
- NativeToJsBridge：NativeToJsBridge是Java调用JS的桥梁，用来调用JS Module，回调Java。
- JsToNativeBridge：JsToNativeBridge是JS调用Java的桥梁，用来调用Java Module。
- JavaScriptModule：JavaScriptModule是JS Module，负责JS到Java的映射调用格式声明，由CatalystInstance统一管理。
- NativeModule：NativeModule是ava Module，负责Java到Js的映射调用格式声明，由CatalystInstance统一管理。
- JavascriptModuleRegistry：JavascriptModuleRegistry是JS Module映射表，NativeModuleRegistry是Java Module映射表

以上便是整套框架中关键的角色，值得一提的是，当页面真正渲染出来以后，它实际上还是Native代码，React Native的作用就是把JavaScript代码映射成Native代码以及实现两端
的通信，所以我们在React Native基础框架搭建的过程中，指导思路之一就是弱化Native与RN的边界与区别，让业务开发组感受不到两者的区别，从而简化开发流程。

好，有了对React Native框架的整体理解，我们来继续分析一个RN页面是如何启动并渲染出来的，这也是我们关心的主要问题。后续的基础框架的搭建、JS Bundle分包加载、渲染性能优化
等都会围绕着着一块做文章。

## 二 启动流程


我们知道RN的页面也是依托Activity，React Native框架里有一个ReactActivity，它就是我们RN页面的容器。ReactActivity里有个ReactRootView，正如它的名字那样，它就是
ReactActivity的root View，最终渲染出来的view都会添加到这个ReactRootView上。ReactRootView调用自己的startReactApplication()方法启动了整个RN页面，在启动的过程
中先去创建页面上下文ReactContext，然后再去加载、执行并将JavaScript映射成Native Widget，最终一个RN页面就显示在了用户面前。

整个RN页面的启动流程图如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_start_flow_structure.png"/>

这个流程看起来有点长，但实际上重要的东西并不多，我们当前只需要重点关注两个问题：

1. ReactInstanceManager是如何被创建的，它在创建的时候都初始化了哪些对象？🤔
2. RN页面上下文ReactContext在创建的过程中都做了什么，都初始化了哪些对象？🤔

### 2.1 ReactInstanceManager的创建流程

我们先来看第一个问题，我们都知道要使用RN页面，就需要先初始化一个ReactNativeHost，它是一个抽象类，ReactInstanceManager就是在这个类里被创建的，如下所示：


```java
public abstract class ReactNativeHost {
      protected ReactInstanceManager createReactInstanceManager() {
          
        ReactInstanceManagerBuilder builder = ReactInstanceManager.builder()
          //应用上下文
          .setApplication(mApplication)
          //JSMainModuleP相当于应用首页的js Bundle，可以传递url从服务器拉取js Bundle
          //当然这个只在dev模式下可以使用
          .setJSMainModulePath(getJSMainModuleName())
          //是否开启dev模式
          .setUseDeveloperSupport(getUseDeveloperSupport())
          //红盒的回调
          .setRedBoxHandler(getRedBoxHandler())
          //JS执行器
          .setJavaScriptExecutorFactory(getJavaScriptExecutorFactory())
           //自定义UI实现机制，这个我们一般用不到
          .setUIImplementationProvider(getUIImplementationProvider())
          .setInitialLifecycleState(LifecycleState.BEFORE_CREATE);
    
        //添加我们外面设置的Package
        for (ReactPackage reactPackage : getPackages()) {
          builder.addPackage(reactPackage);
        }
    
        //获取js Bundle的加载路径
        String jsBundleFile = getJSBundleFile();
        if (jsBundleFile != null) {
          builder.setJSBundleFile(jsBundleFile);
        } else {
          builder.setBundleAssetName(Assertions.assertNotNull(getBundleAssetName()));
        }
        return builder.build();
      }
}
```
可以看到该方法就是将我们在外面配置的各种RN参数传递给ReactInstanceManager，我们接着来看看ReactInstanceManager拿到这些参数之后会怎么处理。

```java
public class ReactInstanceManager {
    
    ReactInstanceManager(
          Context applicationContext,
          @Nullable Activity currentActivity,
          @Nullable DefaultHardwareBackBtnHandler defaultHardwareBackBtnHandler,
          JavaScriptExecutorFactory javaScriptExecutorFactory,
          @Nullable JSBundleLoader bundleLoader,
          @Nullable String jsMainModulePath,
          List<ReactPackage> packages,
          boolean useDeveloperSupport,
          @Nullable NotThreadSafeBridgeIdleDebugListener bridgeIdleDebugListener,
          LifecycleState initialLifecycleState,
          UIImplementationProvider uiImplementationProvider,
          NativeModuleCallExceptionHandler nativeModuleCallExceptionHandler,
          @Nullable RedBoxHandler redBoxHandler,
          boolean lazyNativeModulesEnabled,
          boolean lazyViewManagersEnabled,
          boolean delayViewManagerClassLoadsEnabled,
          @Nullable DevBundleDownloadListener devBundleDownloadListener,
          int minNumShakes,
          int minTimeLeftInFrameForNonBatchedOperationMs) {
        Log.d(ReactConstants.TAG, "ReactInstanceManager.ctor()");
        initializeSoLoaderIfNecessary(applicationContext);
    
        DisplayMetricsHolder.initDisplayMetricsIfNotInitialized(applicationContext);
    
        mApplicationContext = applicationContext;
        mCurrentActivity = currentActivity;
        mDefaultBackButtonImpl = defaultHardwareBackBtnHandler;
        mJavaScriptExecutorFactory = javaScriptExecutorFactory;
        mBundleLoader = bundleLoader;
        mJSMainModulePath = jsMainModulePath;
        mPackages = new ArrayList<>();
        mUseDeveloperSupport = useDeveloperSupport;
        mDevSupportManager =
            DevSupportManagerFactory.create(
                applicationContext,
                createDevHelperInterface(),
                mJSMainModulePath,
                useDeveloperSupport,
                redBoxHandler,
                devBundleDownloadListener,
                minNumShakes);
        mBridgeIdleDebugListener = bridgeIdleDebugListener;
        mLifecycleState = initialLifecycleState;
        mMemoryPressureRouter = new MemoryPressureRouter(applicationContext);
        mNativeModuleCallExceptionHandler = nativeModuleCallExceptionHandler;
        mLazyNativeModulesEnabled = lazyNativeModulesEnabled;
        mDelayViewManagerClassLoadsEnabled = delayViewManagerClassLoadsEnabled;
        synchronized (mPackages) {
          PrinterHolder.getPrinter()
              .logMessage(ReactDebugOverlayTags.RN_CORE, "RNCore: Use Split Packages");
          mPackages.add(
              new CoreModulesPackage(
                  this,
                  new DefaultHardwareBackBtnHandler() {
                    @Override
                    public void invokeDefaultOnBackPressed() {
                      ReactInstanceManager.this.invokeDefaultOnBackPressed();
                    }
                  },
                  uiImplementationProvider,
                  lazyViewManagersEnabled,
                  minTimeLeftInFrameForNonBatchedOperationMs));
          if (mUseDeveloperSupport) {
            mPackages.add(new DebugCorePackage());
          }
          mPackages.addAll(packages);
        }
    
        // Instantiate ReactChoreographer in UI thread.
        ReactChoreographer.initialize();
        if (mUseDeveloperSupport) {
          mDevSupportManager.startInspector();
        }
      }
}
```

### 2.2 ReactContext的创建流程

我们再来看第二个问题，ReactContext创建流程序列图如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_start_flow_structure.png"/>

可以发现，最终创建ReactContext是createReactContext()方法，我们来看看它的实现。

```java
public class ReactInstanceManager {
    
    private ReactApplicationContext createReactContext(
         JavaScriptExecutor jsExecutor,
         JSBundleLoader jsBundleLoader) {
       Log.d(ReactConstants.TAG, "ReactInstanceManager.createReactContext()");
       ReactMarker.logMarker(CREATE_REACT_CONTEXT_START);
       //ReactApplicationContext是ReactContext的包装类。
       final ReactApplicationContext reactContext = new ReactApplicationContext(mApplicationContext);
   
       //debug模式里开启异常处理器，就是我们开发中见到的调试工具（红色错误框等）
       if (mUseDeveloperSupport) {
         reactContext.setNativeModuleCallExceptionHandler(mDevSupportManager);
       }
   
       //创建JavaModule注册表
       NativeModuleRegistry nativeModuleRegistry = processPackages(reactContext, mPackages, false);
   
       NativeModuleCallExceptionHandler exceptionHandler = mNativeModuleCallExceptionHandler != null
         ? mNativeModuleCallExceptionHandler
         : mDevSupportManager;
       
       //创建CatalystInstanceImpl的Builder，它是三端通信的管理类
       CatalystInstanceImpl.Builder catalystInstanceBuilder = new CatalystInstanceImpl.Builder()
         .setReactQueueConfigurationSpec(ReactQueueConfigurationSpec.createDefault())
         //JS执行器
         .setJSExecutor(jsExecutor)
         //Java Module注册表
         .setRegistry(nativeModuleRegistry)
         //JS Bundle加载器
         .setJSBundleLoader(jsBundleLoader)
         //Java Exception处理器
         .setNativeModuleCallExceptionHandler(exceptionHandler);
   
       ReactMarker.logMarker(CREATE_CATALYST_INSTANCE_START);
       // CREATE_CATALYST_INSTANCE_END is in JSCExecutor.cpp
       Systrace.beginSection(TRACE_TAG_REACT_JAVA_BRIDGE, "createCatalystInstance");
       final CatalystInstance catalystInstance;
       //构建CatalystInstance实例
       try {
         catalystInstance = catalystInstanceBuilder.build();
       } finally {
         Systrace.endSection(TRACE_TAG_REACT_JAVA_BRIDGE);
         ReactMarker.logMarker(CREATE_CATALYST_INSTANCE_END);
       }
   
       if (mBridgeIdleDebugListener != null) {
         catalystInstance.addBridgeIdleDebugListener(mBridgeIdleDebugListener);
       }
       if (Systrace.isTracing(TRACE_TAG_REACT_APPS | TRACE_TAG_REACT_JS_VM_CALLS)) {
         catalystInstance.setGlobalVariable("__RCTProfileIsProfiling", "true");
       }
       ReactMarker.logMarker(ReactMarkerConstants.PRE_RUN_JS_BUNDLE_START);
       //开启加载执行JS Bundle
       catalystInstance.runJSBundle();
       //关联catalystInstance与reactContext
       reactContext.initializeWithInstance(catalystInstance);
   
       return reactContext;
     } 
}
```

在这个方法里完成了RN页面上下文ReactContext的创建，我们先来看看这个方法的两个入参：

- JSCJavaScriptExecutor jsExecutor：JSCJavaScriptExecutor继承于JavaScriptExecutor，当该类被加载时，它会自动去加载"reactnativejnifb.so"库，并会调用Native方
法initHybrid()初始化C++层RN与JSC通信的框架。
- JSBundleLoader jsBundleLoader：缓存了JSBundle的信息，封装了上层加载JSBundle的相关接口，CatalystInstance通过其简介调用ReactBridge去加载JS文件，不同的场景会创建
不同的加载器，具体可以查看类JSBundleLoader。

可以看到在ReactContext创建的过程中，主要做了以下几件事情：

1. 构建ReactApplicationContext对象，ReactApplicationContext是ReactContext的包装类。
2. 利用jsExecutor、nativeModuleRegistry、jsBundleLoader、exceptionHandler等参数构建CatalystInstance实例，作为以为三端通信的中枢。
3. 调用CatalystInstance的runJSBundle()开始加载执行JS。

另一个重要的角色CatalystInstance出现了，前面我们也说过它是三端通信的中枢。关于通信的具体实现我们会在接下来的通信机制小节来讲述，我们先来接着看JS的加载过程。

### 1.2 JS Bundle的加载流程

在分析JS Bundle的加载流程之前，我们先来看一下上面提到CatalystInstance，它是一个接口，其实现类是CatalystInstanceImpl，我们来看看它的构造方法。

```java
public class CatalystInstanceImpl implements CatalystInstance {

     private CatalystInstanceImpl(
          final ReactQueueConfigurationSpec reactQueueConfigurationSpec,
          final JavaScriptExecutor jsExecutor,
          final NativeModuleRegistry nativeModuleRegistry,
          final JSBundleLoader jsBundleLoader,
          NativeModuleCallExceptionHandler nativeModuleCallExceptionHandler) {
        Log.d(ReactConstants.TAG, "Initializing React Xplat Bridge.");
        mHybridData = initHybrid();
        
        //创建三大线程：UI线程、Native线程与JS线程
        mReactQueueConfiguration = ReactQueueConfigurationImpl.create(
            reactQueueConfigurationSpec,
            new NativeExceptionHandler());
        mBridgeIdleListeners = new CopyOnWriteArrayList<>();
        mNativeModuleRegistry = nativeModuleRegistry;
        //创建JS Module注册表实例，这个在以前的代码版本中是在上面的createReactContext()方法中创建的
        mJSModuleRegistry = new JavaScriptModuleRegistry();
        mJSBundleLoader = jsBundleLoader;
        mNativeModuleCallExceptionHandler = nativeModuleCallExceptionHandler;
        mNativeModulesQueueThread = mReactQueueConfiguration.getNativeModulesQueueThread();
        mTraceListener = new JSProfilerTraceListener(this);
    
        Log.d(ReactConstants.TAG, "Initializing React Xplat Bridge before initializeBridge");
        //在C++层初始化通信桥
        initializeBridge(
          new BridgeCallback(this),
          jsExecutor,
          mReactQueueConfiguration.getJSQueueThread(),
          mNativeModulesQueueThread,
          mNativeModuleRegistry.getJavaModules(this),
          mNativeModuleRegistry.getCxxModules());
        Log.d(ReactConstants.TAG, "Initializing React Xplat Bridge after initializeBridge");
    
        mJavaScriptContextHolder = new JavaScriptContextHolder(getJavaScriptContext());
      }          
}
```

这个函数的入参大部分我们都已经很熟悉了，我们单独说说这个ReactQueueConfigurationSpec，它用来创建ReactQueueConfiguration的实例，ReactQueueConfiguration
同样是个接口，它的实现类是ReactQueueConfigurationImpl。

ReactQueueConfiguration的定义如下：

```java
public interface ReactQueueConfiguration {
  //UI线程
  MessageQueueThread getUIQueueThread();
  //Native线程
  MessageQueueThread getNativeModulesQueueThread();
  //JS线程
  MessageQueueThread getJSQueueThread();
  void destroy();
}
```

可以看着这个接口的作用就是创建三个带消息队列的线程：

- UI线程：Android的UI线程，处理和UI相关的事情。
- Native线程：主要是完成通信的工作。
- JS线程：主要完成JS的执行和渲染工作。

可以看到CatalystInstance对象在构建的时候，主要做了两件事情：

1. 创建三大线程：UI线程、Native线程与JS线程。
2. 在C++层初始化通信桥。

我们接着来看JS Bundle的加载流程，JS Bundle的加载实际上是指C++层完成的，我们看一下序列图。




## 三 渲染原理

## 四 通信机制