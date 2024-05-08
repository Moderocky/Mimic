package mx.kenzie.mimic.test;

import mx.kenzie.mimic.InternalAccess;
import mx.kenzie.mimic.Mimic;
import org.junit.BeforeClass;
import org.junit.Test;

public class PrivateMimicTest {

    static Object javaLangAccess;

    @BeforeClass
    public static void find() {
        javaLangAccess = InternalAccess.getJavaLangAccess();
    }

    @Test
    public void test() {

        interface JLS {

            Object classData(Class<?> c);

        }

        final JLS jls = Mimic.create(JLS.class).forward(javaLangAccess).build();
        IllegalAccessError error = null;
        try {
            jls.classData(PrivateMimicTest.class);
        } catch (IllegalAccessError ex) {
            error = ex;
        }
        assert error != null;

    }

}
