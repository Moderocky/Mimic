package mx.kenzie.mimic.test;

import mx.kenzie.mimic.Mimic;
import org.junit.Test;

public class MimicTest {
    
    @Test
    public void basic() {
        
        interface Blob {
            int bean(int i);
            
            String say(String word);
        }
        
        class Box {
            
            public String doSomething(String word) {
                return "goodbye";
            }
            
            public String doSomething(int i) {
                return "goodbye";
            }
            
        }
        
        final Blob blob = Mimic.create((proxy, method, arguments) -> arguments[0], Blob.class);
        assert blob.bean(2) == 2;
        assert blob.say("hello").equals("hello");
        assert !(blob instanceof Box);
        
        final Box box = Mimic.create((proxy, method, arguments) -> arguments[0] + "", Box.class, Blob.class);
        assert box.doSomething("goodbye").equals("goodbye");
        assert box.doSomething(6).equals("6");
        assert box instanceof Blob;
        
    }
    
}
