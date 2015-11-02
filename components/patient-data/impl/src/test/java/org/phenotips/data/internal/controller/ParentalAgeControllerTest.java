package org.phenotips.data.internal.controller;

import org.junit.Rule;
import org.junit.Test;
import org.phenotips.data.PatientDataController;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

public class ParentalAgeControllerTest {

    @Rule
    MockitoComponentMockingRule<PatientDataController<Integer>> mocker =
            new MockitoComponentMockingRule<PatientDataController<Integer>>(ParentalAgeController.class);

    private ParentalAgeController parentalAgeController;

    @Test
    public void loadDefaultBehaviourTest(){

    }

    @Test
    public void loadHandlesExceptions(){

    }

    @Test
    public void saveDefaultBehaviourTest(){

    }

    @Test
    public void saveHandlesExceptions(){

    }
}
