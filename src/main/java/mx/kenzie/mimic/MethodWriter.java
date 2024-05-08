package mx.kenzie.mimic;

import org.valross.foundation.assembler.code.Instruction;
import org.valross.foundation.assembler.tool.Access;
import org.valross.foundation.assembler.tool.ClassFileBuilder;
import org.valross.foundation.assembler.tool.CodeBuilder;
import org.valross.foundation.assembler.tool.MethodBuilder;
import org.valross.foundation.detail.Member;
import org.valross.foundation.detail.Signature;
import org.valross.foundation.detail.Type;

import java.lang.reflect.Method;
import java.util.function.Supplier;

import static org.valross.foundation.assembler.code.OpCode.*;

class AcquireForwardingWriter extends ForwardingWriter {

    AcquireForwardingWriter(Class<?> type, int index) {
        super(type, index);
    }

    @Override
    protected void acquire(CodeBuilder visitor, Method method, Type internal) {
        visitor.write(ALOAD_0);
        visitor.write(GETFIELD.field(internal, "supplier_" + index, Supplier.class));
        visitor.write(INVOKEINTERFACE.method(true, Supplier.class, Object.class, "get"));
        visitor.write(CHECKCAST.type(target));
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
    protected void write(MimicGenerator generator, Method original, ClassFileBuilder writer, Type internal,
                         int index) {
        final Method method = this.findSimilar(original, target);
        final MethodBuilder visitor = writer.method().addModifiers(Access.PUBLIC, Access.FINAL, Access.SYNTHETIC)
            .named(method.getName()).signature(new Signature(method));
        CodeBuilder code = visitor.code();
        this.acquire(code, method, internal);
        int argumentIndex = 0;
        for (Class<?> parameter : method.getParameterTypes()) {
            code.write(ALOAD.var(parameter, ++argumentIndex));
            argumentIndex += wideIndexOffset(parameter);
        }
        final Class<?> owner = method.getDeclaringClass();
        final Member member = new Member(method);
        if (owner.isInterface())
            code.write(INVOKEINTERFACE.interfaceMethod(member));
        else code.write(INVOKEVIRTUAL.method(member));
        if (method.getReturnType() == void.class) {
            code.write(RETURN);
        } else {
            code.write((byte) (171 + instructionOffset(method.getReturnType())));
        }
    }

    protected void acquire(CodeBuilder visitor, Method method, Type internal) {
        visitor.write(ALOAD_0, GETFIELD.field(internal, "target_" + index, target));
    }

}

class SpecificExecutorWriter extends MethodWriter {

    protected final int index;

    SpecificExecutorWriter(int index) {
        this.index = index;
    }

    @Override
    protected void write(MimicGenerator generator, Method method, ClassFileBuilder writer, Type internal,
                         final int index) {
        final MethodBuilder visitor = writer.method().addModifiers(Access.PUBLIC, Access.FINAL, Access.SYNTHETIC)
            .named(method.getName()).signature(new Signature(method));
        final CodeBuilder code = visitor.code();
        code.write(ALOAD_0, GETFIELD.field(internal, "executor_" + this.index, MethodExecutor.class));
        code.write(ALOAD_0, ALOAD_0, GETFIELD.field(internal, "methods", Signature[].class));
        code.write(BIPUSH.value(index), AALOAD, BIPUSH.value(method.getParameterCount()));
        code.write(ANEWARRAY.type(Object.class));
        int argumentIndex = 0;
        int storeIndex = -1;
        for (final Class<?> parameter : method.getParameterTypes()) {
            code.write(DUP, BIPUSH.value(++storeIndex));
            code.write(ALOAD.var(parameter, ++argumentIndex));
            this.box(code, parameter);
            code.write(AASTORE);
            argumentIndex += wideIndexOffset(parameter);
        }
        code.write(INVOKEINTERFACE.method(true, MethodExecutor.class, Object.class, "invoke", Object.class,
            Signature.class, Object[].class));
        if (method.getReturnType() == void.class) {
            code.write(POP, RETURN);
        } else {
            code.write(CHECKCAST.type(this.getWrapperType(method.getReturnType())));
            this.unbox(code, method.getReturnType());
            code.write((byte) (171 + instructionOffset(method.getReturnType())));
        }
    }

}

public class MethodWriter {

