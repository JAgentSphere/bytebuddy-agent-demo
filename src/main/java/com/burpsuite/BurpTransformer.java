package com.burpsuite;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public class BurpTransformer implements ClassFileTransformer {

    public BurpTransformer() {
        System.out.println("    ____                      _____       _ __          __    ____            __         \n   / __ )__  ___________     / ___/__  __(_) /____     / /   / __ \\____ _____/ /__  _____\n  / __  / / / / ___/ __ \\    \\__ \\/ / / / / __/ _ \\   / /   / / / / __ `/ __  / _ \\/ ___/\n / /_/ / /_/ / /  / /_/ /   ___/ / /_/ / / /_/  __/  / /___/ /_/ / /_/ / /_/ /  __/ /    \n/_____/\\__,_/_/  / .___/   /____/\\__,_/_/\\__/\\___/  /_____/\\____/\\__,_/\\__,_/\\___/_/     \n                /_/                                                                      ");
        System.err.format("Github:https://github.com/x-Ai/BurpSuite 商业使用请购买正版软件！", new Object[0]);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBytes) {
        if (className == null || !className.startsWith("burp/") || classBytes == null || classBytes.length < 110000) {
            return null;
        }
        ClassReader classReader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 6);
        boolean patched = false;
        for (MethodNode method : classNode.methods) {
            if (method.desc.equals("([Ljava/lang/Object;Ljava/lang/Object;)V") && method.instructions.size() > 20000) {
                System.out.printf("Burp加载器类: %s  类 %s  方法: %s %s%n", loader, className, method.name, method.desc);
                InsnList instructions = method.instructions;
                instructions.clear();
                instructions.add(new VarInsnNode(25, 0));
                instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/burpsuite/Spy", "rewriteBytes", "([Ljava/lang/Object;)V", false));
                instructions.add(new InsnNode(Opcodes.RETURN));
                method.exceptions.clear();
                method.tryCatchBlocks.clear();
                patched = true;
            }
        }
        if (patched) {
            ClassWriter classWriter = new ClassWriter(classReader, 3);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        }
        return null;
    }
}