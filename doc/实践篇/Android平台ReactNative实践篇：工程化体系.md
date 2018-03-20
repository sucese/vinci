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

我们的RN集成方案是采用强依赖的方式进行的，在进行集成方案的设计之前我们主要考虑两个方面的问题：

- 客户端版本和RN业务版本强强依赖，特定的客户端版本会锁定特定的RN业务版本，新业务发布需要走客户端发版。
- 提供热更新能力，但是只是用来做hotfix，修复bug。

我们提出这两个点是基于我们业务特点来的，我们的母应用大风车是面向B端的产品，它的特点是重交互而轻营销，它需要的是良好的稳定性，而不是随时变化的能力。
而我们团队引入RN也不是为了代替H5，毕竟H5天然具有随时变化的能力，而且开发效率和性能表现也还不错。我们引入RN最大的目的就在于减少客户端double的人力
成本，以满足业务和团队快速扩张的需求。

具体的打包和集成方案如下所示：

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/package_structure.png"/>

我们来说一下我们为什么采用强依赖，主要有以下几个方面的原因：

- 跨端依赖：RN中会大量依赖客户端的功能，RN业务已经内嵌到公司的四个主要App上，页面的跳转也是通过协议进行的，如果RN业务更新了某个协议，而这个客户端又不支持就
会出现问题。
- Native升级：RN中依赖了许多Native的能力，例如：图片视频库、Widget库，如果RN业务使用了新的Native功能，而这些Native库没有随着客户端版本升级，也会出现问题。
- RN框架升级：随着RN业务的发展，我们的RN工程化框架也在不断改进，例如：Bundle拆分、Bundle预加载等等，这些框架的新功能也是无法向下兼容的，新的RN业务跑在旧的客户端版本
上就会出现问题。

基于以上三点，我们采用了强依赖的方式。

我们接下来再说一说我们在热更新上是如何做的。👇

## 三 热更新方案

<img src="https://github.com/guoxiaoxing/vinci/raw/master/art/practice/hotfix_structure.png"/>

热更新流程主要围绕着版本号来的，具体如下所示：

> ${major}.${feature}.${patch}

- ${major} 描述为”不兼容老版本的发布（新增原生协议依赖或者升级底层库的版本）”
- ${feature} 描述为 “兼容老版本的功能迭代”
- ${patch} 描述为“兼容老版本的bugfix”

热更新方案的核心就是围绕着版本号做文章，我们有一个SRNHub的Node服务，它管理者RN Bundle以及它们的版本号。客户端在启动的时候回去请求RN Bundle以最新Bundle的版本号，然后和自己的版本
号进行对比，按照上述的版本策略决定是否更新。