package mx.kenzie.mimic;

import java.security.PrivilegedAction;

public interface Mimic {
    
    @SuppressWarnings("removal")
    static <Template> Template create(MethodExecutor executor, Class<Template> template, Class<?>... interfaces) {
        final int index = MimicGenerator.count();
        final String name = InternalAccess.getStrictPackageName(template, interfaces);
        final Module module = InternalAccess.getStrictModule(template, interfaces);
        final String path = name.replace('.', '/') + "/Mimic_" + index;
        final PrivilegedAction<ClassLoader> action = module::getClassLoader;
        final ClassLoader loader = java.security.AccessController.doPrivileged(action);
        return new MimicGenerator(path, template, interfaces).create(loader, executor);
    }
    
}
