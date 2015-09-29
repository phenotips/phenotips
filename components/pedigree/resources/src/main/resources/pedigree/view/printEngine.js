/**
 * PrintEngine handles splitting of pedigree into pages for printing and generating print previews
 *
 * @class PrintEngine
 */
var PrintEngine = Class.create({

    initialize: function() {
        // TODO: load default paper settings from pedigree preferences in admin section
        this.printPageWidth  = 1055;
        this.printPageHeight = 756;
        this.xOverlap = 15;
        this.yOverlap = 15;
    },

    /**
     * @param {emulateFullPage} When true, makes svg include extra blank space up to the pageWidth/pageHeight size
     */
    _generatePages: function(scale, addOverlaps, pageWidth, pageHeight, includeLegend, emulateFullPage) {
        var totalLegendHeight = 0;
        var legendHTML = "";
        if (includeLegend) {
            // Manually compose legend for print; 3 benefits:
            //   - color samples not using background gcolor (which is not printed by default in some browsers)
            //   - known height (need th eheight to compute required number of pages; no need to pre-render in the browser)
            //   - better handling of disabled/enabled legend items (regular legend has both enabled and disabled items)

            var legendData = editor.getView().getSettings();

            var generateSection = function(colors, names, sectionName) {
                var count  = 0;
                var height = 30;
                var html   = "<div class='legend-section'><h3 class='section-title'>" + sectionName + "</h3>"+
                             "<ul class='abnormality-list'>";
                for (var id in colors) {
                    if (colors.hasOwnProperty(id)) {
                        count++;
                        var color = colors[id];
                        var name  = (names && names.hasOwnProperty(id)) ? names[id] : id;
                        html += "<li class='abnormality'>";
                        html += "<span class='abnormality-color'><svg height='13' width='13' style='display: inline-block;'><rect x='0' y='0' rx='6' ry='6' width='13' height='13' fill='" + color + "'></rect></svg></span>";
                        html += "<span class='legend-item-name'>" + name + "</span>";
                        height += 20;
                    }
                }
                html += "</ul></div>";
                if (count > 0) {
                    return {"html": html, "heightInPixels": height};
                } else {
                    return {"html": "", "heightInPixels": 0};
                }
            }
            var sections = [];
            if (legendData.hasOwnProperty("colors")) {
                legendData["colors"].hasOwnProperty("disorders")  && sections.push(generateSection(legendData["colors"]["disorders"], legendData["names"]["disorders"], "Disorders"));
                legendData["colors"].hasOwnProperty("genes")      && sections.push(generateSection(legendData["colors"]["genes"], null, "Genes"));
                legendData["colors"].hasOwnProperty("phenotypes") && sections.push(generateSection(legendData["colors"]["phenotypes"], null, "Phenotypes"));
                legendData["colors"].hasOwnProperty("cancers")    && sections.push(generateSection(legendData["colors"]["cancers"], null, "Cancers"));
            }

            for (var i = 0; i < sections.length; i++) {
                totalLegendHeight += sections[i].heightInPixels;
                legendHTML        += sections[i].html;
            }
        } // if includeLegend

        var svg = editor.getWorkspace().getSVGCopy();

        svg.scale(scale);

        var svgText = svg.getSVGText();
        var bbox    = svg.getBBox();

        var xOverlap = addOverlaps ? Math.floor(this.xOverlap * scale) : 0;
        var yOverlap = addOverlaps ? Math.floor(this.yOverlap * scale) : 0;

        var pagesWide = (bbox.width  < pageWidth)  ? 1 : Math.ceil(bbox.width  / pageWidth);
        var pagesTall = (bbox.height < pageHeight) ? 1 : Math.ceil(bbox.height / pageHeight);
        if (pagesWide > 1 && addOverlaps) {
            var realWidthWithOverlaps = bbox.width + (pagesWide-1)*xOverlap;
            pagesWide = Math.ceil(realWidthWithOverlaps / pageWidth);
        }
        if (pagesTall > 1 && addOverlaps) {
            var realHeightWithOverlaps = bbox.height + (pagesTall-1)*yOverlap;
            pagesTall = Math.ceil(realHeightWithOverlaps / pageHeight);
        }
        // split into pages
        var pages = [];
        var pageStartY = 0;
        var legendOnSeparatePage = false;
        for (var pageNumY = 0; pageNumY < pagesTall; pageNumY++) {
            var rowHeight = emulateFullPage ? pageHeight : Math.min(pageHeight, bbox.height - pageStartY);
            var pagesRow = [];
            var pageStartX = 0;
            for (var pageNumX = 0; pageNumX < pagesWide; pageNumX++) {
                var columnWidth = emulateFullPage ? pageWidth : Math.min(pageWidth,  bbox.width - pageStartX);
                var startX = pageStartX;
                if (emulateFullPage && pagesWide == 1) {
                    startX = pageStartX - (pageWidth - bbox.width)/2;
                }
                var pageSvg = svg.getCopy().setViewBox(startX, pageStartY, columnWidth, rowHeight).getSVGText();
                var page = { "pageName" : "page " + pageNumX + ":" + pageNumY,
                             "svg": pageSvg, "width": columnWidth };
                pagesRow.push(page);
                pageStartX += (columnWidth - xOverlap);
            }
            pages.push(pagesRow);
            if (pageNumY == (pagesTall-1) && pageHeight < rowHeight + totalLegendHeight) {
                legendOnSeparatePage = true;
            }
            pageStartY += (rowHeight - yOverlap);
        }

        return { "pages": pages,
                 "pagesWide": pageNumX,
                 "pagesTall": pageNumY,
                 "needLegendOnSeparatePage": legendOnSeparatePage,
                 "legendHTML": legendHTML,
                 "legendHeight": totalLegendHeight }
    },

    generatePreviewHTML: function(maxPreviewWidth, maxPreviewHeight, printScale, addOverlaps) {
        var previewWidth = maxPreviewWidth - 30;

        // generate pages for print, and based on the number of pages used re-generate preview pages
        var pages = this._generatePages(printScale,
                addOverlaps,
                this.printPageWidth,
                this.printPageHeight,
                false,
                false);

        var printWidth = 0;
        for (var pageNumX = 0; pageNumX < pages.pagesWide; pageNumX++) {
            printWidth += pages.pages[0][pageNumX].width; // this includes overlaps, if any
        }
        var expectedWidth = pages.pagesWide * this.printPageWidth;

        // need to scale even more than for print, the ratio is the ratio of printWidth to previewWidth
        var scaleComparedToPrint = previewWidth / expectedWidth;
        var useScale = scaleComparedToPrint * printScale;

        var pages = this._generatePages(useScale,
                                        addOverlaps,
                                        this.printPageWidth*scaleComparedToPrint,
                                        this.printPageHeight*scaleComparedToPrint,
                                        false,
                                        true);
        var html = "<div class='printPreview' style='height: " + maxPreviewHeight + "px; width: " + maxPreviewWidth + "px; overflow-y: scroll;'>";
        for (var pageNumY = 0; pageNumY < pages.pagesTall; pageNumY++) {
            for (var pageNumX = 0; pageNumX < pages.pagesWide; pageNumX++) {
                var page = pages.pages[pageNumY][pageNumX];
                html += "<div class='previewPage' style='border: 1px; border-style: dotted; float: left;' id='pedigree-page-x" + pageNumX + "-y" + pageNumY + "'>" + page.svg + "</div>";
            }
            html += "<br>";
        }
        html += "</div>";
        return html;
    },

    print: function(printScale, addOverlaps, includeLegend, closeAfterPrint, printPageSet) {
        var pages = this._generatePages(printScale,
                                        addOverlaps,
                                        this.printPageWidth,
                                        this.printPageHeight,
                                        includeLegend,
                                        false);
        var w=window.open();
        //w.document.write("<link rel='stylesheet' type='text/css' href='print.css' />");
        w.document.write("<style type='text/css' media='print'>@page { size: landscape; }</style>");
        w.document.write("<style type='text/css'>"+
            "* {margin: 0;}" +
            "html, body { height: 100%; }" +
            ".wrapper { min-height: 100%; height: auto !important; height: 100%; }" +
            ".break_here { page-break-before:always; }" +
            ".abnormality-list { list-style-type: none; }" +
            ".abnormality-color { margin-right: 8px; }" +
            ".legend-item-name {font-size: 11pt }" +
            "</style>");

        for (var pageNumY = 0; pageNumY < pages.pagesTall; pageNumY++) {
            for (var pageNumX = 0; pageNumX < pages.pagesWide; pageNumX++) {
                if (printPageSet && !printPageSet["x" + pageNumX + "y" + pageNumY]) {
                    // skip pages marked ot be skipped by the user
                    continue;
                }
                var page = pages.pages[pageNumY][pageNumX];
                var bottomLeftPage = (pageNumY == (pages.pagesTall - 1)) && (pageNumX == 0);
                var spaceForLegend = includeLegend && bottomLeftPage && !pages.needLegendOnSeparatePage;
                if (spaceForLegend) {
                    w.document.write("<div class='wrapper' style='margin: 0 auto -" + pages.legendHeight + "px;'>");
                }
                if (pages.pagesWide == 1) {
                    w.document.write("<center>");
                }
                w.document.write("<div id='pedigree-page-x" + pageNumX + "-" + pageNumY + "'>" + page.svg + "</div>");
                if (pages.pagesWide == 1) {
                    w.document.write("</center>");
                }
                if (spaceForLegend) {
                    w.document.write("<div style='height: " + pages.legendHeight + "px;'></div></div>");
                    w.document.write("<div id='legend' class='footer print-legend' style='height: " + pages.legendHeight + "px;'>" + pages.legendHTML + "</div>");
                } else {
                    w.document.write("<div class='break_here'></div>");
                }
            }
        }

        if (includeLegend && pages.needLegendOnSeparatePage) {
            w.document.write("<div class='break_here'></div>");
            w.document.write("<div id='legend' class='print-legend'>" + pages.legendHTML + "</div>");
        }
        w.document.close(); // to prevent infinite "page loading"

        w.print();
        if (closeAfterPrint) {
            w.close();
        }
    }
});
