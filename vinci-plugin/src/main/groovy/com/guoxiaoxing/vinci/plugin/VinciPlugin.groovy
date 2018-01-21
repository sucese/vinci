package com.guoxiaoxing.vinci.plugin

import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class VinciPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        println("============================================================================")
        println("==                                                                        ==")
        println("==                      Cuckoo Bytecode Insert ...                        ==")
        println("==                                                                        ==")
        println("==   For more information, you can visit  https://github.com/guoxiaoxing  ==")
        println("==   or contact me by guoxiaoxingse@163.com.                              ==")
        println("==                                                                        ==")
        println("==                                                                        ==")
        println("============================================================================")

        project.repositories.flatDir { dirs 'libs' }

        final def log = project.logger

        project.dependencies {
            implementation 'org.aspectj:aspectjrt:1.8.10'
            implementation 'com.github.guoxiaoxing:cuckoo-aspectj:0.0.9'
        }

        project.extensions.create("cuckoo", VinciExtension)

        project.android.applicationVariants.all { variant ->
            JavaCompile javaCompile = variant.javaCompile

            javaCompile.doLast {
                String[] args = [
                        "-showWeaveInfo",
                        "-1.7",
                        "-inpath", javaCompile.destinationDir.toString(),
                        "-aspectpath", javaCompile.classpath.asPath,
                        "-d", javaCompile.destinationDir.toString(),
                        "-classpath", javaCompile.classpath.asPath,
                        "-bootclasspath", project.android.bootClasspath.join(File.pathSeparator)
                ]
                MessageHandler handler = new MessageHandler(true);
                new Main().run(args, handler);
                for (IMessage message : handler.getMessages(null, true)) {
                    switch (message.getKind()) {
                        case IMessage.ABORT:
                        case IMessage.ERROR:
                        case IMessage.FAIL:
                            log.error message.message, message.thrown
                            break;
                        case IMessage.WARNING:
                            log.warn message.message, message.thrown
                            break;
                        case IMessage.INFO:
                            log.info message.message, message.thrown
                            break;
                        case IMessage.DEBUG:
                            log.debug message.message, message.thrown
                            break;
                    }
                }
            }
        }
    }
}