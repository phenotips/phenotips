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
package org.phenotips.hpoa.annotation;

import org.phenotips.hpoa.ontology.Ontology;
import org.phenotips.hpoa.utils.graph.BGraph;

import org.xwiki.component.annotation.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

@Component
@Named("omim-hpo")
@Singleton
public class OmimHPOAnnotations extends AbstractHPOAnnotation
{
    public static final Side OMIM = BGraph.Side.L;

    private static final String OMIM_ANNOTATION_MARKER = "OMIM";

    private static final String SEPARATOR = "\t";

    private static final int MIN_EXPECTED_FIELDS = 8;

    public OmimHPOAnnotations(Ontology hpo)
    {
        super(hpo);
    }

    @Override
    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        clear();
        // Load data
        try {
            BufferedReader in = new BufferedReader(new FileReader(source));
            String line;
            Map<Side, AnnotationTerm> connection = new HashMap<Side, AnnotationTerm>();
            while ((line = in.readLine()) != null) {
                if (!line.startsWith(OMIM_ANNOTATION_MARKER)) {
                    continue;
                }
                String pieces[] = line.split(SEPARATOR, MIN_EXPECTED_FIELDS);
                if (pieces.length != MIN_EXPECTED_FIELDS) {
                    continue;
                }
                final String omimId = OMIM_ANNOTATION_MARKER + ":" + pieces[1], omimName = pieces[2], hpoId =
                    this.hpo.getRealId(pieces[4]), rel = pieces[3];
                if (!"NOT".equals(rel)) {
                    connection.clear();
                    connection.put(OMIM, new AnnotationTerm(omimId, omimName));
                    connection.put(HPO, new AnnotationTerm(hpoId));
                    this.addConnection(connection);
                }
            }
            in.close();
            propagateHPOAnnotations();
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("File does not exist");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.err.println("Could not locate source file: " + source.getAbsolutePath());
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        }
        return size();
    }

    public Set<String> getOMIMNodesIds()
    {
        return this.getNodesIds(OMIM);
    }

    public Collection<AnnotationTerm> getOMIMNodes()
    {
        return this.getNodes(OMIM);
    }

    public AnnotationTerm getOMIMNode(String omimId)
    {
        return this.getNode(omimId, OMIM);
    }
}
