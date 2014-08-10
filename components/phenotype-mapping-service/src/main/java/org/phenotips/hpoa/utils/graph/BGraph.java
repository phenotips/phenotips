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
package org.phenotips.hpoa.utils.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class BGraph<T extends Node>
{
    public enum Side
    {
        L,
        R;
    }

    private Map<String, Side> nodeSides = new HashMap<String, Side>();

    private Map<String, T> lNodes = new TreeMap<String, T>();

    private Map<String, T> rNodes = new TreeMap<String, T>();

    protected Map<Side, Map<String, T>> nodes = new HashMap<Side, Map<String, T>>()
    {
        private static final long serialVersionUID = 201204022008L;
        {
            put(Side.L, BGraph.this.lNodes);
            put(Side.R, BGraph.this.rNodes);
        }
    };

    public void clear()
    {
        this.lNodes.clear();
        this.rNodes.clear();
    }

    public void addNode(T n, Side s)
    {
        this.nodes.get(s).put(n.getId(), n);
        this.nodeSides.put(n.getId(), s);
    }

    private T addIfAbsent(T node, Side s)
    {
        T existingNode = this.nodes.get(s).get(node.getId());
        if (existingNode == null) {
            this.addNode(node, s);
            return node;
        }
        return existingNode;
    }

    public void addConnection(T lNode, T rNode)
    {
        T crtLNode = addIfAbsent(lNode, Side.L);
        T crtRNode = addIfAbsent(rNode, Side.R);
        crtLNode.addNeighbor(crtRNode);
        crtRNode.addNeighbor(crtLNode);
    }

    public void addConnection(Map<Side, T> nodes)
    {
        T lNode = nodes.get(Side.L);
        T rNode = nodes.get(Side.R);
        if (lNode != null && rNode != null) {
            addConnection(lNode, rNode);
        } else {
            System.err.println("Wrong arguments: " + nodes);
        }
    }

    public Set<String> getNodesIds(Side s)
    {
        return this.nodes.get(s).keySet();
    }

    public Collection<T> getNodes(Side s)
    {
        return this.nodes.get(s).values();
    }

    protected T getNode(String id, Side s)
    {
        return this.nodes.get(s).get(id);
    }

    public T getNode(String id)
    {
        Side side = this.nodeSides.get(id);
        if (side != null) {
            return getNode(id, side);
        }
        return null;
    }

    public int size()
    {
        return this.lNodes.size() + this.rNodes.size();
    }

    public int size(Side s)
    {
        return this.nodes.get(s).size();
    }

    public List<String> getNeighborIds(T node)
    {
        return node.getNeighbors();
    }

    public List<String> getNeighborIds(String nodeId)
    {
        T node = this.getNode(nodeId);
        return node.getNeighbors();
    }
}
