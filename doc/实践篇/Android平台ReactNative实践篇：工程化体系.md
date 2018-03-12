# Android平台ReactNative实践篇：工程化体系

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

大搜车的SRN工程体系已经全面铺设到公司的各个应用中，本文以公司母应用大风车为例来分析我们在RN工程化体系相关的实践经验。

大风车：http://fengche.souche.com/

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/dafengche_banner.png"/>

## 应用架构

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/rn_structure.png"/>

从上到下我们将RN项目体系分为了四层，如下所示：

- React业务层：实现相应的业务逻辑，React业务层我们也提供了很多工具，包括SRNPage/SRNStore/SRNConfig为一体的统一项目架构方案，以及使用Native能力的SRNNative、捕获异常信息的Sentry以及管理RN Bundle版本策略的SRNHub。
- React Native Android框架层：RN自带的Framework层，我们也对其做了封装和优化，简化了开发流程，提升了业务开发的效率。
- Router/React层：这一层主要用来向RN提供一些Native能力以及执行页面的跳转等功能。
- Android Framework层：主要为RN提供了许多Native组件，包含各种Widget、图片/视频库支持、埋点工具、分享工具等。

## 集成方案

## 热更新方案