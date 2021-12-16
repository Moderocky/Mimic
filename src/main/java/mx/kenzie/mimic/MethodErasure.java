package mx.kenzie.mimic;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record MethodErasure(Class<?> owner, String name, Class<?>... parameters) implements Constable {
    
    public MethodErasure(Method method) {
        this(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
    }
    
    public Method reflect()
        throws NoSuchMethodException {
        return owner.getDeclaredMethod(name, parameters);
    }
    
    public MethodHandle handle()
        throws NoSuchMethodException, IllegalAccessException {
        return MethodHandles.lookup().unreflect(reflect());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodErasure erasure)) return false;
        return Objects.equals(name, erasure.name) && Arrays.equals(parameters, erasure.parameters);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(owner, name);
        result = 31 * result + Arrays.hashCode(parameters);
        return result;
    }
    
    @Override
    public Optional<? extends ConstantDesc> describeConstable() {
        return Optional.empty();
    }
    
}
