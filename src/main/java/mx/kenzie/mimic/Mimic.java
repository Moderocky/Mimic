package mx.kenzie.mimic;

import java.security.PrivilegedAction;
import java.util.function.Supplier;

public interface Mimic {
    
    static <Template> Template create(Supplier<Template> target, Class<Template> type) {
        return create(type).forward(target, type).build();
    }
    
    static <Template> MimicBuilder<Template> create(Class<Template> template, Class<?>... interfaces) {
        return new SimpleMimicBuilder<>(template, interfaces);
    }
    
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
