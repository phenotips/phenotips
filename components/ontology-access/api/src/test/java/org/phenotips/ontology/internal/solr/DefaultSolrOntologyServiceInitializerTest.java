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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.phenotips.ontology.internal.solr;

import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Rule;

/**
 * Test for the implementation of DefaultSolrOntologyServiceInitializer. It is the class that creates a connection to
 * the Solr server and creates a cache.
 */
public class DefaultSolrOntologyServiceInitializerTest
{
    @Rule
    public final MockitoComponentMockingRule<DefaultSolrOntologyServiceInitializer> mocker =
        new MockitoComponentMockingRule<DefaultSolrOntologyServiceInitializer>(
            DefaultSolrOntologyServiceInitializer.class);
}
