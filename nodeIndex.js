var NodeIndex = Class.create({
  gridUnit : {
    x: 150,
    y: 300
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
      var id = relativePosition.above;
      var node = this.nodes[id];
      var i = 0, total = identifiers.length;
      identifiers.each(function(item) {
        result[item] = { x: node.getX() + (i - (total - 1) / 2) * _this.gridUnit.x, y: node.getY() - _this.gridUnit.y}
	++i;
      });
    } else if (relativePosition.below) {
      var id = relativePosition.below;
      var node = this.nodes[id];
      var i = 0, total = identifiers.length;
      identifiers.each(function(item) {
        result[item] = { x: node.getX() + (2 * i - total + 1) * _this.gridUnit.x, y: node.getY() + _this.gridUnit.y}
	++i;
      });
    } else {
       var id = relativePosition.above;
       var node = this.nodes[id];
       var neighbors = node.getSideNeighbors();
       //neighbors.each(
       //if (
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