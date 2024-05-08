package mx.kenzie.mimic.test;

import mx.kenzie.mimic.Mimic;
import org.junit.Test;
import org.valross.foundation.detail.Signature;

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

        final Blob blob = Mimic.create((_, _, arguments) -> arguments[0], Blob.class);
        assert blob.bean(2) == 2;
        assert blob.say("hello").equals("hello");
        assert !(blob instanceof Box);

        final Box box = Mimic.create((_, _, arguments) -> arguments[0] + "", Box.class, Blob.class);
        assert box.doSomething("goodbye").equals("goodbye");
        assert box.doSomething(6).equals("6");
        assert box instanceof Blob;

    }

    @Test
    public void forward() {

        interface Blob {

            int bean(int i);

            String say(String word);

        }

        class Thing implements Blob {

            @Override
            public int bean(int i) {
                return 0;
            }

            @Override
            public String say(String word) {
                return "hello";
            }

        }

        {
            final Blob blob = Mimic.create(Blob.class).forward(Thing::new, Thing.class).build();
            assert blob.bean(2) == 0;
            assert blob.say("box").equals("hello");
            assert !(blob instanceof Thing);
        }

        {
            final Blob blob = Mimic.create(Blob.class).forward(new Thing()).build();
            assert blob.bean(2) == 0;
            assert blob.say("box").equals("hello");
            assert !(blob instanceof Thing);
        }

    }

    @Test
    public void override() {

        interface Blob {

            int a();

            int b();

            int c();

        }

        class Thing1 {

            public int a() {
                return 6;
            }

            public int b() {
                return 5;
            }

        }

        class Thing2 {

            public int a() {
                return 3;
            }

            public int c() {
                return 2;
            }

        }

        {
            final Blob blob = Mimic.create(Blob.class)
                .forward(Thing1::new, Thing1.class)
                .forward(Thing2::new, Thing2.class)
                .build();
            assert blob.a() == 6;
            assert blob.b() == 5;
            assert blob.c() == 2;
            assert !(blob instanceof Thing1);
            assert !(blob instanceof Thing2);
        }

        {
            final Blob blob = Mimic.create(Blob.class)
                .override((proxy, method, arguments) -> 17, new Signature(int.class, "b"))
                .forward(new Thing1())
                .forward(Thing2::new, Thing2.class)
                .build();
            assert blob.a() == 6;
            assert blob.b() == 17;
            assert blob.c() == 2;
            assert !(blob instanceof Thing1);
            assert !(blob instanceof Thing2);
        }

    }

}
