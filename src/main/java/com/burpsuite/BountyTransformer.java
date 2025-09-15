package com.burpsuite;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;

public class BountyTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBytes) throws IllegalClassFormatException {
        try {
            byte[] result = patchFeignClient(className, classBytes, loader);
            if (result != null) {
                return result;
            }
            return patchReadPemFile(className, classBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] patchFeignClient(String className, byte[] classBytes, ClassLoader loader) throws Exception {
        if (className == null || !className.equals("feign/okhttp/OkHttpClient")) {
            return null;
        }
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        defineHelper(loader);
        for (MethodNode method : cn.methods) {
            if ("toFeignResponse".equals(method.name)
                    && "(Lokhttp3/Response;Lfeign/Request;)Lfeign/Response;".equals(method.desc)) {

                AbstractInsnNode[] snapshot = method.instructions.toArray();
                for (AbstractInsnNode node : snapshot) {
                    if (!(node instanceof MethodInsnNode)) continue;
                    MethodInsnNode insnNode = (MethodInsnNode) node;
                    if (insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && "feign/Response$Builder".equals(insnNode.owner)
                            && "build".equals(insnNode.name)
                            && "()Lfeign/Response;".equals(insnNode.desc)) {

                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "feign/Request", "url", "()Ljava/lang/String;", false));
                        insnList.add(new VarInsnNode(Opcodes.ASTORE, 2));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "feign/Request", "body", "()[B", false));
                        insnList.add(new VarInsnNode(Opcodes.ASTORE, 3));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 2));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 3));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/burpsuite/Spy", "testFilter", "(Ljava/lang/String;[B)[B", false));
                        insnList.add(new VarInsnNode(Opcodes.ASTORE, 4));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 4));
                        LabelNode outLabel = new LabelNode();
                        insnList.add(new JumpInsnNode(Opcodes.IFNULL, outLabel));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 4));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "feign/Response$Builder", "body", "([B)Lfeign/Response$Builder;", false));
                        insnList.add(new IntInsnNode(Opcodes.SIPUSH, 200));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "feign/Response$Builder", "status", "(I)Lfeign/Response$Builder;", false));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 2));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 4));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "okhttp3/Response", "headers", "()Lokhttp3/Headers;", false));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "feign/okhttp/OkHttpClient", "toMap", "(Lokhttp3/Headers;)Ljava/util/Map;", false));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/burpsuite/Spy", "testMap", "(Ljava/lang/String;[BLjava/util/Map;)Ljava/util/Map;", false));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "feign/Response$Builder", "headers", "(Ljava/util/Map;)Lfeign/Response$Builder;", false));
                        insnList.add(outLabel);

                        method.instructions.insertBefore(insnNode, insnList);
                    }
                }
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(writer);
        return writer.toByteArray();
    }

    private byte[] patchReadPemFile(String className, byte[] classBytes) {
        if (className == null || !className.equals("com/licensespring/api/AuthorizationService")) {
            return null;
        }
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        for (MethodNode method : cn.methods) {
            if ("readPemFile".equals(method.name) && "()[B".equals(method.desc)) {
                method.instructions.clear();
                if (method.localVariables != null) {
                    method.localVariables.clear();
                }
                if (method.tryCatchBlocks != null) {
                    method.tryCatchBlocks.clear();
                }
                InsnList il = new InsnList();
                il.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/util/Base64",
                        "getDecoder",
                        "()Ljava/util/Base64$Decoder;",
                        false
                ));
                il.add(new LdcInsnNode(Spy.publicKey));
                il.add(new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        "java/util/Base64$Decoder",
                        "decode",
                        "(Ljava/lang/String;)[B",
                        false
                ));
                il.add(new InsnNode(Opcodes.ARETURN));
                method.instructions.add(il);
                method.maxStack = 2;
                method.maxLocals = 1;
                break;
            }
        }
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        cn.accept(writer);
        return writer.toByteArray();
    }

    private void defineHelper(ClassLoader loader) {
        try {
            String classPath = "com/burpsuite/Spy.class";
            InputStream classStream = BountyTransformer.class
                    .getClassLoader()
                    .getResourceAsStream(classPath);
            if (classStream == null) {
                throw new IllegalStateException("Cannot find Spy.class in agent JAR");
            }
            byte[] classBytes = readBytes(classStream);
            Object unsafe = null;
            Object rawModule = null;
            long offset = 48;
            Method getAndSetObjectM = null;
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
                unsafeField.setAccessible(true);
                unsafe = unsafeField.get(null);
                rawModule = Class.class.getMethod("getModule").invoke(this.getClass(), (Object[]) null);
                Object module = Class.class.getMethod("getModule").invoke(Object.class, (Object[]) null);
                Method objectFieldOffsetM = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
                offset = (Long) objectFieldOffsetM.invoke(unsafe, Class.class.getDeclaredField("module"));
                getAndSetObjectM = unsafe.getClass().getMethod("getAndSetObject", Object.class, long.class, Object.class);
                getAndSetObjectM.invoke(unsafe, this.getClass(), offset, module);
            } catch (Throwable ignored) {
            }
            Method defMethod = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, Integer.TYPE, Integer.TYPE);
            defMethod.setAccessible(true);
            defMethod.invoke(loader, classBytes, 0, classBytes.length);
            if (getAndSetObjectM != null) {
                getAndSetObjectM.invoke(unsafe, this.getClass(), offset, rawModule);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int len;
        while ((len = is.read(chunk)) != -1) {
            buffer.write(chunk, 0, len);
        }
        return buffer.toByteArray();
    }
}
