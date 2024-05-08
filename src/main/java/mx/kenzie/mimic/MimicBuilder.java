package mx.kenzie.mimic;

import org.valross.foundation.detail.Member;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public interface MimicBuilder<Template> {

    MimicBuilder<Template> definer(ClassDefiner definer);

    MimicBuilder<Template> forward(Object object);

    <Type> MimicBuilder<Template> forward(Supplier<Type> supplier, Class<Type> type);

    default MimicBuilder<Template> override(MethodExecutor executor, Method... methods) {
        final Member[] erasures = new Member[methods.length];
        for (int i = 0; i < methods.length; i++) {
            erasures[i] = new Member(methods[i]);
        }
        return this.override(executor, erasures);
    }

    MimicBuilder<Template> override(MethodExecutor executor, Member... methods);

    MimicBuilder<Template> executor(MethodExecutor executor);

    Template build();

}

class SimpleMimicBuilder<Template> implements MimicBuilder<Template> {

    protected static final MethodExecutor DEFAULT = (object, erasure, args) -> {
        throw new RuntimeException("This method has no defined executor.");
    };
    protected final Map<Member, MethodWriter> overrides;
    protected final Map<Member, Object> fields;
    protected Class<Template> top;
    protected Class<?>[] interfaces;
    protected MethodExecutor executor = DEFAULT;
    protected int index;
    protected ClassDefiner definer;

    public SimpleMimicBuilder(Class<Template> top, Class<?>... interfaces) {
        this.top = top;
        this.interfaces = interfaces;
        this.overrides = new HashMap<>();
        this.fields = new HashMap<>();
    }

    @Override
    public MimicBuilder<Template> definer(ClassDefiner definer) {
        this.definer = definer;
        return this;
    }

    @Override
    public MimicBuilder<Template> forward(Object object) {
        final int index = ++this.index;
        final MethodWriter writer = new ForwardingWriter(object.getClass(), index);
        for (final Member erasure : this.scrapeMethods(object.getClass())) {
            this.overrides.put(erasure, writer);
        }
        this.fields.put(new Member(top, "target_" + index, object.getClass()), object);
        return this;
    }

    @Override
    public <Type> MimicBuilder<Template> forward(Supplier<Type> supplier, Class<Type> type) {
        final int index = ++this.index;
        final MethodWriter writer = new AcquireForwardingWriter(type, index);
        for (final Member erasure : this.scrapeMethods(type)) {
            this.overrides.put(erasure, writer);
        }
        this.fields.put(new Member(top, "supplier_" + index, Supplier.class), supplier);
        return this;
    }

    @Override
    public MimicBuilder<Template> override(MethodExecutor executor, Member... methods) {
        final int index = ++this.index;
        final MethodWriter writer = new SpecificExecutorWriter(index);
        for (final Member erasure : methods) {
            this.overrides.put(erasure, writer);
        }
        this.fields.put(new Member(top, "executor_" + index, MethodExecutor.class), executor);
        return this;
    }

    @Override
    public MimicBuilder<Template> executor(MethodExecutor executor) {
        this.executor = executor;
        return this;
    }

    @Override
    @SuppressWarnings("removal")
    public Template build() {
        final int index = MimicGenerator.count();
        final String name;
        if (definer != null && definer.getPackage() != null) name = definer.getPackage();
        else name = InternalAccess.getStrictPackageName(top, interfaces);
        final Module module = InternalAccess.getStrictModule(top, interfaces);
        final String path = name.replace('.', '/') + "/Mimic_" + index;
        final PrivilegedAction<ClassLoader> action = module::getClassLoader;
        final ClassLoader loader = java.security.AccessController.doPrivileged(action);
        final MimicGenerator generator = new MimicGenerator(path, top, interfaces);
        if (definer != null) generator.definer = definer;
        generator.overrides.putAll(this.overrides);
        generator.fields.putAll(this.fields);
        return generator.create(loader, executor);
    }


    protected List<Member> scrapeMethods(Class<?> template) {
        final List<Member> methods = new ArrayList<>();
        for (final Method method : template.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) continue;
            if (Modifier.isFinal(method.getModifiers())) continue;
            if (Modifier.isPrivate(method.getModifiers())) continue;
            final Member erasure = new Member(method);
            if (overrides.containsKey(erasure)) continue;
            methods.add(erasure);
        }
        return methods;
    }
}
