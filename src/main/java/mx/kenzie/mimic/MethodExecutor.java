package mx.kenzie.mimic;

import org.valross.foundation.detail.Signature;

@FunctionalInterface
@SuppressWarnings("unused")
public interface MethodExecutor {

    Object invoke(Object proxy, Signature method, Object... arguments) throws Throwable;

}
