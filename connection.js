var Connection = Class.create( {
    initialize: function(type, node1, node2) {
        this.node1 = node1;
        this.node2 = node2;
        return this.generatePartnerConnection();

    },

    generatePartnerConnection: function() {
        var xDistance = (this.node1._xPos - this.node2._xPos).abs();
        var yDistance = (this.node1._yPos - this.node2._yPos).abs();
        var wirePath = ["M", this.node1._xPos, this.node1._yPos, "L", this.node2._xPos, this.node2._yPos];
        var iconPath = "M25.979,12.896 19.312,12.896 19.312,6.229 12.647,6.229 12.647,12.896 5.979,12.896 5.979,19.562 12.647,19.562 12.647,26.229 19.312,26.229 19.312,19.562 25.979,19.562z";
        var iconX =  this.node1._xPos + xDistance/2;
        var iconY =  this.node1._yPos + yDistance/2 - 10;

//        var path = [["M", this.node1._xPos, this.node1._yPos],
//                    ["L", this.node1._xPos + xDistance/2, this.node1._yPos + yDistance/2],
//                   [["L", this.node1._xPos + xDistance, this.node1._yPos + yDistance]]];

        var wire =  editor.paper.path(wirePath);
        wire.path = wirePath;
        var icon = editor.paper.path(iconPath).attr({fill: "#000", stroke: "none"}).translate();
        return editor.paper.set(wire, icon);
    },

    getPartner: function(node) {
        if(node === this.node1)
        {
            return this.node2;
        }
        else if(node === this.node2)
        {
            return this.node1;
        }
    }
});