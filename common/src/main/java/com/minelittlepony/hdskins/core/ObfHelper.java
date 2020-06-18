package com.minelittlepony.hdskins.core;

import java.lang.reflect.Field;

public abstract class ObfHelper<Owner, Descriptor> {

    protected final Class<Owner> owner;
    protected final String name;
    protected final Class<?> desc;

    private Field cachedField;

    public ObfHelper(Class<Owner> owner, String name, Class<?> desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    protected abstract String mapFieldName();

    private Field getField() {
        if (cachedField == null) {
            try {
                Field f = owner.getDeclaredField(mapFieldName());
                f.setAccessible(true);

                cachedField = f;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cachedField;
    }

    @SuppressWarnings("unchecked")
    public Descriptor get(Owner owner) {
        try {
            return (Descriptor) getField().get(owner);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void set(Owner owner, Descriptor value) {
        try {
            getField().set(owner, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s.%s", owner.getSimpleName(), getField().getName());
    }
}
