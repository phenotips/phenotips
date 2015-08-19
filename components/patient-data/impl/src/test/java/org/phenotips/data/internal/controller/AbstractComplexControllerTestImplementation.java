package org.phenotips.data.internal.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Dean on 2015-08-19.
 */
public class AbstractComplexControllerTestImplementation extends AbstractComplexController<String> {

    protected static String DATA_NAME = "test";

    protected static String PROPERTY_1 = "property1";

    protected static String PROPERTY_2 = "property2";

    protected static String PROPERTY_3 = "property3";

    @Override
    protected List<String> getBooleanFields() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getCodeFields() {
        return Collections.emptyList();
    }

    @Override
    protected List<String> getProperties() {
        return Arrays.asList(PROPERTY_1, PROPERTY_2, PROPERTY_3);
    }

    @Override
    protected String getJsonPropertyName() {
        return DATA_NAME;
    }

    @Override
    public String getName() {
        return DATA_NAME;
    }
}
