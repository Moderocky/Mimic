package mx.kenzie.mimic.test;

import mx.kenzie.mimic.Mimic;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.PrivilegedExceptionAction;

public class MimicTest {
    
    @Test
    @SuppressWarnings("removal")
    public void fieldArrayTest() throws Throwable {
        final Unsafe unsafe = java.security.AccessController.doPrivileged((PrivilegedExceptionAction<Unsafe>) () -> {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        });
        assert unsafe.objectFieldOffset(mx.kenzie.mimic.test.Test.class.getDeclaredField("executor")) == 12;
        assert unsafe.objectFieldOffset(mx.kenzie.mimic.test.Test.class.getDeclaredField("methods")) == 16;
    }
    
    @Test
    public void basic() {
        
        class Box {
            
            public String doSomething(String word) {
                return "goodbye";
            }
            
            public String doSomething(int i) {
                return "goodbye";
            }
            
        }
        
        interface Blob {
            void bean(int i);
            
            String say(String word);
        }
        
        final Blob blob = Mimic.create((proxy, method, arguments) -> arguments[0], Blob.class);
        blob.bean(2);
        assert blob.say("hello").equals("hello");
        assert !(blob instanceof Box);
        
        final Box box = Mimic.create((proxy, method, arguments) -> arguments[0] + "", Box.class, Blob.class);
        assert box.doSomething("goodbye").equals("goodbye");
        assert box.doSomething(6).equals("6");
        assert box instanceof Blob;
        
    }
    
}
