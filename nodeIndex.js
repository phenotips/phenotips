var TmpIndex = Class.create({
  idToOriginalPos : {},
  originalPosToId : {},
  initialize : function() {},
  insert : function(node) {
    this.idToOriginalPos[node.getID()] = {x : node.getX(), y : node.getY()};
    this.originalPosToId[node.getX()+ "," + node.getY()] = node;
  },
  get : function(node) {
    return this.idToOriginalPos[node.getID()];
  },
  getAtPos : function(x, y) {
    return this.originalPosToId[x + ',' + y];
  },
  remove : function(node) {
    var pos = this.idToOriginalPos[node.getID()];
    if (pos) {
      delete this.idToOriginalPos[node.getID()];
      delete this.originalPosToId[pos.x + "," + pos.y];
    }
  },
  keys : function() {
    return Object.keys(this.idToOriginalPos);
  },
  poss : function() {
    return Object.keys(this.originalPosToId);
  },
  clear : function() {
    var _this = this;
    var keys = this.keys();
    keys.each(function(key){
      delete _this.idToOriginalPos[key];
    });
    keys = this.poss();
    keys.each(function(key){
      delete _this.originalPosToId[key];
    });
  }
});

var NodeIndex = Class.create({
  /** The grid unit. Nodes will be placed only at horizontal and vertical distances indicated here. */ 
  gridUnit : {
    x: 100,
    y: 200
  },
  /**
   * Initialize the node index
   * 
   * @param nodes optional set of nodes to insert in the index upon its creation
   */
  initialize : function(nodes) {
    var _this = this;
    this.nodes = {};
    this.positionTree = new kdTree([]);
    this.position2Node = {};
    this.xIndex = {};
    this.yIndex = {};
    this._addNode = this._addNode.bind(this);
    nodes && nodes.each(this._addNode);
    this.tmpIndex = new TmpIndex();
  },

  /**
   * [Internal method] return the closest position in the grid for a 2D point
   * 
   * @param point an object with two mandatory numeric fields, x and y
   * @return an object with two numeric fields, x and y, the point in the grid that is closest to the inpput
   */ 
  _snapToGrid : function(point) {
      return {x: Math.round(point.x / this.gridUnit.x) * this.gridUnit.x, y: Math.round(point.y / this.gridUnit.y) * this.gridUnit.y};
  },
  
  /**
   * Find the best position to insert one or more new neighbors for an existing node
   * 
   * @param relativePosition an object with one field, which can be either 'above', 'below', 'side', or 'join', whose value indicated the id of a node
   * @param identifiers an array of new ids for which positions must be found
   * 
   * @return an object where each field is one of the ids given as input, and the value is the point where that node should be placed
   */
  findPosition : function (relativePosition, identifiers) {
    var result = {};
    var _this = this;
    if (relativePosition.above) {
      // Finding positions for parents...
      var id = relativePosition.above;
      var node = this.nodes[id];
      var i = 0, total = identifiers.length;
      identifiers.each(function(item) {
        result[item] = { x: node.getX() + (2 * i - total + 1) * _this.gridUnit.x, y: node.getY() - _this.gridUnit.y}
        ++i;
      });
    } else if (relativePosition.below) {
      // Finding positions for children...
      var id = relativePosition.below;
      var node = this.nodes[id];
      var i = 1, total = identifiers.length;
      var crtSize = this._findLowerNeighborGroupsSize(node);
      var newSize = crtSize + total * 2 * this.gridUnit.x;
      var start = node.getX() - newSize / 2 + crtSize;
      identifiers.each(function(item) {
        result[item] = { x: start + (2 * i) * _this.gridUnit.x, y: node.getY() + _this.gridUnit.y}
        ++i;
      });
    } else if (relativePosition.join) {
       // Finding positions for partnerships...
       // expecting to join 2 nodes
       var node1 = this.nodes[relativePosition.join[0]],
           node2 = this.nodes[relativePosition.join[1]];
           // TODO
       result.y = Math.max(node1.getY(), node2.getY());
       var middleX = (node1.getX() + node2.getX()) / 2;
       var nearestNode = node1.getY() != node2.getY() ? (node1.getY() == y ? node1 : node2) : (node1.getSideNeighbors().length > node2.getSideNeighbors().length ? node2 : node1);
       result.x = nearestNode.getX() + this.gridUnit.x * (middleX < nearestNode.getX() ? -1 : 1);
    } else if (relativePosition.side) {
       // Finding positions for partners...
       var id = relativePosition.side;
       var node = this.nodes[id];
       var neighbors = node.getSideNeighbors();
       var found = false, dx = 2 * this.gridUnit.x;
       while (!found) {
        var rightNode = this.getNodeNear(node.getX() + dx, node.getY());
        if (!rightNode) {
          found = true;
        } else {
          var leftNode = this.getNodeNear(node.getX() - dx, node.getY());
          if (!leftNode) {
            dx = -dx;
            found = true;
          } else {
            if (!node.isPartnerOf(rightNode)) {
              found = true;
            } else if (!node.isPartnerOf(leftNode)) {
              dx = -dx;
              found = true;
            } else {
              dx += 2 * this.gridUnit.x;
            }
          }
        }
      }
      result.y = node.getY();
      result.x = node.getX() + dx;
    }
    return result;
  },
  /**
   * Registers a new node in the index
   * 
   * @param node the node to register
   * @param source the node that "generated" the node to add
   */
  _addNode : function(node,fullAdd) {
    /*var neighbors = node.getNeighbors();
    if (neighbors.length > 0) {
      var n = neighbors[0];
      if (n.getY() < node.getY()) {
        // adding a child;
        // shift siblings and left nodes to the left, others to the right;
      } else if (n.getY() < node.getY()) {
        // adding parents
        
      } else {
        
      }*/
    
    this.nodes[node.getID()] = node;
    if (fullAdd) {
      var position = this._snapToGrid({x: node.getX(), y: node.getY()});
      //this._clearPositionFor(node, -1);
      this._insertInDimensionIndex('x', node);
      this._insertInDimensionIndex('y', node);
      this.positionTree.insert(position);
      this.position2Node[position.x + ',' + position.y] = node;
    }
  },
  /**
   * [Internal method] inserts a node in one of the dimension indexes, xIndex or yIndex.
   *
   * @param dimension expects 'x' or 'y'
   * @param node the node to insert 
   */ 
  _insertInDimensionIndex : function (dimension, node) {
    var index = dimension + "Index";
    var accessorName = "get" + dimension.toUpperCase();
    if (typeof(this[index]) == "undefined" ||  typeof(node[accessorName]) != "function") {
      return;
    }
    var key = node[accessorName]();
    if (!this[index][key]) {
      this[index][key] = [];
    }
    this[index][key].push(node);
  },
  /**
   * [Internal method] Retrieves all the nodes on a certain line or column of the grid 
   * 
   * @param dimension expects 'x' or 'y'
   * @param value the value for the given dimension
   * @return an array of nodes
   */
  _getNodesAt : function(dimension, value) {
    var index = dimension + "Index";
    return this[index] && this[index][value] || [];
  },
  /**
   * Update the index after having performed node moves
   */
  _findNodeInTmpIndex :  function(x, y) {
    var node = this.tmpIndex.getAtPos(x, y);
    if (node && x == node.getX() && y == node.getY()) {
      return node;
    }
    node = this.getNodeNear(x, y);
    var pos = node && this.tmpIndex.get(node);
    if (!pos || pos.x == node.getX() && pos.y == node.getY()) {
      return node;
    }
    return false;
  },
  _updateTmpPositions : function() {
    var ids = this.tmpIndex.keys();
    var _this = this;
    ids.each(function(item) {
      var node = _this.getNode(item);
      var position = _this._snapToGrid(_this.tmpIndex.get(node));
      if (_this.position2Node[position.x + ',' + position.y] == node) {
        _this.positionTree.remove(position);
        delete _this.position2Node[position.x + ',' + position.y];
        _this.xIndex[node.getX()] && (_this.xIndex[node.getX()] = _this.xIndex[node.getX()].without(node));
        _this.yIndex[node.getY()] && (_this.yIndex[node.getY()] = _this.yIndex[node.getY()].without(node));
      }
      
      var position = _this._snapToGrid({x: node.getX(), y: node.getY()});
      _this.positionTree.insert(position);
      _this.position2Node[position.x + ',' + position.y] = node;
      _this._insertInDimensionIndex('x', node);
      _this._insertInDimensionIndex('y', node);
      
      _this.tmpIndex.remove(node);
    });
  },
  _nodeUpgraded : function(node, original) {
    this.remove(original);
    this._addNode(node);
  },
  /**
   * [Internal method] Updates the index after adding a new node as another node's child.
   * 
   * @param node the added node
   */
  _childAdded : function(node, _parent) {
    this.tmpIndex.clear();
    this._addNode(node, true);
    if (node.getUpperNeighbors().length > 0) {
      var parent = node.getUpperNeighbors()[0];
      var siblings = parent.getLowerNeighbors().without(node);
      var _this = this;
      var ignore = {};
      ignore[parent.getID()] = true;
      ignore[node.getID()] = true;
      siblings.each(function(item) {
        _this._subgraphShift(item, -_this.gridUnit.x, ignore);
      });
      var limits = this._findLowerNeighborGroupsLimits(node);
      this._rowShift(node, limits, this.gridUnit.x, ignore);
      this._updateTmpPositions();
    }
  },
  /**
   * [Internal method] Updates the index after adding a new node as another node's partner.
   * 
   * @param node the added node
   */
  _partnerAdded : function(node, partner) {
     this.tmpIndex.clear();
     if (node.getSideNeighbors().length > 0) {
      var side = node.getSideNeighbors()[0];
      var _this = this;
      var ignore = {};
      ignore[side.getID()] = true;
      ignore[node.getID()] = true;
      partner && (ignore[partner.getID()] = true);
      if (partner && partner.getUpperNeighbors().length > 0) {
        var parent = partner.getUpperNeighbors()[0];
        ignore[parent.getID()] = true;
      }
      if (partner && partner.getSideNeighbors().length > 0) {
        var sides = partner.getSideNeighbors();
        sides.each(function(item) {
          ignore[item.getID()] = true;
          item.getSideNeighbors().each(function(item2) {
            ignore[item2.getID()] = true;
          });
        });
        var parent = partner.getUpperNeighbors()[0];
        parent && (ignore[parent.getID()] = true);
      }
      var limits = this._findHorizontalGroupLimits(node);
      this._rowShift(node, limits, 2 * this.gridUnit.x, ignore);
      this._updateTmpPositions();
      this._addNode(node, true);
      side && this._addNode(side, true);
    }
  },
  /**
   * [Internal method] Updates the index after adding a nodes as another node's parents.
   * 
   * @param node the added partnerships node
   */
  _parentsAdded : function(node, child) {
    this.tmpIndex.clear();
    var neighbors = node.getSideNeighbors();
    if (neighbors.length > 0) {
      var _this = this;
      var ignore = {};
      neighbors.each(function (item) {
        ignore[item.getID()] = true;
      });
      ignore[node.getID()] = true;
      if (child) {
        ignore[child.getID()] = true;
        var sides = child.getSideNeighbors();
        sides.each(function(side) {
          var partners = side.getSideNeighbors().without(child);
          partners.each(function(partner) {
            var hasSiblings = false;
            var parent = partner.getUpperNeighbors().length && partner.getUpperNeighbors()[0];
            if (parent) {
              var siblings = parent.getLowerNeighbors().without(partner);
              siblings.each(function(sib) {
                ignore[sib.getID] = true;
                hasSiblings = true;
              });
            }
            if (hasSiblings) {
              ignore[partner.getID()] = true;
              ignore[side.getID()] = true;
            }
          });
        });
      }
      
      var limits = this._findHorizontalGroupLimits(node);
      limits.low -= this.gridUnit.x;
      limits.high += this.gridUnit.x;
      
      var originalIgnore = Object.clone(ignore);
      
      while (this._rowShift(node, limits, this.gridUnit.x, ignore)){
        ignore = Object.clone(originalIgnore);
      }
  
      this._updateTmpPositions();
      this._addNode(node, true);
      neighbors.each(function (item) {
        _this._addNode(item, true);
      });
    }
  },
  _partnershipAdded : function() {
    // TODO
  },
  /**
   * [Internal method] Updates the positions of all the nodes in a row when a node in that row was added or removed.
   * 
   * @param node the modified node
   * @param length the distance to shift with
   * @param ignore a map of nodes which should not be moved or traversed
   */
  _rowShift : function(node, limits, length, ignore) {
    var row = this._getNodesAt('y', node.getY());
    var _this = this;
    var mid = (limits.low + limits.high)/2;
    var shifted = false;
    row.each(function (item) {
      if (item.getX() >= limits.low && item.getX() < mid) {
        shifted = true;
        _this._subgraphShift(item, -length, ignore);
      } else if (item.getX() <= limits.high && item.getX() >= mid) {
        shifted = true;
        _this._subgraphShift(item, length, ignore);
      }
    });
    return shifted;
  },
  /**
   * [Internal method] Updates the positions of all the nodes related to a recently moved node
   * 
   * @param node the modified node
   * @param length the distance to shift with
   * @param ignore a map of nodes which should not be moved or traversed
   */
  _subgraphShift : function(node, dx, ignore) {
    if (ignore[node.getID()]) {
      return false;
    }
    ignore[node.getID()] = true;
    var newDx = dx;
    if (node.getType() == "partnership") {
      var isBlocked = true;
      var sides = node.getSideNeighbors();
      if (sides.length == 2) {
        node.getSideNeighbors().each( function(item) {
          if (!ignore[item.getID()]) {
            isBlocked = false;
          }
        });
        if (isBlocked &&  sides[0].getY() == sides[1].getY() && node.getX() != (sides[0].getY() + sides[1].getY()) / 2) {
          var min = Math.min(sides[0].getX(), sides[1].getX());
          var max = Math.max(sides[0].getX(), sides[1].getX());
          var y = sides[0].getY();

          var canMove = true;
          var found = false;
          for (var x = min + this.gridUnit.x; x < max; x += this.gridUnit.x) {
            found = this._findNodeInTmpIndex(x, y);
            if (found && found != node) {
              break;
            } else {
              found = false;
            }
          }
          if (!found) {
            var midX = min + Math.round((max - min) / (2 * this.gridUnit.x)) * this.gridUnit.x;
            newDx = midX - node.getX();
          }
        }
      }
    }
    if (newDx == 0) {
      return false;
    } else {
      dx = newDx;
    }
    this.relativeMove(node, dx, 0);
    var _this = this;
    var neighbors = node.getNeighbors();
    neighbors.each(function(item) {
      _this._subgraphShift(item, dx, ignore);
    });
    return true;
  },
  /**
   * [Internal method] Updates the vertical ordering of nodes whenever a higher node is becomes the child of a lower one.
   * 
   * @param upperNode the parent node
   * @param lowerNode the child node
   */
  _rowConsistencyFix : function(upperNode, lowerNode) {
    if (upperNode.getY() >= lowerNode.getY()) {
    // TODO
      
    }
  },
  /**
   * Remove a node from the index
   * 
   * @param node the removed node
   * @return node the removed node
   */
  remove : function (node) {
    var node = this._getActualNode(node);
    if (node) {
      var position = this._snapToGrid({x: node.getX(), y: node.getY()});
      this.positionTree.remove(position);
      delete this.position2Node[position.x + ',' + position.y];
      delete this.nodes[node.getID()];
      this.xIndex[node.getX()] && (this.xIndex[node.getX()] = this.xIndex[node.getX()].without(node));
      this.yIndex[node.getY()] && (this.yIndex[node.getY()] = this.yIndex[node.getY()].without(node));
    }
    return node;
  },
  /**
   * Move a node to a diferent position
   * 
   * @param node the node to move
   * @param x the new horizontal coordinate
   * @param y the new vertical coordinate
   * @return true if the move was successful, false otherwise
   */
  move : function(node, x, y) {
    this.tmpIndex.insert(node);
    node.setPos(x, y);
    /*var node = this.remove(node) || node;
    if (node) {
      node.setPos(x, y);
      this._addNode(node);
    }*/
    return !!node;
  },
  /**
   * Move a node to a diferent position, indicated by relative coordinates
   * 
   * @param node the node to move
   * @param dx the horizontal distance to the new position
   * @param dy the vertical distance to the new position
   * @return true if the move was successful, false otherwise
   */
  relativeMove : function(node, dx, dy) {
    this.move(node, node.getX() + dx, node.getY() + dy);
  },
  /**
   * [Internal method] Retrieves the indexed node matching the input
   * 
   * @param node the node to search in the index
   * @return the matching node from the index
   */
  _getActualNode : function(node) {
    var id;
    if (typeof(node.getID == 'function')) {
      id = node.getID();
    } else {
      id = node;
    }
    return this.nodes[id];
  },
  /**
   * Retrieves the node indexed for a given id
   * 
   * @param id
   * @return node indexed for id or undefined
   */
  getNode : function(id) {
    return this.nodes[id];
  },
   /**
   * Retrieves the node indexed for a given position
   * 
   * @param x
   * @param y
   * @return node indexed for the position or undefined
   */
  getNodeAt : function(x, y) {
    return this.position2Node[x + ',' + y];
  },
  /**
   * Retrieves the node in the grid point closest to the input coordinates
   * 
   * @param x
   * @param y
   * @return node indexed for the position or undefined
   */
  getNodeNear : function(x, y) {
    var position = this._snapToGrid({x: x, y: y});
    return this.position2Node[position.x + ',' + position.y];
  },
  /**
   * Checks if an id is registered in the index
   */
  exists : function(id) {
    return !!this.nodes[id];
  },
  
  /**
   * [Internal method] Find the leftmost and rightmost x coordinate of nodes horizontally connected to a given node
   * 
   * @param node the node for which the group is measured
   * @param _visited optional map of already visited nodes, which should be ignored
   * @param _limits optional object {high: number, low: number} containg the limits computed so far
   * @return an object {high: number, low: number} containing the computed limits
   */
  _findHorizontalGroupLimits : function (node, _visited, _limits) {
    var visited = _visited || {};
    if (!node || visited[node.getID()]) {
      return _limits;
    }
    var limits = {};
    if (_limits)  {
      limits.low  = Math.min(_limits.low,  node.getX());
      limits.high = Math.max(_limits.high, node.getX());
    } else {
      limits = _limits || {low : node.getX(), high : node.getX()}
    }
    visited[node.getID()] = true;
    var _this = this;
    node.getSideNeighbors().each(function (item) {
      if (item.getY() == node.getY() && !visited[item.getID]) {
        limits = _this._findHorizontalGroupLimits(item, visited, limits);
      }
    });
    return limits;
  },
  
  /**
   * [Internal method] Find the distance between leftmost and rightmost x coordinate of nodes horizontally connected to a given node
   * 
   * @param node the node for which the group is measured
   * @return a number representing the distance between leftmost and rightmost x coordinate of the group
   */
  _findHorizontalGroupSize :  function (node) {
    var limits = this._findHorizontalGroupLimits(node);
    return limits.high - limits.low;
  },
   /**
   * [Internal method] Find the leftmost and rightmost x coordinate of the lower neighbors of a node
   * 
   * @param node the node for which the group is measured
   * @return an object {high: number, low: number} containing the computed limits
   */
  _findLowerNeighborGroupsLimits : function(node) {
    var list = node.getLowerNeighbors();
    if (list.length == 0) {
      return {low: node.getX(), high: node.getX()};
    }
    var start =  this._findHorizontalGroupLimits(list[0]).low;
    var end = this._findHorizontalGroupLimits(list[list.length - 1]).high;
    return {low: start, high: end};
  },
  
  /**
   * [Internal method] Find the distance between leftmost and rightmost x coordinate of the lower neighbors of a node
   * 
   * @param node the node for which the group is measured
   * @return a number representing the distance between leftmost and rightmost x coordinate of the group
   */
  _findLowerNeighborGroupsSize : function(node) {
    var list = node.getLowerNeighbors();
    if (list.length == 0) {
      return -2*this.gridUnit.x;
    }
    var start =  list[0].getX();
    var end = this._findHorizontalGroupLimits(list[list.length - 1]).high;
    return end - start;
  }
});