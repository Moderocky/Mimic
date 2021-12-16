package mx.kenzie.mimic;

@FunctionalInterface
@SuppressWarnings("unused")
public interface MethodExecutor {
    
    Object invoke(Object proxy, MethodErasure method, Object... arguments) throws Throwable;
    
}
