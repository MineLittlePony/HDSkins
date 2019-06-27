package com.minelittlepony.hdskins.util;

import static net.minecraft.util.Formatting.ITALIC;
import static net.minecraft.util.Formatting.RESET;
import static net.minecraft.util.Formatting.YELLOW;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class IndentedToStringStyle extends ToStringStyle {

    private static final long serialVersionUID = 2031593562293731492L;

    private static final ToStringStyle INSTANCE = new IndentedToStringStyle();

    private IndentedToStringStyle() {
        this.setFieldNameValueSeparator(": " + RESET + ITALIC);
        this.setContentStart(null);
        this.setFieldSeparator(System.lineSeparator() + "  " + RESET + YELLOW);
        this.setFieldSeparatorAtStart(true);
        this.setContentEnd(null);
        this.setUseIdentityHashCode(false);
        this.setUseShortClassName(true);
    }

    public static class Builder extends ToStringBuilder {
        public Builder(Object o) {
            super(o, IndentedToStringStyle.INSTANCE);
        }

        @Override
        public String build() {
            return YELLOW + super.build();
        }
    }
}
