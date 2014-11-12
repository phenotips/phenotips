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

import java.util.List;

public interface IDAGNode extends CoreNode
{
    boolean addParent(String parentId);

    boolean addParent(IDAGNode parent);

    boolean removeParent(String parentId);

    boolean removeParent(IDAGNode parent);

    boolean hasParent(String parentId);

    boolean hasParent(IDAGNode parent);

    boolean addChild(String childId);

    boolean addChild(IDAGNode child);

    boolean removeChild(String childId);

    boolean removeChild(IDAGNode child);

    boolean hasChild(String childId);

    boolean hasChild(IDAGNode child);

    List<String> getParents();

    List<String> getChildren();
}
