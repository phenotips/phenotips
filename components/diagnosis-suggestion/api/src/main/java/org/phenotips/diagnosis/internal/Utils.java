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
package org.phenotips.diagnosis.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import java.io.IOException;

import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;

/**
 * The role used for loading data for BOQA's consumption.
 *
 * @since 1.1M1
 * @version $Id$
 */
@Unstable
@Role
public interface Utils
{
    /**
     * Loads an ontology graph and an association container. Taken from a benchmark class in ontologizer.
     *
     * @param oboFileName the name of the file to load the graph from
     * @param associationFileName the name of the file to load the association container from
     * @throws java.lang.InterruptedException if the notification process fails
     * @throws IOException if the graph or the association container fail to load
     */
    void loadDataFiles(String oboFileName, String associationFileName) throws InterruptedException, IOException;

    /**
     * @return ontology graph which could be null
     */
    Ontology getGraph();

    /**
     * @return the association container which could be null
     */
    AssociationContainer getDataAssociation();
}
