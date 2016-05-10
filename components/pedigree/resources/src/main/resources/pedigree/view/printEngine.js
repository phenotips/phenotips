/**
 * PrintEngine handles splitting of pedigree into pages for printing and generating print previews
 *
 * @class PrintEngine
 */
define([
        "pedigree/pedigreeDate",
    ], function(
        PedigreeDate
    ){
    var PrintEngine = Class.create({

        initialize: function() {
            // TODO: load default paper settings from pedigree preferences in admin section
            this.printPageWidth  = 1055;
            this.printPageHeight = 756;
            this.printPageWidthPortrait  = 756;
            this.printPageHeightPortrait = 980;
            this.xOverlap = 32;
            this.yOverlap = 32;
        },

        /**
         * @param {emulateFullPage} When true, makes svg include extra blank space up to the pageWidth/pageHeight size
         */
        _generatePages: function(scale, moveHorizontallySize,
                                 pageWidth, pageHeight,
                                 options, emulateFullPage, scaleComparedToPrint) {
            var totalLegendHeight = 0;
            var patientInfoHeight = 0;
            var legendHTML      = "";
            var patientInfoHTML = "";
            if (options.includeLegend) {
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
                    legendData["colors"].hasOwnProperty("candidateGenes") && sections.push(generateSection(legendData["colors"]["candidateGenes"], null, "Candidate genes"));
                    legendData["colors"].hasOwnProperty("causalGenes") && sections.push(generateSection(legendData["colors"]["causalGenes"], null, "Causal genes"));
                    legendData["colors"].hasOwnProperty("phenotypes") && sections.push(generateSection(legendData["colors"]["phenotypes"], null, "Phenotypes"));
                    legendData["colors"].hasOwnProperty("cancers")    && sections.push(generateSection(legendData["colors"]["cancers"], null, "Cancers"));
                }

                for (var i = 0; i < sections.length; i++) {
                    totalLegendHeight += sections[i].heightInPixels;
                    legendHTML        += sections[i].html;
                }
            } // if includeLegend
            if (options.includePatientInfo) {
                patientInfoHeight = 30;
                var proband = editor.getNode(0);
                if (options.anonymize.removePII || (!proband.getFirstName() && !proband.getLastName())) {
                    if (editor.getUrlQueryPatientID()) {
                        patientInfoHTML = "Patient " + editor.getUrlQueryPatientID();
                    } else {
                        patientInfoHTML = "Family " + XWiki.currentDocument.page;
                    }
                } else {
                    // TODO: update to correct proband/family when fmaly studies are merged in
                    var space = (proband.getFirstName() && proband.getLastName()) ? " " : "";
                    var probandName = proband.getFirstName() + space + proband.getLastName();
                    patientInfoHTML = probandName + ", " + XWiki.currentDocument.page;
                }
                var userFirstName = editor.getPreferencesManager().getConfigurationOption("firstName");
                var userLastName  = editor.getPreferencesManager().getConfigurationOption("lastName");
                var date = new PedigreeDate(new Date());
                patientInfoHTML += ". Printed";
                if (userFirstName || userLastName) {
                    patientInfoHTML += " by " + userFirstName + " " + userLastName;
                }
                var dateDisplayFormat = editor.getPreferencesManager().getConfigurationOption("dateDisplayFormat");
                if (dateDisplayFormat == "DMY" || dateDisplayFormat == "MY" || dateDisplayFormat == "Y") {
                    patientInfoHTML += " on " + date.getBestPrecisionStringDDMMYYY() + ".";
                } else {
                    patientInfoHTML += " on " + date.getMonthName() + " " + date.getDay() + ", " + date.getYear() + ".";
                }
            }

            var svg = editor.getWorkspace().getSVGCopy(options.anonymize);

            //console.log("BBOX: " + stringifyObject(svg.getBBox()));

            svg.scale(scale);

            svg.move(moveHorizontallySize, 0);

            var svgText = svg.getSVGText();
            var bbox    = svg.getBBox();

            var xOverlap = options.addOverlaps ? Math.floor(this.xOverlap * scale) : 0;
            var yOverlap = options.addOverlaps ? Math.floor(this.yOverlap * scale) : 0;

            var pagesWide = (bbox.width <= pageWidth) ? 1 : Math.ceil(bbox.width / pageWidth);
            var pagesTall = (bbox.height + patientInfoHeight*scaleComparedToPrint <= pageHeight) ? 1 : Math.ceil((bbox.height + patientInfoHeight*scaleComparedToPrint) / pageHeight);
            if (pagesWide > 1 && options.addOverlaps) {
                var realWidthWithOverlaps = bbox.width + (pagesWide-1)*xOverlap;
                pagesWide = Math.ceil(realWidthWithOverlaps / pageWidth);
            }
            if (pagesTall > 1 && options.addOverlaps) {
                var realHeightWithOverlaps = bbox.height + (pagesTall-1)*yOverlap;
                pagesTall = Math.ceil(realHeightWithOverlaps / pageHeight);
            }

            // split into pages
            var pages = [];
            var pageStartY = 0;
            var legendOnSeparatePage = false;
            for (var pageNumY = 0; pageNumY < pagesTall; pageNumY++) {
                var rowHeight = Math.min(pageHeight, bbox.height - pageStartY);
                // to account for rounding & avoid priting pages with just 1 pixel of data
                if (!emulateFullPage && rowHeight <= 2) {
                    pagesTall = pagesTall - 1;
                    legendOnSeparatePage = true;
                    continue;
                }
                if (emulateFullPage) {
                    var rowHeight = pageHeight;
                }
                var pagesRow = [];
                var pageStartX = 0;
                if (pageNumY == 0 && options.includePatientInfo) {
                    if (rowHeight + patientInfoHeight*scaleComparedToPrint > pageHeight) {
                        rowHeight = pageHeight - patientInfoHeight*scaleComparedToPrint;
                    }
                }
                for (var pageNumX = 0; pageNumX < pagesWide; pageNumX++) {
                    var columnWidth = pageWidth; //(emulateFullPage || (pagesWide > 1 && pageNumX == pagesWide-1))? pageWidth : Math.min(pageWidth,  bbox.width - pageStartX);
                    var startX = pageStartX;
                    if (pagesWide == 1) {
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
                     "pagesWide": pagesWide,
                     "pagesTall": pagesTall,
                     "needLegendOnSeparatePage": legendOnSeparatePage,
                     "legendHTML": legendHTML,
                     "legendHeight": totalLegendHeight,
                     "patientInfoHTML": patientInfoHTML,
                     "patientInfoHeight": patientInfoHeight}
        },

        generatePreviewHTML: function(landscape, maxPreviewWidth, maxPreviewHeight, printScale, moveHorizontallySize, options) {
            var previewWidth = maxPreviewWidth - 30;

            var printedWidth  = landscape ? this.printPageWidth : this.printPageWidthPortrait;
            var printedHeight = landscape ? this.printPageHeight : this.printPageHeightPortrait;

            // generate pages for print, and based on the number of pages used re-generate preview pages
            var pagesReal = this._generatePages(printScale,
                    moveHorizontallySize,
                    printedWidth,
                    printedHeight,
                    options,
                    false,
                    1);

            var printWidth = 0;
            for (var pageNumX = 0; pageNumX < pagesReal.pagesWide; pageNumX++) {
                printWidth += pagesReal.pages[0][pageNumX].width; // this includes overlaps, if any
            }
            var expectedWidth = pagesReal.pagesWide * printedWidth;

            // need to scale even more than for print, the ratio is the ratio of printWidth to previewWidth
            var scaleComparedToPrint = previewWidth / expectedWidth;
            var useScale = scaleComparedToPrint * printScale;

            var pages = this._generatePages(useScale,
                                            moveHorizontallySize,
                                            scaleComparedToPrint * printedWidth,
                                            scaleComparedToPrint * printedHeight,
                                            options,
                                            true,
                                            scaleComparedToPrint);
            if (pages.pagesTall > pagesReal.pagesTall) {
                pages.pagesTall = pagesReal.pagesTall;  // may hapen due to rounding errors
            }
            var html = "<div class='printPreview' id='printPreview' style='height: " + maxPreviewHeight + "px; width: " + maxPreviewWidth + "px; overflow-y: scroll;'>";
            for (var pageNumY = 0; pageNumY < pages.pagesTall; pageNumY++) {
                for (var pageNumX = 0; pageNumX < pages.pagesWide; pageNumX++) {
                    var page = pages.pages[pageNumY][pageNumX];
                    html += "<div class='previewPage' style='border: 1px; border-style: dotted; float: left;' id='pedigree-page-x" + pageNumX + "-y" + pageNumY + "'>";
                    if(pageNumY == 0 && options.includePatientInfo) {
                        var content = (pageNumX == 0) ? pages.patientInfoHTML : "";
                        html += "<div style='height: " + pages.patientInfoHeight*scaleComparedToPrint  + "px; font-size: " + (11*scaleComparedToPrint) + "pt; text-align: left'>" + content + "</div>";
                    }
                    html += page.svg;
                    html += "</div>";
                }
                //html += "<br>";
            }
            html += "</div>";
            return html;
        },

        print: function(landscape, printScale, moveHorizontallySize, options, printPageSet) {
            var pages = this._generatePages(printScale,
                                            moveHorizontallySize,
                                            landscape ? this.printPageWidth : this.printPageWidthPortrait,
                                            landscape ? this.printPageHeight : this.printPageHeightPortrait,
                                            options,
                                            false,
                                            1);
            var w=window.open();
            w.document.write('<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">');
            //w.document.write("<link rel='stylesheet' type='text/css' href='print.css' />");
            if (landscape) {
                w.document.write("<style type='text/css' media='print'>@page { size: landscape; }</style>");
            } else {
                w.document.write("<style type='text/css' media='print'>@page { size: portrait; }</style>");
            }
            w.document.write("<style type='text/css'>"+
                "* {margin: 0;}" +
                "html, body { height: 100%; }" +
                ".wrapper { min-height: 100%; height: auto !important; height: 100%; }" +
                ".break_here { page-break-before:always; }" +
                ".abnormality-list { list-style-type: none; }" +
                ".abnormality-color { margin-right: 8px; }" +
                ".legend-item-name {font-size: 11pt }" +
                ".patient-info {font-size: 11pt }" +
                "</style>");

            for (var pageNumY = 0; pageNumY < pages.pagesTall; pageNumY++) {
                for (var pageNumX = 0; pageNumX < pages.pagesWide; pageNumX++) {
                    if (printPageSet && !printPageSet["x" + pageNumX + "y" + pageNumY]) {
                        // skip pages marked to be skipped by the user
                        continue;
                    }
                    var patientInfoOnThisPage = (pageNumY == 0 && options.includePatientInfo);
                    if(patientInfoOnThisPage) {
                        var content = (pageNumX == 0) ? pages.patientInfoHTML : "";
                        w.document.write("<div id='patientInfo' class='header patient-info' style='height: " + pages.patientInfoHeight + "px;'>" + content + "</div>");
                    }
                    var page = pages.pages[pageNumY][pageNumX];
                    var bottomLeftPage = (pageNumY == (pages.pagesTall - 1)) && (pageNumX == 0);
                    var spaceForLegend = options.includeLegend && (pages.legendHeight > 0) && options.legendAtBottom && bottomLeftPage && !pages.needLegendOnSeparatePage;
                    if (spaceForLegend) {
                        var skipOnTop = patientInfoOnThisPage ? -pages.patientInfoHeight : 0;
                        w.document.write("<div class='wrapper' style='margin: " + skipOnTop + "px auto -" + pages.legendHeight + "px;'>");
                        if (patientInfoOnThisPage) {
                            w.document.write("<div style='height: " + pages.patientInfoHeight + "px;'></div>");
                        }
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
                    } else if (!this._isLastPage(pageNumY, pageNumX, pages, printPageSet)) {  // IE9 is not smart enough to realize there is no page after the very last break
                        w.document.write("<div class='break_here'></div>");
                    }
                }
            }

            if (options.includeLegend && options.legendAtBottom && pages.needLegendOnSeparatePage) {
                w.document.write("<div class='break_here'></div>");
                w.document.write("<div id='legend' class='print-legend'>" + pages.legendHTML + "</div>");
            }
            w.document.close(); // to prevent infinite "page loading"

            w.print();
            if (options.closeAfterPrint) {
                w.close();
            }
        },

        _isLastPage: function(pageY, pageX, pages, printPageSet) {
            for (var pageNumY = pageY; pageNumY < pages.pagesTall; pageNumY++) {
                var startX = ((pageNumY == pageY) ? pageX + 1 : 0);
                for (var pageNumX = startX; pageNumX < pages.pagesWide; pageNumX++) {
                    if (printPageSet && !printPageSet["x" + pageNumX + "y" + pageNumY]) {
                        continue;  // skip pages marked to be skipped by the user
                    }
                    return false;  // yes, there is another page to be printed
                }
            }
            return true;
        }
    });

    return PrintEngine;
});