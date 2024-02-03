# Agent-Demo

使用 Byte Buddy 和 Gradle 构建一个最简单的 Java Agent

Java Agent 的入口类是 `com.jas.agent.Main`

```java
public class Main {
    public static void premain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    public static void agentmain(String args, Instrumentation inst) {
        launch(args, inst);
    }

    private static void launch(String args, Instrumentation inst) {
        System.out.println("hello java agent");
    }
}
```

提供 AgentBuilder 最简化便利测试的配置项

```java
AgentBuilder agentBuilder = new AgentBuilder.Default()
        .ignore(ElementMatchers.none()) // 忽略空，即允许 hook 所有类
        .with(AgentBuilder.RedefinitionStrategy.REDEFINITION) // 开启类被加载后也允许进行字节码修改
        .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly()) // 字节码修改失败打印错误信息到控制台
        .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly()) // 字节码修改成功也输出到控制台
        .with(new DumpClassListener()); // 字节码修改成功把类信息给报错到 weaving/classes 目录下
```

## 编译

在项目目录下执行如下命令，会在 test 文件夹中生成 agent.jar 和 demo.jar

```shell

# linux or macos
./gradlew jar

# windows
gradlew.bat jar
```

## 启动 Java Agent

使用如下命令挂载 agent 启动 SpringBoot 程序，会打印 `hello java agent`

```shell
cd test && \
  java -javaagent:agent.jar -jar demo.jar
```