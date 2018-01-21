# React Native for Android原理分析与实践：实现原理

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

从2016年中开始，我司开始筹措推进React Native在全公司的推广使用，从最基础的基础框架搭建开始，到各种组件库、开发工具的完善，经历了诸多波折，也累积了很多经验。今年的工作也
马上接近尾声，打算写几篇文章来对这一年多的实践经验做个总结。读者有什么问题或者想要交流的地方，可以去[vinci](https://github.com/guoxiaoxing/vinci)提issue。

预先善其事，必先利其器。开篇第一篇文章我们还是从React Native实现原理讲起，这样过渡到后续的内容，才更加容易理解。

源码地址：https://github.com/facebook/react-native

源码版本：[![Build Status](https://travis-ci.org/facebook/react-native.svg?branch=master)](https://travis-ci.org/facebook/react-native) [![Circle CI](https://circleci.com/gh/facebook/react-native.svg?style=shield)](https://circleci.com/gh/facebook/react-native) [![npm version](https://badge.fury.io/js/react-native.svg)](https://badge.fury.io/js/react-native)

## 一 原理概览

当你拿到React Native的源码的时候，它的目录结构

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

- Java层：该层主要提供了

>注：JSCore，即JavaScriptCore，JS解析的核心部分，IOS使用的是内置的JavaScriptCore，Androis上使用的是https://webkit.org/家的jsc.so。