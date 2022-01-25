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
    protected ClassDefiner definer = new InternalAccess();
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
        final Class<?> type = definer.define(loader, internal.replace('/', '.'), bytecode);
        assert type != null;
        final Object object = this.allocateInstance(type);
        if (complex) {
            for (Map.Entry<FieldErasure, Object> entry : fields.entrySet()) {
                this.setField(object, entry.getKey().name(), entry.getValue());
            }
            this.setField(object, "executor", executor);
            this.setField(object, "methods", finished.toArray(new MethodErasure[0]));
        } else { // trivial
            this.putValue(object, 12, executor);
            this.putValue(object, 16, finished.toArray(new MethodErasure[0]));
        }
        return (Template) object;
    }
    
    protected byte[] writeCode() {
        this.writer.visit(61, 1 | 16 | 32, internal, null, Type.getInternalName(top != null && !top.isInterface() ? top : Object.class), this.getInterfaces());
        this.writer.visitField(0x00000080 | 0x00000004, "executor", "Lmx/kenzie/mimic/MethodExecutor;", null, null)
            .visitEnd();
        this.writer.visitField(0x00000080 | 0x00000004, "methods", "[Lmx/kenzie/mimic/MethodErasure;", null, null)
            .visitEnd();
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
    
    private void setField(Object owner, String name, Object value) {
        final Field field = this.getField(owner.getClass(), name);
        if (field == null) return;
        final long offset = this.offset(this.getField(owner.getClass(), name));
        this.putValue(owner, offset, value);
    }
    
    protected void putValue(final Object object, final long offset, final Object value) {
        InternalAccess.put(object, offset, value);
    }
    
    protected String[] getInterfaces() {
        final Set<String> strings = new HashSet<>();
        if (top.isInterface()) strings.add(Type.getInternalName(top));
        for (final Class<?> type : interfaces) strings.add(Type.getInternalName(type));
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
    
    private Field getField(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException | NullPointerException ex) {
            return null;
        }
    }
    
    protected long offset(final Field field) {
        return InternalAccess.offset(field);
    }
    
    protected void writeCaller(Method method) {
        final MethodErasure erasure = new MethodErasure(method);
        final MethodWriter writer;
        if (overrides.containsKey(erasure)) writer = this.overrides.get(erasure);
        else writer = this.handler;
        writer.write(this, method, this.writer, internal, index);
    }
    
}
