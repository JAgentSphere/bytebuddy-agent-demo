package com.jas.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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


        agentBuilder.type(ElementMatchers.named("java.lang.Runtime"))
                .transform(((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(
                                Advice.to(RuntimeExecInterceptor.class)
                                        .on(ElementMatchers.named("exec")
                                                .and(ElementMatchers.takesArguments(1)))
                        ))).installOn(inst);
    }

    private static class RuntimeExecInterceptor {
        @Advice.OnMethodEnter
        public static void interceptor(@Advice.AllArguments Object[] args) {
            System.out.println("Runtime.exec is invoked");
            String command = (String) args[0];
            System.out.println("Runtime.exec arg is " + command);

            // 方法参数的判断, 执行 whoami 会在这儿抛出异常
            if ("whoami".equals(command)) {
                throw new SecurityException("the command whoami is prohibited in this env");
            }

            // 获取堆栈执行上下文进行特定的判断
            List<String> stackTraces = Arrays.stream(new Throwable().getStackTrace()).limit(100)
                    .map(StackTraceElement::toString).collect(Collectors.toList());

            // 所有的命令都会被阻断，因为堆栈中包含了这个
            for (String stackTrace : stackTraces) {
                if (stackTrace.contains("com.jas.web.demo.IndexController.cmd")) {
                    throw new SecurityException("exec command with dangerous stack");
                }
            }
        }
    }

    private static class SpringStartupInterceptor {
        @Advice.OnMethodEnter
        public static void interceptor() {
            System.out.println("hello springboot, i know you will print the starting info");
        }
    }
}