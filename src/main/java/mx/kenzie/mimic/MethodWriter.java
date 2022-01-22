package mx.kenzie.mimic;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.objectweb.asm.Opcodes.*;

class AcquireForwardingWriter extends ForwardingWriter {
    
    AcquireForwardingWriter(Class<?> type, int index) {
        super(type, index);
    }
    
    @Override
    protected void acquire(MethodVisitor visitor, Method method, String internal) {
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "supplier_" + index, Type.getDescriptor(Supplier.class));
        visitor.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(Supplier.class), "get", "()Ljava/lang/Object;", true);
        visitor.visitTypeInsn(CHECKCAST, Type.getInternalName(target));
    }
    
}

class ForwardingWriter extends MethodWriter {
    
    protected Class<?> target;
    protected int index;
    
    ForwardingWriter(Class<?> target, int index) {
        this.target = target;
        this.index = index;
    }
    
    ForwardingWriter() {
        this.target = null;
    }
    
    @Override
    protected void write(MimicGenerator generator, Method original, ClassWriter writer, String internal, int index) {
        final Method method = this.findSimilar(original, target);
        final MethodVisitor visitor = writer.visitMethod(1 | 16 | 4096, method.getName(), Type.getMethodDescriptor(method), null, null);
        visitor.visitCode();
        this.acquire(visitor, method, internal);
        int argumentIndex = 0;
        for (Class<?> parameter : method.getParameterTypes()) {
            visitor.visitVarInsn(20 + instructionOffset(parameter), ++argumentIndex);
            argumentIndex += wideIndexOffset(parameter);
        }
        final Class<?> owner = method.getDeclaringClass();
        final int code;
        if (method.getDeclaringClass().isInterface()) code = INVOKEINTERFACE;
        else code = INVOKEVIRTUAL;
        visitor.visitMethodInsn(code, Type.getInternalName(method.getDeclaringClass()), method.getName(), Type.getMethodDescriptor(method), owner.isInterface());
        if (method.getReturnType() == void.class) {
            visitor.visitInsn(RETURN);
        } else {
            visitor.visitInsn(171 + instructionOffset(method.getReturnType()));
        }
        final int max = 1 + method.getParameterCount() + wideIndexOffset(method.getParameterTypes(), method.getReturnType());
        visitor.visitMaxs(max + 1, max);
        visitor.visitEnd();
    }
    
    protected void acquire(MethodVisitor visitor, Method method, String internal) {
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "target_" + index, Type.getDescriptor(target));
    }
    
}

class SpecificExecutorWriter extends MethodWriter {
    
    protected final int index;
    
    SpecificExecutorWriter(int index) {
        this.index = index;
    }
    
    @Override
    protected void write(MimicGenerator generator, Method method, ClassWriter writer, String internal, final int index) {
        final MethodVisitor visitor = writer.visitMethod(1 | 16 | 4096, method.getName(), Type.getMethodDescriptor(method), null, null);
        visitor.visitCode();
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "executor_" + this.index, "Lmx/kenzie/mimic/MethodExecutor;");
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitVarInsn(ALOAD, 0);
        visitor.visitFieldInsn(GETFIELD, internal, "methods", "[Lmx/kenzie/mimic/MethodErasure;");
        visitor.visitIntInsn(BIPUSH, index);
        visitor.visitInsn(AALOAD);
        visitor.visitIntInsn(BIPUSH, method.getParameterCount());
        visitor.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        int argumentIndex = 0;
        int storeIndex = -1;
        for (final Class<?> parameter : method.getParameterTypes()) {
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
}

public class MethodWriter {
    
    protected void write(MimicGenerator generator, Method method, ClassWriter writer, String internal, final int index) {
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
        for (final Class<?> parameter : method.getParameterTypes()) {
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
        if (value == char.class)
            visitor.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Character.class), "valueOf", "(Z)Ljava/lang/Character;", false);
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
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "byteValue", "()B", false);
        if (parameter == short.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "shortValue", "()S", false);
        if (parameter == int.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "intValue", "()I", false);
        if (parameter == long.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "longValue", "()J", false);
        if (parameter == float.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "floatValue", "()F", false);
        if (parameter == double.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Number.class), "doubleValue", "()D", false);
        if (parameter == boolean.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Boolean.class), "booleanValue", "()Z", false);
        if (parameter == char.class)
            visitor.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Character.class), "charValue", "()C", false);
    }
    
    protected int wideIndexOffset(Class<?>[] params, Class<?> ret) {
        int i = 0;
        for (Class<?> param : params) {
            i += wideIndexOffset(param);
        }
        return Math.max(i, wideIndexOffset(ret));
    }
    
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
    
    protected Method findSimilar(Method method, Class<?> type) {
        try {
            return type.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method;
        }
    }
    
    protected Method findSimilar(MethodErasure erasure, Class<?> type) {
        try {
            return type.getMethod(erasure.name(), erasure.parameters());
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
    
}
