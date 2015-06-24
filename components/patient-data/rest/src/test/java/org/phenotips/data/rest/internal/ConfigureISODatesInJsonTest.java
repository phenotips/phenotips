package org.phenotips.data.rest.internal;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig.Feature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.restlet.engine.Engine;
import org.restlet.engine.converter.ConverterHelper;
import org.restlet.ext.jackson.JacksonConverter;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;


import java.util.LinkedList;
import java.util.List;

/**
 * Created by Dean on 2015-06-24.
 */
public class ConfigureISODatesInJsonTest{

    @Rule
    public MockitoComponentMockingRule<EventListener> mocker =
        new MockitoComponentMockingRule<EventListener>(ConfigureISODatesInJson.class);

    @Mock
    private Event event;

    @Mock
    private Object source;

    @Mock
    private Object data;

    @Mock
    private Engine engine;

    @Mock
    private ConverterHelper converter;

    @Mock
    private JacksonConverter jacksonConverter;

    @Mock
    private ObjectMapper objectMapper;

    private List<ConverterHelper> converterList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        converterList = new LinkedList<>();
    }

    @Test
    public void verifyOnEventConfiguresProperly() throws ComponentLookupException {
        converterList.add(converter);
        converterList.add(jacksonConverter);
        Engine.setInstance(engine);
        Mockito.when(engine.getRegisteredConverters()).thenReturn(converterList);
        Mockito.when(jacksonConverter.getObjectMapper()).thenReturn(objectMapper);

        this.mocker.getComponentUnderTest().onEvent(event, source, data);

        Mockito.verify(objectMapper).configure(Feature.WRITE_DATES_AS_TIMESTAMPS, false);


    }
}
