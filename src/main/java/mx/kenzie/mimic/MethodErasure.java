package mx.kenzie.mimic;

import java.io.Serializable;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record MethodErasure(Class<?> owner, String name, Class<?>... parameters) implements Constable, Serializable {
    
    public MethodErasure(Method method) {
        this(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
    }
    
    public MethodHandle handle()
        throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().unreflect(reflect());
    }
    
    public Method reflect()
        throws NoSuchMethodException {
        return owner.getDeclaredMethod(name, parameters);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodErasure erasure)) return false;
        return Objects.equals(name, erasure.name) && Arrays.equals(parameters, erasure.parameters);
    }
    
    @Override
    public int hashCode() {
        return 31 * name.hashCode() + Arrays.hashCode(parameters);
    }
    
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(owner.getSimpleName()).append('.');
        builder.append(name).append('(');
        boolean first = true;
        for (Class<?> parameter : parameters) {
            if (!first) builder.append(", ");
            first = false;
            builder.append(parameter.getSimpleName());
        }
        builder.append(')');
        return builder.toString();
    }
    
    @Override
    public Optional<? extends ConstantDesc> describeConstable() {
        return Optional.empty();
    }
    
}
