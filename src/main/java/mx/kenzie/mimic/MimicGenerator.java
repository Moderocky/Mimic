package mx.kenzie.mimic;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

public class MimicGenerator {
    
    private static volatile int counter;
    protected final ClassWriter writer;
    protected final String internal;
    protected final Class<?> top;
    protected final Class<?>[] interfaces;
    protected final List<MethodErasure> finished;
    protected int index;
    
    protected MimicGenerator(String location, Class<?> top, Class<?>... interfaces) {
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
        final boolean complex = !top.isInterface();
        final byte[] bytecode = writeCode();
        final Class<?> type = InternalAccess.loadClass(loader, internal.replace('/', '.'), bytecode);
        final Object object = this.allocateInstance(type);
        if (complex) {
            try {
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
        this.writer.visitField(0, "methods", "[Lmx/kenzie/mimic/MethodErasure;", null, null);
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
        final MethodVisitor visitor = writer.visitMethod(1 | 16 | 4096, method.getName(), Type.getMethodDescriptor(method), null, null);
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "executor", "Lmx/kenzie/mimic/MethodExecutor;");
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "methods", "[Lmx/kenzie/mimic/MethodErasure;");
        visitor.visitIntInsn(BIPUSH, index);
        visitor.visitInsn(AALOAD);
        visitor.visitIntInsn(BIPUSH, method.getParameterCount());
        visitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int argumentIndex = 0;
        int storeIndex = -1;
        for (Class<?> parameter : method.getParameterTypes()) {
            visitor.visitInsn(DUP);
            visitor.visitIntInsn(BIPUSH, ++storeIndex);
            visitor.visitVarInsn(20 + instructionOffset(parameter), ++argumentIndex);
            this.box(visitor, parameter);
            visitor.visitInsn(AASTORE);
            argumentIndex += wideIndexOffset(parameter);
        }
        visitor.visitMethodInsn(INVOKEINTERFACE, "mx/kenzie/mimic/MethodExecutor", "invoke", "(Ljava/lang/Object;Lmx/kenzie/mimic/MethodErasure;[Ljava/lang/Object;)Ljava/lang/Object;", true);
        if (method.getReturnType() == void.class) {
            visitor.visitInsn(POP);
            visitor.visitInsn(RETURN);
        } else {
            visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(getWrapperType(method.getReturnType())));
            this.unbox(visitor, method.getReturnType());
            visitor.visitInsn(171 + instructionOffset(method.getReturnType()));
        }
        visitor.visitMaxs(8, 1 + method.getParameterCount() + wideIndexOffset(method.getParameterTypes(), method.getReturnType()));
        visitor.visitEnd();
    }
    
    protected int instructionOffset(Class<?> type) {
        if (type == int.class) return 1;
        if (type == boolean.class) return 1;
        if (type == byte.class) return 1;
        if (type == short.class) return 1;
        if (type == long.class) return 2;
        if (type == float.class) return 3;
        if (type == double.class) return 4;
        if (type == void.class) return 6;
        return 5;
    }
    
    protected void box(MethodVisitor visitor, Class<?> value) {
        if (value == byte.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Byte.class), "valueOf", "(B)Ljava/lang/Byte;", false);
        if (value == short.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Short.class), "valueOf", "(S)Ljava/lang/Short;", false);
        if (value == int.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", "(I)Ljava/lang/Integer;", false);
        if (value == long.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Long.class), "valueOf", "(J)Ljava/lang/Long;", false);
        if (value == float.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Float.class), "valueOf", "(F)Ljava/lang/Float;", false);
        if (value == double.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Double.class), "valueOf", "(D)Ljava/lang/Double;", false);
        if (value == boolean.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Boolean.class), "valueOf", "(Z)Ljava/lang/Boolean;", false);
        if (value == void.class)
            visitor.visitInsn(ACONST_NULL);
    }
    
    protected int wideIndexOffset(Class<?> thing) {
        if (thing == long.class || thing == double.class) return 1;
        return 0;
    }
    
    protected Class<?> getWrapperType(Class<?> primitive) {
        if (primitive == byte.class) return Byte.class;
        if (primitive == short.class) return Short.class;
        if (primitive == int.class) return Integer.class;
        if (primitive == long.class) return Long.class;
        if (primitive == float.class) return Float.class;
        if (primitive == double.class) return Double.class;
        if (primitive == boolean.class) return Boolean.class;
        if (primitive == void.class) return Void.class;
        return primitive;
    }
    
    protected void unbox(MethodVisitor visitor, Class<?> parameter) {
        if (parameter == byte.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Byte.class), "byteValue", "()B", false);
        if (parameter == short.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Short.class), "shortValue", "()S", false);
        if (parameter == int.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Integer.class), "intValue", "()I", false);
        if (parameter == long.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Long.class), "longValue", "()J", false);
        if (parameter == float.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Float.class), "floatValue", "()F", false);
        if (parameter == double.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Double.class), "doubleValue", "()D", false);
        if (parameter == boolean.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
    }
    
    protected int wideIndexOffset(Class<?>[] params, Class<?> ret) {
        int i = 0;
        for (Class<?> param : params) {
            i += wideIndexOffset(param);
        }
        return Math.max(i, wideIndexOffset(ret));
    }
    
    //region Utilities
    protected void doTypeConversion(MethodVisitor visitor, Class<?> from, Class<?> to) {
        if (from == to) return;
        if (from == void.class || to == void.class) return;
        if (from.isPrimitive() && to.isPrimitive()) {
            final int opcode;
            if (from == float.class) {
                if (to == double.class) opcode = F2D;
                else if (to == long.class) opcode = F2L;
                else opcode = F2I;
            } else if (from == double.class) {
                if (to == float.class) opcode = D2F;
                else if (to == long.class) opcode = D2L;
                else opcode = D2I;
            } else if (from == long.class) {
                if (to == float.class) opcode = L2F;
                else if (to == double.class) opcode = L2D;
                else opcode = L2I;
            } else {
                if (to == float.class) opcode = I2F;
                else if (to == double.class) opcode = I2D;
                else if (to == byte.class) opcode = I2B;
                else if (to == short.class) opcode = I2S;
                else if (to == char.class) opcode = I2C;
                else opcode = I2L;
            }
            visitor.visitInsn(opcode);
        } else if (from.isPrimitive() ^ to.isPrimitive()) {
            throw new IllegalArgumentException("Type wrapping is currently unsupported due to side-effects: '" + from.getSimpleName() + "' -> '" + to.getSimpleName() + "'");
        } else visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(to));
    }
    //endregion
    
}
