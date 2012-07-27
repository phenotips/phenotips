var PartnerConnection = Class.create( {
    initialize: function(node1, node2) {
        this.node1 = node1;
        this.node2 = node2;
//        var xDistance = (this.getNode1().getGraphics().getAbsX() - this.getNode2().getGraphics().getAbsX()).abs();
//        var yDistance = (this.getNode1().getGraphics().getAbsY() - this.getNode2().getGraphics().getAbsY()).abs();
//        var iconPath = Raphael.pathToRelative("M16,1.466C7" +
//            ".973,1.466,1.466,7.973,1.466,16c0,8.027,6.507,14.534,14.534,14.534c8.027,0,14.534-6.507,14.534-14.534C30.534,7.973,24.027,1.466,16,1.466zM16,28.792c-1.549,0-2.806-1.256-2.806-2.806s1.256-2.806,2.806-2.806c1.55,0,2.806,1.256,2.806,2.806S17.55,28.792,16,28.792zM16,21.087l-7.858-6.562h3.469V5.747h8.779v8.778h3.468L16,21.087z");
//        var circleX =  this.getNode1().getGraphics().getAbsX() - xDistance/2;
//        var circleY =  this.getNode1().getGraphics().getAbsY() - yDistance/2;
//        this.connect1Path = ["M", this.node1.getGraphics().getAbsX(), this.node1.getGraphics().getAbsY(), "L", circleX, circleY];
//        this.connect2Path = ["M", this.node2.getGraphics().getAbsX(), this.node2.getGraphics().getAbsY(), "L", circleX, circleY];
//        this.connect1 = editor.paper.path(this.connect1Path);
//        this.connect2 = editor.paper.path(this.connect2Path);
//        iconPath[0][1] = circleX;
//        iconPath[0][2] = circleY + 100;
//
//        var lineToIconPath = ["M", circleX, circleY, "L", circleX,circleY+100];
//        var lineToIcon = editor.paper.path(lineToIconPath);
//        var circle = editor.paper.circle(circleX, circleY, 3).attr("fill", "black");
//        this.icon = editor.paper.path(iconPath).attr({fill: 'gray', stroke: "none"});
//        this.connection = editor.paper.set(this.connect1, this.connect2, this,lineToIcon, circle, this.icon);
//        this.connection.toBack();
        var x1 = node1.getGraphics().getAbsX();
        var y1 = node1.getGraphics().getAbsY();
        var x2 = node2.getGraphics().getAbsX();
        var y2 = node2.getGraphics().getAbsY();
        editor.paper.path(["M", x1, y1, "L", x2, y2]).toBack();
    },

    getNode1: function() {
        return this.node1;
    },

    getNode2: function() {
        return this.node2;
    },

    getPartnerOf: function(node) {
        if(this.getNode1() == node) {
            return this.getNode2();
        }
        else if(this.getNode2() == node) {
            return this.getNode1();
        }
        else {
            return null;
        }
    }


});



