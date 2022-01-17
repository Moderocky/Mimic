package mx.kenzie.mimic;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Objects;

public interface Mimic {
    
    @SuppressWarnings("removal")
    static <Template> Template create(MethodExecutor executor, Class<Template> template, Class<?>... interfaces) {
        final int hash = Objects.hash(template, Arrays.hashCode(interfaces));
        final String name = InternalAccess.getStrictPackageName(template, interfaces);
        final Module module = InternalAccess.getStrictModule(template, interfaces);
        final PrivilegedAction<ClassLoader> action = module::getClassLoader;
        final ClassLoader loader = java.security.AccessController.doPrivileged(action);
        return new MimicGenerator(name.replace('.', '/') + "/Mimic_" + MimicGenerator.count(), template, interfaces)
            .create(loader, executor);
    }
    
}
