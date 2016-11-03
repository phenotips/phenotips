//attributes for graphical elements in the editor
define([], function(){
    var PedigreeEditorParameters = {};

    PedigreeEditorParameters.styles = {
        "blackAndWhite": {
            nodeShapeFemale: {fill: "#ffffff", stroke: "#222222"},
            nodeShapeMale:   {fill: "#ffffff", stroke: "#111111"},
            nodeShapeOther:  {fill: "#ffffff", stroke: "#222222"},
            nodeShapeDiag:   {fill: "#ffffff", stroke: "#222222"},
            nodeShapeAborted:{fill: "#ffffff", stroke: "#222222"}
        }
    };

    PedigreeEditorParameters.lineStyles = {
        "thinLines": {
            partnershipLines :         {"stroke-width": 1.25, stroke : '#303058'},
            partnershipLinesAdoptedIn: {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "--"},
            consangrPartnershipLines : {"stroke-width": 1.25, stroke : '#402058'},
            noContactLines:            {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "-.-."},
            noContactAdoptedIn:        {"stroke-width": 1.25, stroke : '#303058', "stroke-dasharray": "-- ."},
            noContactLinesConsangr:    {"stroke-width": 1.25, stroke : '#402058', "stroke-dasharray": "-.-."},
            childlessShapeAttr:            {"stroke-width": 2.5, stroke: "#3C3C3C"},
            partnershipChildlessShapeAttr: {"stroke-width": 2.0, stroke: "#3C3C3C"},
        },
        "regularLines": {
            partnershipLines :         {"stroke-width": 2.0, stroke : '#303058'},
            partnershipLinesAdoptedIn: {"stroke-width": 2.0, stroke : '#303058', "stroke-dasharray": "--"},
            consangrPartnershipLines : {"stroke-width": 2.0, stroke : '#402058'},
            noContactLines:            {"stroke-width": 2.0, stroke : '#303058', "stroke-dasharray": "-.-."},
            noContactAdoptedIn:        {"stroke-width": 2.0, stroke : '#303058', "stroke-dasharray": "-- ."},
            noContactLinesConsangr:    {"stroke-width": 2.0, stroke : '#402058', "stroke-dasharray": "-.-."},
            childlessShapeAttr:            {"stroke-width": 2.6, stroke: "#3C3C3C"},
            partnershipChildlessShapeAttr: {"stroke-width": 2.25, stroke: "#3C3C3C"},
        },
        "boldLines": {
            partnershipLines :         {"stroke-width": 2.25, stroke : '#101028'},
            partnershipLinesAdoptedIn: {"stroke-width": 2.25, stroke : '#101028', "stroke-dasharray": "--"},
            consangrPartnershipLines : {"stroke-width": 2.25, stroke : '#200028'},
            noContactLines:            {"stroke-width": 2.25, stroke : '#101028', "stroke-dasharray": "-.-."},
            noContactAdoptedIn:        {"stroke-width": 2.25, stroke : '#101028', "stroke-dasharray": "-- ."},
            noContactLinesConsangr:    {"stroke-width": 2.25, stroke : '#200028', "stroke-dasharray": "-.-."},
            childlessShapeAttr:            {"stroke-width": 2.6, stroke: "#2C2C2C"},
            partnershipChildlessShapeAttr: {"stroke-width": 2.25, stroke: "#2C2C2C"},
        }
    };

    PedigreeEditorParameters.attributes = {
        radius: 40,
        orbRadius: 6,
        touchOrbRadius: 8,
        personHoverBoxRadius: 90,  // 80    for old handles, 90 for new
        newHandles: true,          // false for old handles
        personHandleLength: 75,    // 60    for old handles, 75 for new
        personHandleBreakX: 55,
        personHandleBreakY: 53,
        personSiblingHandleLengthX: 65,
        personSiblingHandleLengthY: 30,
        enableHandleHintImages: true,
        handleStrokeWidth: 5,
        groupNodesScale: 0.85,
        infertileMarkerHeight: 4,
        infertileMarkerWidth: 14,
        twinCommonVerticalLength: 6,
        twinMonozygothicLineShiftY: 24,
        curvedLinesCornerRadius: 25,
        unbornShape: {'font-size': 50, 'font-family': 'Cambria'},
        carrierShape: {fill : '#595959'},
        carrierDotRadius: 8,
        presymptomaticShape: {fill : '#777777', "stroke": "#777777"},
        presymptomaticShapeWidth: 8,
        uncertainShape:      {'font-size': '45px', 'font-family': 'Arial', 'fill': '#696969', 'font-weight': 'bold'},
        uncertainSmallShape: {'font-size': '30px', 'font-family': 'Arial', 'fill': '#696969', 'font-weight': 'bold'},
        evaluationShape: {'font-size': 40, 'font-family': 'Arial'},
        nodeShapeFemale: PedigreeEditorParameters.styles.blackAndWhite.nodeShapeFemale,
        nodeShapeMale:   PedigreeEditorParameters.styles.blackAndWhite.nodeShapeMale,
        nodeShapeOther:  PedigreeEditorParameters.styles.blackAndWhite.nodeShapeOther,
        nodeShapeDiag:   PedigreeEditorParameters.styles.blackAndWhite.nodeShapeDiag,
        nodeShapeAborted:PedigreeEditorParameters.styles.blackAndWhite.nodeShapeAborted,
        nodeShapeMenuOn:         {fill: "#000", stroke: "none", "fill-opacity": 0.1},
        nodeShapeMenuOff:        {fill: "#000", stroke: "none", "fill-opacity": 0},
        nodeShapeMenuOnPartner:  {fill: "#000", stroke: "none", "fill-opacity": 0.1},
        nodeShapeMenuOffPartner: {fill: "#000", stroke: "none", "fill-opacity": 0},
        boxOnHover : {fill: "gray", stroke: "none", opacity: 1, "fill-opacity":.35},
        menuBtnIcon : {fill: "#1F1F1F", stroke: "none"},
        deleteBtnIcon : {fill: "#990000", stroke: "none"},
        btnMaskHoverOn : {opacity:.6, stroke: 'none'},
        btnMaskHoverOff : {opacity:0},
        btnMaskClick: {opacity:1},
        orbHue : .53,
        phShape: {fill: "white","fill-opacity": 0, "stroke": 'black', "stroke-dasharray": "- "},
        dragMeLabel: {'font-size': 14, 'font-family': 'Tahoma'},
        probandArrowLabel: {'font-size': 17, 'font-weight': 'bold', 'font-family': 'Arial', 'fill': '#595959'},
        pedNumberLabel: {'font-size': 19, 'font-family': 'Serif'},
        descendantGroupLabel: {'font-size': 21, 'font-family': 'Tahoma'},
        label: {'font-size': 20, 'font-family': 'Arial'},
        nameLabels: {'font-size': 20, 'font-family': 'Arial'},
        commentLabel: {'font-size': 19, 'font-family': 'Arial' },
        cancerAgeOfOnsetLabels: {'font-size': 19, 'font-family': 'Arial' },
        externalIDLabels: {'font-size': 18, 'font-family': 'Arial' },
        disorderShapes: {},
        partnershipNode: {fill: '#d79185', stroke: 'black', 'stroke-width':2},  //#dc7868, #E25740
        partnershipRadius: 7.0,
        partnershipHandleBreakY: 18,
        partnershipHandleLength: 36,
        partnershipLines :         PedigreeEditorParameters.lineStyles.thinLines.partnershipLines,
        partnershipLinesAdoptedIn: PedigreeEditorParameters.lineStyles.thinLines.partnershipLinesAdoptedIn,
        consangrPartnershipLines : PedigreeEditorParameters.lineStyles.thinLines.consangrPartnershipLines,
        noContactLines:            PedigreeEditorParameters.lineStyles.thinLines.noContactLines,
        noContactAdoptedIn:        PedigreeEditorParameters.lineStyles.thinLines.noContactAdoptedIn,
        noContactLinesConsangr:    PedigreeEditorParameters.lineStyles.thinLines.noContactLinesConsangr,
        childlessShapeAttr:            PedigreeEditorParameters.lineStyles.thinLines.childlessShapeAttr,
        partnershipChildlessShapeAttr: PedigreeEditorParameters.lineStyles.thinLines.partnershipChildlessShapeAttr,
        childlessLength: 14,
        parnershipChildlessLength: 27,
        graphToCanvasScale: 12,
        layoutRelativePersonWidth: 10,
        layoutRelativeOtherWidth: 2,
        layoutScale: { xscale: 12.0, yscale: 8 },
        maxPrintPreviewPaneHeight: 600,
        minPrintPreviewPaneHeight: 250,
        howerboxHighlightColor: "green",
        mainGlowColor: "green",
        secondaryGlowColor: "#556B2F"
    };

    return PedigreeEditorParameters;
});
