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
