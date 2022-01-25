package mx.kenzie.mimic;

import sun.misc.Unsafe;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;

@SuppressWarnings("removal")
public class InternalAccess implements ClassDefiner {
    
    static final String LOCATION = "com.sun.proxy";
    static Object javaLangAccess;
    static Unsafe unsafe;
    static Method defineClass;
    static Method addExports0;
    static long offset;
    
    static {
        try {
            final Class<?> secrets = Class.forName("jdk.internal.access.SharedSecrets", false, ClassLoader.getSystemClassLoader());
            unsafe = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>) () -> {
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return (Unsafe) field.get(null);
            });
            final Field field = Class.class.getDeclaredField("module");
            offset = unsafe.objectFieldOffset(field);
            unsafe.putObject(InternalAccess.class, offset, Object.class.getModule());
            final Method setAccessible0 = AccessibleObject.class.getDeclaredMethod("setAccessible0", boolean.class);
            setAccessible0.setAccessible(true);
            final Method implAddExportsOrOpens = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
            setAccessible0.invoke(implAddExportsOrOpens, true);
            addExports0 = Module.class.getDeclaredMethod("addExports0", Module.class, String.class, Module.class);
            setAccessible0.invoke(addExports0, true);
            final Method getJavaLangAccess = secrets.getDeclaredMethod("getJavaLangAccess");
            setAccessible0.invoke(getJavaLangAccess, true);
            javaLangAccess = getJavaLangAccess.invoke(null);
            defineClass = javaLangAccess.getClass()
                .getMethod("defineClass", ClassLoader.class, String.class, byte[].class, ProtectionDomain.class, String.class);
            setAccessible0.invoke(defineClass, true);
        } catch (Throwable ex) {
            throw new RuntimeException(ex);
        }
    }
    
    InternalAccess() {
    }
    
    static void export(final Module module, final String namespace) {
        try {
            addExports0.invoke(null, module, namespace, MethodErasure.class.getModule());
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            module.addExports(namespace, MethodErasure.class.getModule());
        }
    }
    
    @SuppressWarnings("unchecked")
    static <Type> Type allocateInstance(Class<?> type) {
        try {
            return (Type) unsafe.allocateInstance(type);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }
    }
    
    static void put(Object target, long offset, Object value) {
        unsafe.putObject(target, offset, value);
    }
    
    static Module getStrictModule(Class<?> top, Class<?>... interfaces) {
        if (!top.getModule().isExported(top.getPackageName())) return top.getModule();
        for (final Class<?> place : interfaces) {
            if (!place.getModule().isExported(place.getPackageName())) return place.getModule();
        }
        return MethodErasure.class.getModule();
    }
    
    static String getStrictPackageName(Class<?> top, Class<?>... interfaces) {
        String namespace = LOCATION + ".mimic";
        if (top != null && !Modifier.isPublic(top.getModifiers())) namespace = top.getPackageName();
        for (final Class<?> place : interfaces) {
            if (!Modifier.isPublic(place.getModifiers())) namespace = place.getPackageName();
        }
        return namespace;
    }
    
    static long offset(Field field) {
        return unsafe.objectFieldOffset(field);
    }
    
    static void moveModule(Class<?> from, Class<?> to) {
        unsafe.putObject(from, offset, to.getModule());
    }
    
    public static Object getJavaLangAccess() {
        return javaLangAccess;
    }
    
    public static Unsafe getUnsafe() {
        return unsafe;
    }
    
    public static ClassDefiner createDefiner(Class<?> target) {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(target, MethodHandles.lookup());
            class Definer implements ClassDefiner {
                
                @Override
                public <Type> Type define(ClassLoader loader, String name, byte[] bytecode) {
                    try {
                        return (Type) lookup.defineHiddenClass(bytecode, true, MethodHandles.Lookup.ClassOption.NESTMATE)
                            .lookupClass();
                    } catch (IllegalAccessException e) {
                        unsafe.throwException(e);
                        return null;
                    }
                }
                
                @Override
                public String getPackage() {
                    return target.getPackageName();
                }
            }
//            moveModule(Definer.class, target);
            return new Definer();
        } catch (IllegalAccessException e) {
            unsafe.throwException(e);
            return null;
        }
    }
    
    @Override
    @SuppressWarnings({"unchecked"})
    public <Type> Type define(ClassLoader loader, String name, byte[] bytecode) {
        return (Type) loadClass(loader, name, bytecode);
    }
    
    static Class<?> loadClass(ClassLoader loader, String name, byte[] bytes) {
        try {
            return (Class<?>) defineClass.invoke(javaLangAccess, loader, name, bytes, null, "__Mimic__");
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }
}
