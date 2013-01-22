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
package edu.toronto.cs.phenotips.hpoa.utils.graph;

import java.util.List;

public interface IDAGNode extends CoreNode {
	public boolean addParent(String parentId);
	
	public boolean addParent(IDAGNode parent);
	public boolean removeParent(String parentId);
	public boolean removeParent(IDAGNode parent);
	public boolean hasParent(String parentId);
	public boolean hasParent(IDAGNode parent);
	
	public boolean addChild(String childId);
	public boolean addChild(IDAGNode child);
	public boolean removeChild(String childId);
	public boolean removeChild(IDAGNode child);
	public boolean hasChild(String childId);
	public boolean hasChild(IDAGNode child);
	
	public List<String> getParents();
	public List<String> getChildren();
}
