# Android平台ReactNative实践篇：工程化体系

**关于作者**

>郭孝星，程序员，吉他手，主要从事Android平台基础架构方面的工作，欢迎交流技术方面的问题，可以去我的[Github](https://github.com/guoxiaoxing)提issue或者发邮件至guoxiaoxingse@163.com与我交流。

**文章目录**

- 一 应用架构
- 二 集成方案
- 三 热更新方案

大搜车的SRN工程体系已经全面铺设到公司的各个应用中，本文以公司母应用大风车为例来分析我们在RN工程化体系相关的实践经验。

大风车：http://fengche.souche.com/

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/dafengche_banner.png"/>

## 一 应用架构

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/rn_structure.png"/>

从上到下我们将RN项目体系分为了四层，如下所示：

- React业务层：实现相应的业务逻辑，React业务层我们也提供了很多工具，包括SRNPage/SRNStore/SRNConfig为一体的统一项目架构方案，以及使用Native能力的SRNNative、捕获异常信息的Sentry以及管理RN Bundle版本策略的SRNHub。
- React Native Android框架层：RN自带的Framework层，我们也对其做了封装和优化，简化了开发流程，提升了业务开发的效率。
- Router/React层：这一层主要用来向RN提供一些Native能力以及执行页面的跳转等功能。
- Android Framework层：主要为RN提供了许多Native组件，包含各种Widget、图片/视频库支持、埋点工具、分享工具等。

## 二 集成方案

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/package_structure.png"/>

## 三 热更新方案

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/hotfix_structure.png"/>

热更新流程如下所示：

1.	版本号表意不变，${major}.${feature}.${patch}
2.	srnhub服务中的热更新逻辑切换为 ${major} 版本变更不热更新，${feature} 版本变更自动热更新，${patch} 版本变更自动热更新。
3.	发布的脚手架（新版的jenkins）中，对应的变更描述，
a.	${major} 描述为”不兼容老版本的发布（新增原生协议依赖或者升级底层库的版本）”
b.	${feature} 描述为 “兼容老版本的功能迭代”
c.	${patch} 描述为“兼容老版本的bugfix”
4.	app发版时，逻辑不变，仍然集成最新版的 RN bundle。后续业务多了之后可能会考虑不走集成，都走线上热更新。
5.	如果在某个时间更新了app版本，升级了RN底层（不兼容老版），此时新的业务包需要升级大版本，之后在老的app上凭空打开此业务，也会下载新的包，会报错，不过如果老的app打开过一次此业务，就不会更新到最新版本的包了（因为major版本升级了）
6.	为了解决上述问题，底层RN包和脚手架统一版本，然后在bundle中内置一段位置用来存储底层版本号，打开业务的时候作对比，如果对不上，出现一个提示界面（请升级app版本再使用此功能）
7.	需要跟产品强调，新增业务需要发版，不要走动态入口，另外测试环境要控制住，不能有动态入口，所有动态入口都需要在下次发布之前补上依赖的声明。
