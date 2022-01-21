package mx.kenzie.mimic;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class MimicGenerator {
    
    private static volatile int counter;
    protected final ClassWriter writer;
    protected final String internal;
    protected final Class<?> top;
    protected final Class<?>[] interfaces;
    protected final List<MethodErasure> finished;
    protected final MethodWriter handler;
    protected final Map<MethodErasure, MethodWriter> overrides = new HashMap<>();
    protected final Map<FieldErasure, Object> fields = new HashMap<>();
    protected int index;
    
    protected MimicGenerator(String location, Class<?> top, Class<?>... interfaces) {
        this(new MethodWriter(), location, top, interfaces);
    }
    
    protected MimicGenerator(MethodWriter handler, String location, Class<?> top, Class<?>... interfaces) {
        this.handler = handler;
        this.writer = new ClassWriter(0);
        this.internal = location;
        this.top = top;
        this.interfaces = interfaces;
        this.finished = new ArrayList<>();
    }
    
    static synchronized int count() {
        return counter++;
    }
    
    @SuppressWarnings("unchecked")
    public <Template> Template create(ClassLoader loader, MethodExecutor executor) {
        final boolean complex = !top.isInterface() || !overrides.isEmpty() || !fields.isEmpty();
        final byte[] bytecode = writeCode();
        final Class<?> type = InternalAccess.loadClass(loader, internal.replace('/', '.'), bytecode);
        final Object object = this.allocateInstance(type);
        if (complex) {
            try {
                for (Map.Entry<FieldErasure, Object> entry : fields.entrySet()) {
                    final long offset = this.offset(object.getClass().getDeclaredField(entry.getKey().name()));
                    this.putValue(object, offset, entry.getValue());
                }
                final long exec = this.offset(object.getClass().getDeclaredField("executor"));
                final long meth = this.offset(object.getClass().getDeclaredField("methods"));
                this.putValue(object, exec, executor);
                this.putValue(object, meth, finished.toArray(new MethodErasure[0]));
            } catch (NoSuchFieldException ignored) {
            }
        } else {
            this.putValue(object, 12, executor);
            this.putValue(object, 16, finished.toArray(new MethodErasure[0]));
        }
        return (Template) object;
    }
    
    protected byte[] writeCode() {
        this.writer.visit(61, 1 | 16 | 32, internal, null, Type.getInternalName(top != null && !top.isInterface() ? top : Object.class), this.getInterfaces());
        this.writer.visitField(0, "executor", "Lmx/kenzie/mimic/MethodExecutor;", null, null).visitEnd();
        this.writer.visitField(0, "methods", "[Lmx/kenzie/mimic/MethodErasure;", null, null).visitEnd();
        for (final FieldErasure erasure : fields.keySet()) {
            this.writer.visitField(0, erasure.name(), Type.getDescriptor(erasure.type()), null, null).visitEnd();
        }
        if (top != null && top != Object.class) this.scrapeMethods(top);
        for (final Class<?> template : interfaces) this.scrapeMethods(template);
        this.writer.visitEnd();
        return writer.toByteArray();
    }
    
    protected Object allocateInstance(Class<?> type) {
        return InternalAccess.allocateInstance(type);
    }
    
    protected long offset(final Field field) {
        return InternalAccess.offset(field);
    }
    
    protected void putValue(final Object object, final long offset, final Object value) {
        InternalAccess.put(object, offset, value);
    }
    
    protected String[] getInterfaces() {
        final Set<String> strings = new HashSet<>();
        if (top.isInterface()) strings.add(Type.getInternalName(top));
        for (Class<?> type : interfaces) {
            strings.add(Type.getInternalName(type));
        }
        return strings.toArray(new String[0]);
    }
    
    protected void scrapeMethods(Class<?> template) {
        for (final Method method : template.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isFinal(method.getModifiers())) continue;
            if (Modifier.isPrivate(method.getModifiers())) continue;
            final MethodErasure erasure = new MethodErasure(method);
            if (finished.contains(erasure)) continue;
            this.finished.add(erasure);
            this.writeCaller(method);
            this.index++;
        }
    }
    
    protected void writeCaller(Method method) {
        final MethodErasure erasure = new MethodErasure(method);
        final MethodWriter writer;
        if (overrides.containsKey(erasure)) writer = this.overrides.get(erasure);
        else writer = this.handler;
        writer.write(this, method, this.writer, internal, index);
    }
    
}
