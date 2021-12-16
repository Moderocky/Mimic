package mx.kenzie.mimic.test;

import mx.kenzie.mimic.MethodErasure;
import mx.kenzie.mimic.MethodExecutor;
import mx.kenzie.mimic.Mimic;

class Test implements Mimic {
    
    public MethodExecutor executor;
    public MethodErasure[] methods;
    
    public void test(Object a) throws Throwable {
        executor.invoke(this, methods[3], a);
    }
    
}
