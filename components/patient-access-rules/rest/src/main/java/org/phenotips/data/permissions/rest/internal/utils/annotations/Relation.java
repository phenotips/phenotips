package org.phenotips.data.permissions.rest.internal.utils.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by matthew on 2016-03-03.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Relation
{
    String value();
}
