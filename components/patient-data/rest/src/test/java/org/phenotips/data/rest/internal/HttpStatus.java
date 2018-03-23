/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.rest.internal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.rules.ExpectedException;

/**
 * JUnit rule that expects a specific HTTP status exception to be thrown from a REST method. To use, first declare an
 * {@link ExpectedException} rule in your test class:
 *
 * <pre>
 * &#64;Rule
 * public ExpectedException exception = ExpectedException.none();
 * </pre>
 *
 * Then, in a method that expects such an exception, place this call at the start of the method body:
 *
 * <pre>
 * this.exception.expect(HttpStatus.of(404));
 * </pre>
 *
 * or
 *
 * <pre>
 * this.exception.expect(HttpStatus.of(Response.Status.FORBIDDEN));
 * </pre>
 *
 * @version $Id$
 */
public final class HttpStatus extends BaseMatcher<WebApplicationException>
{
    private final int expectedStatus;

    private HttpStatus(int expectedStatus)
    {
        this.expectedStatus = expectedStatus;
    }

    static HttpStatus of(int expectedStatus)
    {
        return new HttpStatus(expectedStatus);
    }

    static HttpStatus of(Response.Status status)
    {
        return new HttpStatus(status.getStatusCode());
    }

    @Override
    public boolean matches(Object item)
    {
        if (!(item instanceof WebApplicationException)) {
            return false;
        }
        WebApplicationException other = (WebApplicationException) item;
        return other.getResponse().getStatus() == this.expectedStatus;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("HTTP status code " + this.expectedStatus);
    }
}
