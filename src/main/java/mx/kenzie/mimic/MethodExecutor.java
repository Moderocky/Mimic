package mx.kenzie.mimic;

import org.valross.foundation.detail.Member;

@FunctionalInterface
@SuppressWarnings("unused")
public interface MethodExecutor {

    Object invoke(Object proxy, Member method, Object... arguments) throws Throwable;

}
