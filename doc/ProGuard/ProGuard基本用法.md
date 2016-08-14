# ProGuard基本用法

ProGuard是一个针对Java字节吗进行压缩、优化、混淆与预检等开源项目。

>ProGuard is a free Java class file shrinker, optimizer, obfuscator, and preverifier. It detects and removes unused classes, fields, methods, 
and attributes. It optimizes bytecode and removes unused instructions. It renames the remaining classes, fields, and methods using short
meaningless names. Finally, it preverifies the processed code for Java 6 or higher, or for Java Micro Edition.

官网地址: http://proguard.sourceforge.net/

文档地址: http://proguard.sourceforge.net/

官方例子: http://proguard.sourceforge.net/

ProGuard主要包含四个方面的功能:

压缩: 侦测并移除代码中无用的类、字段、方法与特性。
优化: 对字节吗进行优化, 移除无用指令。
混淆: 使用abcd等这样简短而又无用的名称对类、字段与方法进行重命名。
预检: 在Java平台上对处理后的代码进行预检。

当我们在Android Studio创建一个项目以后, build.gradle文件里有一段代码:

```
buildTypes {
    release {
        minifyEnabled false
        proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
    }
}
```


这里指定了两个混淆配置文件

proguard-android.txt

proguard-rules.pro


混淆之后会生成四个文件

mapping.txt: 表示混淆前后代码的对照表，这个文件非常重要。如果你的代码混淆后会产生bug的话，log提示中是混淆后的代码，希望定位到源代码的话就可以根据mapping.txt反推。
每次发布都要保留它方便该版本出现问题时调出日志进行排查，它可以根据版本号或是发布时间命名来保存或是放进代码版本控制中。

dump.txt: 描述apk内所有class文件的内部结构。

seeds.txt: 列出了没有被混淆的类和成员。

usage.txt: 列出了源代码中被删除在apk中不存在的代码。