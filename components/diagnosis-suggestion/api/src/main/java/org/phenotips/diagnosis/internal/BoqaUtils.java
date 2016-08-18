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
package org.phenotips.diagnosis.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;

import ontologizer.FileCache;
import ontologizer.association.AssociationContainer;
import ontologizer.go.Ontology;
import ontologizer.worksets.WorkSet;
import ontologizer.worksets.WorkSetLoadThread;

/**
 * Utility functions for BOQA's integration into PhenoTips. Currently deals only with data loading.
 *
 * @since 1.1M2
 * @version $Id$
 */
@Component
public class BoqaUtils implements Utils
{
    private Ontology graph;

    private AssociationContainer dataAssociation;

    @Inject
    private Environment env;

    /**
     * Loads an ontology graph and an association container. Taken from a benchmark class in ontologizer.
     *
     * @param oboFileName the name of the file to load the graph from
     * @param associationFileName the name of the file to load the association container from
     * @throws java.lang.InterruptedException if the notification process fails
     * @throws IOException if the graph or the association container fail to load
     */
    public void loadDataFiles(String oboFileName, String associationFileName) throws InterruptedException, IOException
    {
        File workspace = new File(env.getTemporaryDirectory(), "ontologizer");
        if (!workspace.exists()) {
            workspace.mkdirs();
        }
        File cacheFile = new File(workspace, ".cache");
        if (cacheFile.exists()) {
            FileCache.setCacheDirectory(cacheFile.getAbsolutePath());
        }
        /* todo. Test if the name change has an effect. */
        final WorkSet ws = new WorkSet("PhenoTips");
        ws.setOboPath(oboFileName);
        ws.setAssociationPath(associationFileName);
        final Object notify = new Object();

        synchronized (notify) {
            WorkSetLoadThread.obtainDatafiles(ws,
                new Runnable()
                {
                    public void run()
                    {
                        graph = WorkSetLoadThread.getGraph(ws.getOboPath());
                        dataAssociation = WorkSetLoadThread.getAssociations(ws.getAssociationPath());
                        synchronized (notify) {
                            notify.notifyAll();
                        }
                    }
                });
            notify.wait();
        }

        String exceptionMessage = "Couldn't open file %s";
        if (this.graph == null) {
            throw new IOException(String.format(exceptionMessage, oboFileName));
        }
        if (this.dataAssociation == null) {
            throw new IOException(String.format(exceptionMessage, associationFileName));
        }
    }

    /**
     * @return ontology graph which could be null
     */
    public Ontology getGraph()
    {
        return graph;
    }

    /**
     * @return the association container which could be null
     */
    public AssociationContainer getDataAssociation()
    {
        return dataAssociation;
    }
}
