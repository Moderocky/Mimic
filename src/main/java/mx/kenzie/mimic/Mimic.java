package mx.kenzie.mimic;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

public interface Mimic {
    
    Random RANDOM = new Random();
    
    @SuppressWarnings("removal")
    static <Template> Template create(MethodExecutor executor, Class<Template> template, Class<?>... interfaces) {
        final int hash = Objects.hash(template, Arrays.hashCode(interfaces));
        final String name = InternalAccess.getStrictPackageName(template, interfaces);
        final Module module = InternalAccess.getStrictModule(template, interfaces);
        PrivilegedAction<ClassLoader> pa = module::getClassLoader;
        final ClassLoader loader = java.security.AccessController.doPrivileged(pa);
        return new MimicGenerator(name.replace('.', '/') + "/Mimic_" + hash + RANDOM.nextInt(10000, 99999), template, interfaces)
            .create(loader, executor);
    }
    
}
