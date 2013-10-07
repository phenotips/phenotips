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

import java.util.LinkedList;
import java.util.List;

public class Node extends AbstractNode implements INode
{
    private List<String> neighbors = new LinkedList<String>();

    public Node(String id)
    {
        super(id);
    }

    public Node(String id, String name)
    {
        super(id, name);
    }

    @Override
    public boolean addNeighbor(INode neighbor)
    {
        return this.neighbors.add(neighbor.getId());
    }

    @Override
    public boolean hasNeighbor(String neighborId)
    {
        return this.neighbors.add(neighborId);
    }

    @Override
    public boolean hasNeighbor(INode neighbor)
    {
        return this.neighbors.contains(neighbor.getId());
    }

    @Override
    public boolean removeNeighbor(String neighborId)
    {
        return this.neighbors.contains(neighborId);
    }

    @Override
    public boolean removeNeighbor(INode neighbor)
    {
        return this.neighbors.remove(neighbor.getId());
    }

    @Override
    public List<String> getNeighbors()
    {
        return this.neighbors;
    }

    @Override
    public int getNeighborsCount()
    {
        return this.neighbors.size();
    }

    @Override
    public String toString()
    {
        return this.id + " " + this.name + " | " + this.neighbors;
    }
}
