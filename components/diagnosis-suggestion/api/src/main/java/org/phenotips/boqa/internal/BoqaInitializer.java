/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.phenotips.boqa.internal;

import org.phenotips.boqa.DiagnosisService;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;


/**
 * Hook into phenotips startup to pre-initialize BOQA.
 * Created by meatcar on 10/3/14.
 * @version $Id$
 */

@Component
@Named("boqainitializer")
@Singleton
public class BoqaInitializer implements EventListener
{
    @Inject
    private DiagnosisService service;


    /**
     * @return the name of the eventlistener
     */
    public String getName() {
        return "boqainitializer";
    }

    /**
     * @return the events to listen to
     */
    public List<Event> getEvents() {
        return Arrays.<Event>asList(new ApplicationStartedEvent());
    }

    /**
     * @param event the event to handle
     * @param o an object related to the event
     * @param o2 another object related to the event
     */
    public void onEvent(Event event, Object o, Object o2) {
        // don't do anything, just injecting the diagnosis service.
    }
}
