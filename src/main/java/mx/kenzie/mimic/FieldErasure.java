package mx.kenzie.mimic;

import java.io.Serializable;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.util.Optional;

public record FieldErasure(Class<?> owner, String name, Class<?> type) implements Constable, Serializable {
    
    @Override
    public Optional<? extends ConstantDesc> describeConstable() {
        return Optional.empty();
    }
    
}
