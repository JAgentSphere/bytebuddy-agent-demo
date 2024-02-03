package com.jas.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;

/**
 * @author ReaJason
 * @since 2024/2/3
 */
public class Main {
    public static void premain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    private static void launch(String args, Instrumentation inst) {
        System.out.println("hello java agent");

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .ignore(ElementMatchers.none()) // 忽略空，即允许 hook 所有类
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION) // 开启类被加载后也允许进行字节码修改
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly()) // 字节码修改失败打印错误信息到控制台
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly()) // 字节码修改成功也输出到控制台
                .with(new DumpClassListener()); // 字节码修改成功把类信息给报错到 weaving/classes 目录下

        // 在 SpringBoot 启动后的打印 Starting DemoApplication 前输出一句话
        // org.springframework.boot.StartupInfoLogger#getStartingMessage
        agentBuilder.type(ElementMatchers.named("org.springframework.boot.StartupInfoLogger"))
                .transform(((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(SpringStartupInterceptor.class)
                                        .on(ElementMatchers.named("getStartingMessage"))))
                ).installOn(inst);
    }

    private static class SpringStartupInterceptor {
        @Advice.OnMethodEnter
        public static void interceptor() {
            System.out.println("hello springboot, i know you will print the starting info");
        }
    }
}