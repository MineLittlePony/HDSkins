package com.minelittlepony.hdskins.net;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The remote net API level that this skin net implements.
 *
 * Current values are:
 *  - legacy
 *  - valhalla
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ServerType {

    String value();
}
