package mx.kenzie.mimic;

@FunctionalInterface
public interface ClassDefiner {

    <Type> Type define(ClassLoader loader, String name, byte[] bytecode);

    default String getPackage() {
        return null;
    }

}
