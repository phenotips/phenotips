var NodeIndex = Class.create({
  gridUnit : {
    x: 100,
    y: 250
  },
  initialize : function(nodes) {
    var _this = this;
    this.nodes = {};
    this.positionTree = new kdTree([]);
    this.position2Node = {};
    this.add = this.add.bind(this);
    nodes && nodes.each(this.add);
  },

  __snapToGrid : function(point) {
      return {x: Math.round(point.x / this.gridUnit.x) * this.gridUnit.x, y: Math.round(point.y / this.gridUnit.y) * this.gridUnit.y};
  },
  
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
      var i = node.getLowerNeighbors().length, total = i + identifiers.length;
      identifiers.each(function(item) {
        result[item] = { x: node.getX() + (2 * i - total + 1) * _this.gridUnit.x, y: node.getY() + _this.gridUnit.y}
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
       var side = 2 * (neighbors.length % 2) - 1;
       result.y = node.getY();
       result.x = node.getX() + 2 * this.gridUnit.x * side;
    }
    return result;
  },
  add : function(node) {
    var position = this.__snapToGrid({x: node.getX(), y: node.getY()});
    //if (this.getNodeAt(position.x, position.y)) {
      // TODO shift node to the side
    //}
    //node.moveTo(position.x, position.y);
    this.nodes[node.getID()] = node;
    this.positionTree.insert(position);
    this.position2Node[position.x + ',' + position.y] = node;
  },
  remove : function (node) {
    var node = this.__getActualNode(node);
    if (node) {
      var position = {x: node.getX(), y: node.getY()};
      this.positionTree.remove(position);
      delete this.position2Node[position.x + ',' + position.y];
      delete this.nodes[id];
    }
    if (positionTree.nearest(position, 0, 0, 'y').size() == 0) {
      // TODO shift neighbors
    }
    return node;
  },
  move : function(node, x, y) {
    var node = this.remove(node) || node;
    if (node) {
      node.moveTo(x, y);
      this.add(node);
    }
    return !!node;
  },
  relativeMove : function(node, dx, dy) {
    var node = this.remove(node) || node;
    if (node) {
      node.moveTo(node.getX() + dx, node.getY() + y);
      this.add(node);
    }
    return !!node;
  },
  __getActualNode : function(node) {
    var id;
    if (typeof(node.getID == 'function')) {
      id = node.getID();
    } else {
      id = node;
    }
    return this.nodes[id];
  },
  getNode : function(id) {
    return this.nodes[id];
  },
  getNodeAt : function(x, y) {
    return this.position2Node[x + ',' + y];
  },
  getNodeNear : function(x, y) {
    var position = this.__snapToGrid({x: x, y: y});
    return this.position2Node[position.x + ',' + position.y];
  },
  exists : function(id) {
    return !!this.nodes[id];
  }
});