# React Native for Android原理分析与实践：实现原理

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

从2016年中开始，我司开始筹措推进React Native在全公司的推广使用，从最基础的基础框架搭建开始，到各种组件库、开发工具的完善，经历了诸多波折，也累积了很多经验。今年的工作也
马上接近尾声，打算写几篇文章来对这一年多的实践经验做个总结。读者有什么问题或者想要交流的地方，可以去[vinci](https://github.com/guoxiaoxing/vinci)提issue。

预先善其事，必先利其器。开篇第一篇文章我们还是从React Native实现原理讲起，事实上原理分析的文章之前就有写过，但是鉴于最新的版本0.52.0源码有
不少的改动，我们就再重新温习一下，这样过渡到后续的内容，才更加容易理解。

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

### 2.1 创建ReactInstanceManager

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

### 2.2 创建ReactContext

我们再来看第二个问题，ReactContext创建流程序列图如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_context_start_sequence.png"/>

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

### 2.3 加载JS Bundle

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

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/js_budle_load_sequence.png"/>

注：JS Bundle有三种加载方式：

- setSourceURLs(String deviceURL, String remoteURL) ：从远程服务器加载。
- loadScriptFromAssets(AssetManager assetManager, String assetURL, boolean loadSynchronously)：从Assets文件夹加载。
- loadScriptFromFile(String fileName, String sourceURL, boolean loadSynchronously)：从文件路径加载。

从这个序列图上我们可以看出，真正加载执行JS的地方就是JSCExector.cpp的loadApplicationScript()方法。

```java
  void JSCExecutor::loadApplicationScript(std::unique_ptr<const JSBigString> script, std::string sourceURL) {
      SystraceSection s("JSCExecutor::loadApplicationScript",
                        "sourceURL", sourceURL);
        ...
        switch (jsStatus) {
          case JSLoadSourceIsCompiled:
            if (!bcSourceCode) {
              throw std::runtime_error("Unexpected error opening compiled bundle");
            }
            //调用JavaScriptCore里的方法验证JS是否有效，并解释执行
            evaluateSourceCode(m_context, bcSourceCode, jsSourceURL);

            flush();

            ReactMarker::logMarker(ReactMarker::CREATE_REACT_CONTEXT_STOP);
            ReactMarker::logTaggedMarker(ReactMarker::RUN_JS_BUNDLE_STOP, scriptName.c_str());
            return;

          case JSLoadSourceErrorVersionMismatch:
            throw RecoverableError(explainLoadSourceStatus(jsStatus));

          case JSLoadSourceErrorOnRead:
          case JSLoadSourceIsNotCompiled:
            // Not bytecode, fall through.
            break;
        }
      }
     ...
    
```
可以看到这个方法主要是调用JavaScriptCore里的evaluateSourceCode()方法验证JS是否有效，并解释执行。然后在调用flush()方法层调用JS层的里
方法执行JS Bundle。

```java
    void JSCExecutor::flush() {
      SystraceSection s("JSCExecutor::flush");

      if (m_flushedQueueJS) {
          //调用MessageQueue.js的flushedQueue()方法
        callNativeModules(m_flushedQueueJS->callAsFunction({}));
        return;
      }

      // When a native module is called from JS, BatchedBridge.enqueueNativeCall()
      // is invoked.  For that to work, require('BatchedBridge') has to be called,
      // and when that happens, __fbBatchedBridge is set as a side effect.
      auto global = Object::getGlobalObject(m_context);
      auto batchedBridgeValue = global.getProperty("__fbBatchedBridge");
      // So here, if __fbBatchedBridge doesn't exist, then we know no native calls
      // have happened, and we were able to determine this without forcing
      // BatchedBridge to be loaded as a side effect.
      if (!batchedBridgeValue.isUndefined()) {
        // If calls were made, we bind to the JS bridge methods, and use them to
        // get the pending queue of native calls.
        bindBridge();
        callNativeModules(m_flushedQueueJS->callAsFunction({}));
      } else if (m_delegate) {
        // If we have a delegate, we need to call it; we pass a null list to
        // callNativeModules, since we know there are no native calls, without
        // calling into JS again.  If no calls were made and there's no delegate,
        // nothing happens, which is correct.
        callNativeModules(Value::makeNull(m_context));
      }
    }
```

上面提到flush()方法调用MessageQueue.js的flushedQueue()方法，这是如何做到的呢？🤔。

事实上这是由bindBridge()方法来完成的，bindBridge()从__fbBatchedBridge（__fbBatchedBridge也是被MessageQueue.js设置为全局变量，供C++层读取）对象中取出MessageQueue.js里的四个方法：

- callFunctionReturnFlushedQueue()
- invokeCallbackAndReturnFlushedQueue()
- flushedQueue()
- callFunctionReturnResultAndFlushedQueue()

并分别存在三个C++变量中：

- m_callFunctionReturnFlushedQueueJS
- m_invokeCallbackAndReturnFlushedQueueJS
- m_flushedQueueJS
- m_callFunctionReturnResultAndFlushedQueueJS

这样C++就可以调用这四个JS方法。

```java
void JSCExecutor::bindBridge() throw(JSException) {
  SystraceSection s("JSCExecutor::bindBridge");
  std::call_once(m_bindFlag, [this] {
    auto global = Object::getGlobalObject(m_context);
    auto batchedBridgeValue = global.getProperty("__fbBatchedBridge");
    if (batchedBridgeValue.isUndefined()) {
      auto requireBatchedBridge = global.getProperty("__fbRequireBatchedBridge");
      if (!requireBatchedBridge.isUndefined()) {
        batchedBridgeValue = requireBatchedBridge.asObject().callAsFunction({});
      }
      if (batchedBridgeValue.isUndefined()) {
        throw JSException("Could not get BatchedBridge, make sure your bundle is packaged correctly");
      }
    }

    auto batchedBridge = batchedBridgeValue.asObject();
    m_callFunctionReturnFlushedQueueJS = batchedBridge.getProperty("callFunctionReturnFlushedQueue").asObject();
    m_invokeCallbackAndReturnFlushedQueueJS = batchedBridge.getProperty("invokeCallbackAndReturnFlushedQueue").asObject();
    m_flushedQueueJS = batchedBridge.getProperty("flushedQueue").asObject();
    m_callFunctionReturnResultAndFlushedQueueJS = batchedBridge.getProperty("callFunctionReturnResultAndFlushedQueue").asObject();
  });
}
```

至此，JS Bundle已经加载解析完成，进入MessageQueue.js开始执行。

### 2.4 绑定ReactContext与ReactRootView

JS Bundle加载完成以后，前面说的createReactContext()就执行完成了，然后开始执行setupReacContext()方法，绑定ReactContext与ReactRootView。
我们来看一下它的实现。

```java
public class ReactInstanceManager {
    
    private void setupReactContext(final ReactApplicationContext reactContext) {
        //...
        
        //执行Java Module的初始化
        catalystInstance.initialize();
        //通知ReactContext已经被创建爱女
        mDevSupportManager.onNewReactContextCreated(reactContext);
        //内存状态回调设置
        mMemoryPressureRouter.addMemoryPressureListener(catalystInstance);
        //复位生命周期
        moveReactContextToCurrentLifecycleState();
    
        ReactMarker.logMarker(ATTACH_MEASURED_ROOT_VIEWS_START);
        synchronized (mAttachedRootViews) {
          //将所有的ReactRootView与catalystInstance进行绑定
          for (ReactRootView rootView : mAttachedRootViews) {
            attachRootViewToInstance(rootView, catalystInstance);
          }
        }
        //...
      }
    
      private void attachRootViewToInstance(
          final ReactRootView rootView,
          CatalystInstance catalystInstance) {
        //...
        UIManagerModule uiManagerModule = catalystInstance.getNativeModule(UIManagerModule.class);
        //将ReactRootView作为根布局
        final int rootTag = uiManagerModule.addRootView(rootView);
        rootView.setRootViewTag(rootTag);
        //执行JS的入口bundle页面
        rootView.invokeJSEntryPoint();
        //...
      }
x
}
```

setupReactContext()方法主要完成每个ReactRootView与catalystInstance的绑定，绑定的过程主要做两件事情:

1. 将ReactRootView作为根布局.
2. 执行JS的入口bundle页面.

JS的页面入口我们可以设置mJSEntryPoint来自定义入口，如果不设置则是默认的入口AppRegistry。

```java
  private void defaultJSEntryPoint() {
      //...
      try {
        //...
        String jsAppModuleName = getJSModuleName();
        catalystInstance.getJSModule(AppRegistry.class).runApplication(jsAppModuleName, appParams);
      } finally {
        Systrace.endSection(TRACE_TAG_REACT_JAVA_BRIDGE);
      }
  }
```

这里的调用方式实际上就是原生调用JS的方法，它调用的正是我们很熟悉的AppRegistry.js，AppRegistry.js调用runApplication()开始执行JS页面的渲染，最终转换为
Native UI显示在手机上。

到此为止，整个RN页面的启动流程就分析完了，我们接着来看看RN页面是如何渲染最终显示在手机上的。

## 三 渲染原理

上面我们也说了，RN页面的入口一般是AppRegistry.js，我们就从这个页面入手开始分析RN页面的渲染流程。先看一下RN页面的渲染流程序列图，如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/render_sequence.png"/>

这个流程比较长，其实主要是方法的调用链多，原理还是很简单的，我们先概括性的总结一下：

1. React Native将代码由JSX转化为JS组件，启动过程中利用instantiateReactComponent将ReactElement转化为复合组件ReactCompositeComponent与元组件ReactNativeBaseComponent，利用
ReactReconciler对他们进行渲染。
2. UIManager.js利用C++层的Instance.cpp将UI信息传递给UIManagerModule.java，并利用UIManagerModule.java构建UI。
3. UIManagerModule.java接收到UI信息后，将UI的操作封装成对应的Action，放在队列中等待执行。各种UI的操作，例如创建、销毁、更新等便在队列里完成，UI最终
得以渲染在屏幕上。

### 3.1 JavaScript层组件渲染

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_render_principle_flow.png"/>

如上图所示AppRegistry.registerComponent用来注册组件，在该方法内它会调用AppRegistry.runApplication()来启动js的渲染流程。AppRegistry.runApplication()
会将传入的Component转换成ReactElement，并在外面包裹一层AppContaniner，AppContaniner主要用来提供一些debug工具（例如：红盒）。

如下所示：

```javascript
function renderApplication<Props: Object>(
  RootComponent: ReactClass<Props>,
  initialProps: Props,
  rootTag: any
) {
  invariant(
    rootTag,
    'Expect to have a valid rootTag, instead got ', rootTag
  );
  ReactNative.render(
    <AppContainer rootTag={rootTag}>
      <RootComponent
        {...initialProps}
        rootTag={rootTag}
      />
    </AppContainer>,
    rootTag
  );
}
```

我们抛开函数调用链，分析其中关键的部分，其他部分都是简单的函数调用。

- ReactNativeMount.renderComponent()
- instantiateReactComponent.instantiateReactComponent(node, shouldHaveDebugID)

#### ReactNativeMount.renderComponent()

```javascript
  /**
   * @param {ReactComponent} instance Instance to render.
   * @param {containerTag} containerView Handle to native view tag
   */
  renderComponent: function(
    nextElement: ReactElement<*>,
    containerTag: number,
    callback?: ?(() => void)
  ): ?ReactComponent<any, any, any> {
  
    //将RectElement使用相同的TopLevelWrapper进行包裹
    var nextWrappedElement = React.createElement(
      TopLevelWrapper,
      { child: nextElement }
    );

    var topRootNodeID = containerTag;
    var prevComponent = ReactNativeMount._instancesByContainerID[topRootNodeID];
    if (prevComponent) {
      var prevWrappedElement = prevComponent._currentElement;
      var prevElement = prevWrappedElement.props.child;
      if (shouldUpdateReactComponent(prevElement, nextElement)) {
        ReactUpdateQueue.enqueueElementInternal(prevComponent, nextWrappedElement, emptyObject);
        if (callback) {
          ReactUpdateQueue.enqueueCallbackInternal(prevComponent, callback);
        }
        return prevComponent;
      } else {
        ReactNativeMount.unmountComponentAtNode(containerTag);
      }
    }

    if (!ReactNativeTagHandles.reactTagIsNativeTopRootID(containerTag)) {
      console.error('You cannot render into anything but a top root');
      return null;
    }

    ReactNativeTagHandles.assertRootTag(containerTag);

    //检查之前的节点是否已经mount到目标节点上，如果有则进行比较处理
    var instance = instantiateReactComponent(nextWrappedElement, false);
    ReactNativeMount._instancesByContainerID[containerTag] = instance;

    // The initial render is synchronous but any updates that happen during
    // rendering, in componentWillMount or componentDidMount, will be batched
    // according to the current batching strategy.

    //将mount任务提交给回调Queue，最终会调用ReactReconciler.mountComponent()
    ReactUpdates.batchedUpdates(
      batchedMountComponentIntoNode,
      instance,
      containerTag
    );
    var component = instance.getPublicInstance();
    if (callback) {
      callback.call(component);
    }
    return component;
  },
```
该方法主要做了以下事情：

1. 将传入的RectElement使用相同的TopLevelWrapper进行包裹，生成nextWrappedElement。
2. 检查之前的节点是否已经mount到目标节点上，如果有则进行比较处理，将上一步生成的nextWrappedElement传入instantiateReactComponent(nextWrappedElement, false)方法。
3. 将mount任务提交给回调Queue，最终会调用ReactReconciler.mountComponent()，ReactReconciler.mountComponent()又会去调用C++层Instance::mountComponent()
方法。

#### instantiateReactComponent.instantiateReactComponent(node, shouldHaveDebugID)

在分析这个函数之前，我们先来补充一下React组件相关知识。React组件可以分为两种：

- 元组件：框架内置的，可以直接使用的组件。例如：View、Image等。它在React Native中用ReactNativeBaseComponent来描述。
- 复合组件：用户封装的组件，一般可以通过React.createClass()来构建，提供render()方法来返回渲染目标。它在React Native中用ReactCompositeComponent来描述。

instantiateReactComponent(node, shouldHaveDebugID)方法根据对象的type生成元组件或者复合组件。

```javascript
/**
 * Given a ReactNode, create an instance that will actually be mounted.
 *
 * @param {ReactNode} node
 * @param {boolean} shouldHaveDebugID
 * @return {object} A new instance of the element's constructor.
 * @protected
 */
function instantiateReactComponent(node, shouldHaveDebugID) {
  var instance;

  if (node === null || node === false) {
    instance = ReactEmptyComponent.create(instantiateReactComponent);
  } else if (typeof node === 'object') {
    var element = node;
    var type = element.type;

    if (typeof type !== 'function' && typeof type !== 'string') {
      var info = '';
      if (process.env.NODE_ENV !== 'production') {
        if (type === undefined || typeof type === 'object' && type !== null && Object.keys(type).length === 0) {
          info += ' You likely forgot to export your component from the file ' + 'it\'s defined in.';
        }
      }
      info += getDeclarationErrorAddendum(element._owner);
      !false ? process.env.NODE_ENV !== 'production' ? invariant(false, 'Element type is invalid: expected a string (for built-in components) or a class/function (for composite components) but got: %s.%s', type == null ? type : typeof type, info) : _prodInvariant('130', type == null ? type : typeof type, info) : void 0;
    }

    //如果对象的type为string，则调用ReactHostComponent.createInternalComponent(element)来注入生成组件的逻辑
    if (typeof element.type === 'string') {
      instance = ReactHostComponent.createInternalComponent(element);
    }
    //如果是内部元组件，则创建一个type实例
    else if (isInternalComponentType(element.type)) {
      // This is temporarily available for custom components that are not string
      // representations. I.e. ART. Once those are updated to use the string
      // representation, we can drop this code path.
      instance = new element.type(element);

      // We renamed this. Allow the old name for compat. :(
      if (!instance.getHostNode) {
        instance.getHostNode = instance.getNativeNode;
      }
    } 
    //否则，则是用户创建的复合组件，这个时候创建一个ReactCompositeComponentWrapper实例，该实例用来描述复合组件
    else {
      instance = new ReactCompositeComponentWrapper(element);
    }
    //当对象为string或者number时，调用ReactHostComponent.createInstanceForText(node)来注入组件生成逻辑。
  } else if (typeof node === 'string' || typeof node === 'number') {
    instance = ReactHostComponent.createInstanceForText(node);
  } else {
    !false ? process.env.NODE_ENV !== 'production' ? invariant(false, 'Encountered invalid React node of type %s', typeof node) : _prodInvariant('131', typeof node) : void 0;
  }

  if (process.env.NODE_ENV !== 'production') {
    process.env.NODE_ENV !== 'production' ? warning(typeof instance.mountComponent === 'function' && typeof instance.receiveComponent === 'function' && typeof instance.getHostNode === 'function' && typeof instance.unmountComponent === 'function', 'Only React Components can be mounted.') : void 0;
  }

  // These two fields are used by the DOM and ART diffing algorithms
  // respectively. Instead of using expandos on components, we should be
  // storing the state needed by the diffing algorithms elsewhere.
  instance._mountIndex = 0;
  instance._mountImage = null;

  if (process.env.NODE_ENV !== 'production') {
    instance._debugID = shouldHaveDebugID ? getNextDebugID() : 0;
  }

  // Internal instances should fully constructed at this point, so they should
  // not get any new fields added to them at this point.
  if (process.env.NODE_ENV !== 'production') {
    if (Object.preventExtensions) {
      Object.preventExtensions(instance);
    }
  }

  return instance;
}
```

该方法根据对象的type生成元组件或者复合组件，具体流程如下：

1. 如果对象的type为string，则调用ReactHostComponent.createInternalComponent(element)来注入生成组件的逻辑，如果是内部元组件，则创建一个type实例，
否则，则是用户创建的复合组件，这个时候创建一个ReactCompositeComponentWrapper实例，该实例用来描述复合组件。
2. 当对象为string或者number时，调用ReactHostComponent.createInstanceForText(node)来注入组件生成逻辑。
3. 以上都不是，则报错。

我们通过前面的分析，了解了整个UI开始渲染的时机，以及js层的整个渲染流程，接下来，我们开始分析每个js的组件时怎么转换成Android的组件，最终显示在屏幕上的。

上面我们提到元组件与复合组件，事实上复合组件也是递归遍历其中的元组件，然后进行渲染。所以我们重点关注元组件的生成逻辑。

我们可以看到，UI渲染主要通过UIManager来完成，UIManager是一个ReactModule，UIManager.js里的操作都会对应到UIManagerModule里来。我们接着来看看Java层的
渲染流程。

### 3.2 Java层组件渲染

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_render_principle_flow.png"/>

从上图我们可以很容易看出，Java层的组件渲染分为以下几步：

1. JS层通过C++层把创建View的请求发送给Java层的UIManagerModule。
2. UIManagerModule通过UIImplentation对操作请求进行包装。
3. 包装后的操作请求被发送到View处理队列UIViewOperationQueue队列中等待处理。
4. 实际处理View时，根据class name查询对应的ViewNManager，然后调用原生View的方法对View进行相应的操作。

## 四 通信机制

Java层与JavaScript层的相互调用都不是直接完成的，而是间接通过C++层来完成的。在介绍通信机制之前我们先来理解一些基本的概念。

**JavaScript Module注册表**

说起JavaScript Module注册表，我们需要先理解3个类/接口：JavaScriptModule、JavaScriptModuleRegistration、JavaScriptModuleRegistry。

- JavaScriptModule：这是一个接口，JS Module都会继承此接口，它表示在JS层会有一个相同名字的js文件，该js文件实现了该接口定义的方法，JavaScriptModuleRegistry会利用
动态代理将这个接口生成代理类，并通过C++传递给JS层，进而调用JS层的方法。
- JavaScriptModuleRegistration用来描述JavaScriptModule的相关信息，它利用反射获取接口里定义的Method。
- JavaScriptModuleRegistry：JS Module注册表，内部维护了一个HashMap：HashMap<Class<? extends JavaScriptModule>, JavaScriptModuleRegistration> mModuleRegistrations，
JavaScriptModuleRegistry利用动态代理生成接口JavaScriptModule对应的代理类，再通过C++传递到JS层，从而调用JS层的方法。

**Java Module注册表**

要理解Java Module注册表，我们同样也需要理解3个类/接口：NativeModule、ModuleHolder、NativeModuleRegistry。

- NativeModule：是一个接口，实现了该接口则可以被JS层调用，我们在为JS层提供Java API时通常会继承BaseJavaModule/ReactContextBaseJavaModule，这两个类就
实现了NativeModule接口。
- ModuleHolder：NativeModule的一个Holder类，可以实现NativeModule的懒加载。
- NativeModuleRegistry：Java Module注册表，内部持有Map：Map<Class<? extends NativeModule>, ModuleHolder> mModules，NativeModuleRegistry可以遍历
并返回Java Module供调用者使用。

### 4.1 创建注册表

关于NativeModuleRegistry和JavaScriptModuleRegistry的创建，我们前面都已经提到管，大家还都记得吗。

- NativeModuleRegistry是在createReactContext()方法里构建的。
- JavaScriptModuleRegistry是在CatalystInstanceImpl的构建方法里构建的。

这些都是在CatalystInstanceImpl的构建方法里通过native方法initializeBridge()传入了C++层，如下所示：

**CatalystInstanceImpl.cpp**

```java
void CatalystInstanceImpl::initializeBridge(
    jni::alias_ref<ReactCallback::javaobject> callback,
    // This executor is actually a factory holder.
    JavaScriptExecutorHolder* jseh,
    jni::alias_ref<JavaMessageQueueThread::javaobject> jsQueue,
    jni::alias_ref<JavaMessageQueueThread::javaobject> moduleQueue,
    jni::alias_ref<jni::JCollection<JavaModuleWrapper::javaobject>::javaobject> javaModules,
    jni::alias_ref<jni::JCollection<ModuleHolder::javaobject>::javaobject> cxxModules) {


  instance_->initializeBridge(folly::make_unique<JInstanceCallback>(callback),
                              jseh->getExecutorFactory(),
                              folly::make_unique<JMessageQueueThread>(jsQueue),
                              folly::make_unique<JMessageQueueThread>(moduleQueue),
                              buildModuleRegistry(std::weak_ptr<Instance>(instance_),
                                                  javaModules, cxxModules));
}
```

这个方法的参数含义如下所示：

- ReactCallback callback：CatalystInstanceImpl的静态内部类ReactCallback，负责接口回调。
- JavaScriptExecutor jsExecutor：JS执行器，将JS的调用传递给C++层。
- MessageQueueThread jsQueue.getJSQueueThread()：JS线程，通过mReactQueueConfiguration.getJSQueueThread()获得，mReactQueueConfiguration通过ReactQueueConfigurationSpec.createDefault()创建。
- MessageQueueThread moduleQueue：Native线程，通过mReactQueueConfiguration.getNativeModulesQueueThread()获得，mReactQueueConfiguration通过ReactQueueConfigurationSpec.createDefault()创建。
- Collection<JavaModuleWrapper> javaModules：java modules，来源于mJavaRegistry.getJavaModules(this)。
- Collection<ModuleHolder> cxxModules)：c++ modules，来源于mJavaRegistry.getCxxModules()。

