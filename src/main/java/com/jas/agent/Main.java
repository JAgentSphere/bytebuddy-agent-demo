package com.jas.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;

/**
 * @author ReaJason
 * @since 2024/3/8
 */
public class Main {
    public static void premain(String args, Instrumentation inst) throws Exception {
        launch(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) throws Exception {
        launch(args, inst);
    }

    private static void launch(String args, Instrumentation inst) throws Exception {
        System.out.println("rainbow-brackets-2024.2.1 cracked plugin");

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .ignore(ElementMatchers.none()) // 忽略空，即允许 hook 所有类
                .with(AgentBuilder.RedefinitionStrategy.REDEFINITION) // 开启类被加载后也允许进行字节码修改
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly()) // 字节码修改失败打印错误信息到控制台
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly()) // 字节码修改成功也输出到控制台
                .with(new DumpClassListener()); // 字节码修改成功把类信息给报错到 weaving/classes 目录下

        byPassJavaAgent(agentBuilder, inst);

        byPassLicense(agentBuilder, inst);
    }

    /**
     * 在 getUserOptionsFile 获取配置文件时返回默认的文件位置，会检测文件中是否包含 ja
     * <p>
     * 在 StringUtils.isEmpty 方法中，如果不是 com.janetfilter 包下的类调用，则抛出异常，因为检测了这个方法
     */
    private static void byPassJavaAgent(AgentBuilder agentBuilder, Instrumentation inst) {
        agentBuilder.type(ElementMatchers.named("com.intellij.diagnostic.VMOptions"))
                .transform(((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(VMOptionsInterceptor.class).on(ElementMatchers.named("getUserOptionsFile"))))
                ).installOn(inst);

        agentBuilder.type(ElementMatchers.named("com.janetfilter.core.utils.StringUtils"))
                .transform(((builder, typeDescription, classLoader, javaModule, protectionDomain) ->
                        builder.visit(Advice.to(StringUtilsInterceptor.class).on(ElementMatchers.named("isEmpty")))))
                .installOn(inst);
    }

    public static class StringUtilsInterceptor {
        @Advice.OnMethodEnter
        public static void interceptorBefore(@Advice.AllArguments Object[] args,
                                             @Advice.Origin("#m") String methodName) {
            if ("isEmpty".equals(methodName)) {
                Object arg = args[0];
                if (arg != null && arg.toString().isEmpty()) {
                    if (!new Throwable().getStackTrace()[2].getClassName().startsWith("com.janetfilter.")) {
                        throw new RuntimeException("fuck you");
                    }
                }
            }
        }
    }

    public static class VMOptionsInterceptor {

        @Advice.OnMethodExit
        public static void interceptor(@Advice.Return(readOnly = false) Path ret) {
            try {
                if (new Throwable().getStackTrace()[2].getClassName().startsWith("jdk.internal.reflect")) {
                    String fileName = (String) Class.forName("com.intellij.diagnostic.VMOptions").getDeclaredMethod("getFileName").invoke(null);
                    String location = (String) Class.forName("com.intellij.openapi.application.PathManager").getDeclaredMethod("getCustomOptionsDirectory").invoke(null);
                    ret = Paths.get(location, fileName);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * 设置证书过期时间为 50 天，绕过大于 60 天的检测
     */
    private static void byPassLicense(AgentBuilder agentBuilder, Instrumentation inst) {
        agentBuilder.type(ElementMatchers.named("com.intellij.ui.LicensingFacade"))
                .transform((builder, typeDescription, classLoader, javaModule, protectionDomain) ->
                        builder.visit(Advice.to(LicenseExpirationInterceptor.class).on(ElementMatchers.named("getLicenseExpirationDate"))))
                .installOn(inst);
    }

    public static class LicenseExpirationInterceptor {
        @Advice.OnMethodExit
        public static void exit(@Advice.Return(readOnly = false) Date ret) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 50);
            ret = calendar.getTime();
        }
    }
}