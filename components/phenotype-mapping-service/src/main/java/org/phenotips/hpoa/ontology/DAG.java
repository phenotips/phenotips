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
package org.phenotips.hpoa.ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class DAG
{
    public final static String PARENT_ID_REGEX = "^([A-Z]{2}\\:[0-9]{7})\\s*!\\s*.*";

    private final static String TERM_MARKER = "[Term]";

    private final static String FIELD_NAME_VALUE_SEPARATOR = "\\s*:\\s+";

    private final TreeMap<String, Node> nodes = new TreeMap<String, Node>();

    public int load(File source)
    {
        // Make sure we can read the data
        if (source == null) {
            return -1;
        }
        // Load data
        this.nodes.clear();
        TermData data = new TermData();
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(source));
            String line;
            while ((line = in.readLine()) != null) {
                if (line.trim().equalsIgnoreCase(TERM_MARKER)) {
                    if (data.getId() != null) {
                        this.nodes.put(data.getId(), new Node(data));
                        data.clear();
                    }
                    continue;
                }
                String pieces[] = line.split(FIELD_NAME_VALUE_SEPARATOR, 2);
                if (pieces.length != 2) {
                    continue;
                }
                String name = pieces[0], value = pieces[1];
                data.addTo(name, value);
            }
            if (data.getId() != null) {
                this.nodes.put(data.getId(), new Node(data));
            }
        } catch (NullPointerException ex) {
            ex.printStackTrace();
            System.err.println("File does not exist");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            System.err.println("Could not locate source file: " + source.getAbsolutePath());
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            ex.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    // Closing a stream shouldn't fail
                }
            }
        }

        // Redo all links
        for (Node n : this.nodes.values()) {
            for (String parentId : n.getParents()) {
                Node p = this.nodes.get(parentId);
                if (p != null) {
                    p.addChild(n);
                } else {
                    System.err.println("[WARNING] Node with id " + n.getId() + " has parent " + parentId
                        + ", but no node " + parentId + "exists in the graph!\n");
                }
            }
        }
        // How much did we load:
        return this.nodes.size();
    }

    public Map<String, Node> getNodesMap()
    {
        return this.nodes;
    }

    public Set<String> getNodesIds()
    {
        return this.nodes.keySet();
    }

    public Collection<Node> getNodes()
    {
        return this.nodes.values();
    }

    public Node getNode(String id)
    {
        return this.nodes.get(id);
    }
}