我们注意这些传入了两个集合：

- javaModules：传入的是Collection<JavaModuleWrapper> ，JavaModuleWrapper是NativeHolder的一个Wrapper类，它对应了C++层JavaModuleWrapper.cpp，
JS在Java的时候最终会调用到这个类的inovke()方法上。
- cxxModules：传入的是Collection<ModuleHolder> ，ModuleHolder是NativeModule的一个Holder类，可以实现NativeModule的懒加载。
```

这两个集合在CatalystInstanceImpl::initializeBridge()被打包成ModuleRegistry传入Instance.cpp.、，如下所示：

**ModuleRegistryBuilder.cpp**

```java
std::unique_ptr<ModuleRegistry> buildModuleRegistry(
    std::weak_ptr<Instance> winstance,
    jni::alias_ref<jni::JCollection<JavaModuleWrapper::javaobject>::javaobject> javaModules,
    jni::alias_ref<jni::JCollection<ModuleHolder::javaobject>::javaobject> cxxModules) {

  std::vector<std::unique_ptr<NativeModule>> modules;
  for (const auto& jm : *javaModules) {
    modules.emplace_back(folly::make_unique<JavaNativeModule>(winstance, jm));
  }
  for (const auto& cm : *cxxModules) {
    modules.emplace_back(
      folly::make_unique<CxxNativeModule>(winstance, cm->getName(), cm->getProvider()));
  }
  if (modules.empty()) {
    return nullptr;
  } else {
    return folly::make_unique<ModuleRegistry>(std::move(modules));
  }
}
```

打包好的ModuleRegistry通过Instance::initializeBridge()传入到NativeToJsBridge.cpp中，并在NativeToJsBridge的构造方法中传给JsToNativeBridge，以后JS如果调用Java就可以通过
ModuleRegistry来进行调用。

这里的NativeToJsBridge.cpp与JsToNativeBridge.cpp就是Java与JS相互调用的通信桥，我们来看看它们的通信方式。

### 4.2 创建通信桥

关于整个RN的通信机制，可以用一句话来概括：

>JNI作为C++与Java的桥梁，JSC作为C++与JavaScript的桥梁，而C++最终连接了Java与JavaScript。

RN应用通信桥结构图如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_communication_bridge.png"/>

理解了整个通信桥的结构，Java与JS是如何互相掉用的就很清晰了。

#### Java调用JS

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_communication_mechanism_java_to_js_flow.png"/>

#### JS调用Java

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/react_native_communication_mechanism_js_to_java_flow.png"/>
