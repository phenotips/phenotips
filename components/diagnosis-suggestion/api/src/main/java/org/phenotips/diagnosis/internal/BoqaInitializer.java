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

package org.phenotips.diagnosis.internal;

import org.phenotips.diagnosis.DiagnosisService;

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
 *
 * @since 1.1M1
 * @version $Id$
 */

@Component
@Named("boqainitializer")
@Singleton
public class BoqaInitializer implements EventListener
{
    @SuppressWarnings("unused")
    @Inject
    private DiagnosisService service;

    @Override
    public String getName()
    {
        return "boqainitializer";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new ApplicationStartedEvent());
    }

    @Override
    public void onEvent(Event event, Object o, Object o2)
    {
        // don't do anything, just injecting the diagnosis service.
    }
}
