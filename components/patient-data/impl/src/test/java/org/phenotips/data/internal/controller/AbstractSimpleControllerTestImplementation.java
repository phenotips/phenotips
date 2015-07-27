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

    @Override
    protected List<String> getProperties() {
        List<String> properties = new ArrayList<>();
        properties.add("property1");
        properties.add("property2");
        properties.add("property3");
        return properties;
    }

    @Override
    protected String getJsonPropertyName() {
        return "TEST";
    }

    @Override
    public String getName() {
        return "test";
    }
}
