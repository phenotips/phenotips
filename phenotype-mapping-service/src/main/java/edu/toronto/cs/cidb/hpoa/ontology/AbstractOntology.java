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
package edu.toronto.cs.cidb.hpoa.ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import edu.toronto.cs.cidb.hpoa.utils.graph.DAG;
import edu.toronto.cs.cidb.hpoa.utils.graph.DAGNode;
import edu.toronto.cs.cidb.hpoa.utils.graph.IDAGNode;
import edu.toronto.cs.cidb.solr.SolrScriptService;

public abstract class AbstractOntology extends DAG<OntologyTerm> implements Ontology
{
    public final static String PARENT_ID_REGEX = "^([A-Z]{2}\\:[0-9]{7})\\s*!\\s*.*";

    private final static String TERM_MARKER = "[Term]";

    private final static String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private final Map<String, String> alternateIdMapping = Collections.synchronizedMap(new HashMap<String, String>());

    private IDAGNode root;

    private final Map<String, Set<String>> ancestorCache = Collections
        .synchronizedMap(new HashMap<String, Set<String>>());

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#load(edu.toronto.cs.cidb.solr.SolrScriptService)
     */
    public int load(SolrScriptService source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        clear();
        TermData data = new TermData();
        SolrDocumentList results = source.search("*:*");
        for (SolrDocument result : results) {
            for (String name : result.getFieldNames()) {
                Object val = result.get(name);
                if (val instanceof Collection< ? >) {
                    data.put(name, (Collection<String>) val);
                } else {
                    data.addTo(name, (String) val);
                }
            }
            if (data.isValid()) {
                this.createOntologyTerm(data);
            }
        }
        cleanArcs();
        // How much did we load:
        return size();
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#load(java.io.File)
     */
    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        clear();
        TermData data = new TermData();
        try {
            BufferedReader in = new BufferedReader(new FileReader(source));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (data.isValid()) {
                        this.createOntologyTerm(data);
                    }
                    data.clear();
                    continue;
                }
                String pieces[] = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                if (pieces.length != 2) {
                    continue;
                }
                String name = pieces[0], value = pieces[1];
                data.addTo(name, value);
            }
            if (data.isValid()) {
                this.createOntologyTerm(data);
            }
            in.close();
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
        cleanArcs();
        // How much did we load:
        return size();
    }

    private void cleanArcs()
    {
        Set<IDAGNode> roots = new HashSet<IDAGNode>();
        // Redo all links
        for (DAGNode n : getNodes()) {
            if (n.getParents().size() == 0) {
                roots.add(n);
                continue;
            }
            for (String parentId : n.getParents()) {
                DAGNode p = getTerm(parentId);
                if (p != null) {
                    p.addChild(n);
                } else {
                    System.err.println("[WARNING] Node with id " + n.getId() + " has parent " + parentId
                        + ", but no node " + parentId + " exists in the graph!\n");
                }
            }
        }
        if (roots.size() == 0) {
            System.err.println("Something's wrong, this directed graph is DEFINITELY not acyclic!");
        } else if (roots.size() == 1) {
            for (IDAGNode n : roots) {
                this.root = n;
            }
        } else {
            this.root = new OntologyTerm("", "FAKE ROOT");
            for (IDAGNode n : roots) {
                this.root.addChild(n);
                n.addParent(this.root);
            }
        }
    }

    protected void createOntologyTerm(TermData data)
    {
        OntologyTerm term = new OntologyTerm(data);
        this.addNode(term);
        this.alternateIdMapping.put(term.getId(), term.getId());
        for (String altId : data.safeGet(TermData.ALT_ID_FIELD_NAME)) {
            this.alternateIdMapping.put(altId, term.getId());
        }
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getRealId(java.lang.String)
     */
    public String getRealId(String id)
    {
        return this.alternateIdMapping.get(id);
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getTerm(java.lang.String)
     */
    public OntologyTerm getTerm(String id)
    {
        String realId = this.getRealId(id);
        if (realId != null) {
            return (OntologyTerm) this.getNode(realId);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getName(java.lang.String)
     */
    public String getName(String id)
    {
        DAGNode node = this.getTerm(id);
        if (node != null) {
            return node.getName();
        }
        return id;
    }

    public void printAltMapping(PrintStream out)
    {
        printAltMapping(out, false);
    }

    public void printAltMapping(PrintStream out, boolean all)
    {
        for (String key : this.alternateIdMapping.keySet()) {
            if (all || !key.equals(this.alternateIdMapping.get(key))) {
                out.println(key + " -> " + this.alternateIdMapping.get(key));
            }

        }
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getRootId()
     */
    public String getRootId()
    {
        return this.root.getId();
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getRoot()
     */
    public IDAGNode getRoot()
    {
        return this.root;
    }

    protected Set<String> findAncestors(String id)
    {
        Set<String> result = new HashSet<String>();
        if (this.getTerm(id) == null) {
            return result;
        }
        Set<String> front = new HashSet<String>();
        Set<String> newFront = new HashSet<String>();
        front.add(this.getRealId(id));
        result.add(this.getRealId(id));
        while (!front.isEmpty()) {
            for (String nextTermId : front) {

                for (String parentTermId : this.getTerm(nextTermId).getParents()) {
                    if (!result.contains(parentTermId)) {
                        newFront.add(parentTermId);
                        result.add(parentTermId);
                    }
                }
            }
            front.clear();
            front.addAll(newFront);
            newFront.clear();
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see edu.toronto.cs.cidb.hpoa.ontology.Ontology#getAncestors(java.lang.String)
     */
    public synchronized Set<String> getAncestors(String termId)
    {
        if (this.ancestorCache.get(termId) == null) {
            this.ancestorCache.put(termId, this.findAncestors(termId));
        }
        return this.ancestorCache.get(termId);
    }
}
