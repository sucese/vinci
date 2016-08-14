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