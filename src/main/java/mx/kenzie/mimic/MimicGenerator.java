package mx.kenzie.mimic;

import org.valross.foundation.assembler.tool.Access;
import org.valross.foundation.assembler.tool.ClassFileBuilder;
import org.valross.foundation.detail.Member;
import org.valross.foundation.detail.Signature;
import org.valross.foundation.detail.Type;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static org.valross.foundation.detail.Version.JAVA_17;
import static org.valross.foundation.detail.Version.RELEASE;

public class MimicGenerator {

    private static volatile int counter;
    protected final ClassFileBuilder writer;
    protected final Type internal;
    protected final Class<?> top;
    protected final Class<?>[] interfaces;
    protected final List<Member> finished;
    protected final MethodWriter handler;
    protected final Map<Member, MethodWriter> overrides = new HashMap<>();
    protected final Map<Member, Object> fields = new HashMap<>();
    protected ClassDefiner definer = new InternalAccess();
    protected int index;

    protected MimicGenerator(String location, Class<?> top, Class<?>... interfaces) {
        this(new MethodWriter(), location, top, interfaces);
    }

    protected MimicGenerator(MethodWriter handler, String location, Class<?> top, Class<?>... interfaces) {
        this.handler = handler;
        this.writer = new ClassFileBuilder(JAVA_17, RELEASE);
        this.internal = Type.fromInternalName(location);
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
        {// todo
            final File file = new File("target/generated-mimic/" + top.getName() + ".class");
            file.getParentFile().mkdirs();
            try (OutputStream out = new FileOutputStream(file)) {
                out.write(bytecode);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        final Class<?> type = definer.define(loader, internal.getTypeName(), bytecode);
        assert type != null;
        final Object object = this.allocateInstance(type);
        if (complex) {
            for (Map.Entry<Member, Object> entry : fields.entrySet())
                this.setField(object, entry.getKey().name(), entry.getValue());
            this.setField(object, "executor", executor);
            this.setField(object, "methods", finished.toArray(new Member[0]));
        } else { // trivial
            this.putValue(object, 12, executor);
            this.putValue(object, 16, finished.toArray(new Member[0]));
        }
        return (Template) object;
    }

    protected byte[] writeCode() {
        this.writer.setType(internal);
        this.writer.setSuperType(top != null && !top.isInterface() ? top : Object.class);
        this.writer.setInterfaces(this.getInterfaces());
        this.writer.setModifiers(Access.PUBLIC, Access.FINAL, Access.SYNTHETIC);
        this.writer.field().setModifiers(Access.PROTECTED, Access.TRANSIENT)
            .signature(new Signature("executor", MethodExecutor.class));
        this.writer.field().setModifiers(Access.PROTECTED, Access.TRANSIENT)
            .signature(new Signature("methods", Member[].class));
        for (final Member erasure : fields.keySet()) {
            this.writer.field().named(erasure.name()).signature(erasure.asFieldErasure());
        }
        if (top != null && top != Object.class) this.scrapeMethods(top);
        for (final Class<?> template : interfaces) this.scrapeMethods(template);
        return writer.bytecode();
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

    protected Type[] getInterfaces() {
        final Set<Type> types = new HashSet<>();
        if (top.isInterface()) types.add(Type.of(top));
        for (final Class<?> type : interfaces) types.add(Type.of(type));
        return types.toArray(new Type[0]);
    }

    protected void scrapeMethods(Class<?> template) {
        for (final Method method : template.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isFinal(method.getModifiers())) continue;
            if (Modifier.isPrivate(method.getModifiers())) continue;
            final Member erasure = new Member(method);
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
        final Member erasure = new Member(method);
        final MethodWriter writer;
        if (overrides.containsKey(erasure)) writer = this.overrides.get(erasure);
        else writer = this.handler;
        writer.write(this, method, this.writer, internal, index);
    }

}