    protected void write(MimicGenerator generator, Method method, ClassFileBuilder writer, Type internal,
                         final int index) {
        final MethodBuilder visitor = writer.method()
            .addModifiers(Access.PUBLIC, Access.FINAL, Access.SYNTHETIC)
            .named(method.getName()).signature(new Signature(method));
        final CodeBuilder code = visitor.code();
        code.write(ALOAD_0).write(GETFIELD.field(internal, "executor", MethodExecutor.class)).write(ALOAD_0);
        code.write(ALOAD_0, GETFIELD.field(internal, "methods", Signature[].class));
        code.write(BIPUSH.value(index), AALOAD, BIPUSH.value(method.getParameterCount()));
        code.write(ANEWARRAY.type(Object.class));
        int argumentIndex = 0;
        int storeIndex = -1;
        for (final Class<?> parameter : method.getParameterTypes()) {
            code.write(DUP, BIPUSH.value(++storeIndex));
            code.write(ALOAD.var(parameter, ++argumentIndex));
            this.box(code, parameter);
            code.write(AASTORE);
            argumentIndex += wideIndexOffset(parameter);
        }
        code.write(INVOKEINTERFACE.method(true, MethodExecutor.class, Object.class, "invoke", Object.class,
            Signature.class, Object[].class));
        if (method.getReturnType() == void.class) {
            code.write(POP, RETURN);
        } else {
            code.write(CHECKCAST.type(this.getWrapperType(method.getReturnType())));
            this.unbox(code, method.getReturnType());
            code.write((byte) (171 + instructionOffset(method.getReturnType())));
        }
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

    protected void box(CodeBuilder visitor, Class<?> value) {
        if (value == byte.class)
            visitor.write(INVOKESTATIC.method(false, Byte.class, Byte.class, "valueOf", byte.class));
        if (value == short.class)
            visitor.write(INVOKESTATIC.method(false, Short.class, Short.class, "valueOf", short.class));
        if (value == int.class)
            visitor.write(INVOKESTATIC.method(false, Integer.class, Integer.class, "valueOf", int.class));
        if (value == long.class)
            visitor.write(INVOKESTATIC.method(false, Long.class, Long.class, "valueOf", long.class));
        if (value == float.class)
            visitor.write(INVOKESTATIC.method(false, Float.class, Float.class, "valueOf", float.class));
        if (value == double.class)
            visitor.write(INVOKESTATIC.method(false, Double.class, Double.class, "valueOf", double.class));
        if (value == boolean.class)
            visitor.write(INVOKESTATIC.method(false, Boolean.class, Boolean.class, "valueOf", boolean.class));
        if (value == char.class)
            visitor.write(INVOKESTATIC.method(false, Character.class, Character.class, "valueOf", char.class));
        if (value == void.class)
            visitor.write(ACONST_NULL);
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

    protected void unbox(CodeBuilder visitor, Class<?> parameter) {
        if (parameter == byte.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, byte.class, "byteValue"));
        if (parameter == short.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, short.class, "shortValue"));
        if (parameter == int.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, int.class, "intValue"));
        if (parameter == long.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, long.class, "longValue"));
        if (parameter == float.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, float.class, "floatValue"));
        if (parameter == double.class)
            visitor.write(INVOKEVIRTUAL.method(false, Number.class, double.class, "doubleValue"));
        if (parameter == boolean.class)
            visitor.write(INVOKEVIRTUAL.method(false, Boolean.class, boolean.class, "booleanValue"));
        if (parameter == char.class)
            visitor.write(INVOKEVIRTUAL.method(false, Character.class, char.class, "charValue"));
    }

    protected int wideIndexOffset(Class<?>[] params, Class<?> ret) {
        int i = 0;
        for (Class<?> param : params) {
            i += wideIndexOffset(param);
        }
        return Math.max(i, wideIndexOffset(ret));
    }

    protected void doTypeConversion(CodeBuilder visitor, Class<?> from, Class<?> to) {
        if (from == to) return;
        if (from == void.class || to == void.class) return;
        if (from.isPrimitive() && to.isPrimitive()) {
            final Instruction opcode;
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
            visitor.write(opcode);
        } else if (from.isPrimitive() ^ to.isPrimitive()) {
            throw new IllegalArgumentException("Type wrapping is currently unsupported due to side-effects: '" + from.getSimpleName() + "' -> '" + to.getSimpleName() + "'");
        } else visitor.write(CHECKCAST.type(to));
    }

    protected Method findSimilar(Method method, Class<?> type) {
        try {
            return type.getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            return method;
        }
    }

    protected Method findSimilar(Signature erasure, Class<?> type) {
        try {
            return type.getMethod(erasure.name(), Type.classArray(erasure.parameters()));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
