package org.phenotips.data.internal.controller;

import org.xwiki.component.annotation.Component;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Component
@Named("test")
@Singleton
public class AbstractSimpleControllerTestImplementation extends AbstractSimpleController{

    protected static final String DATA_NAME = "test";

    protected static final String PROPERTY_1 = "property1";

    protected static final String PROPERTY_2 = "property2";

    protected static final String PROPERTY_3 = "property3";
    @Override
    protected List<String> getProperties() {
        List<String> properties = new ArrayList<>();
        properties.add(PROPERTY_1);
        properties.add(PROPERTY_2);
        properties.add(PROPERTY_3);
        return properties;
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
