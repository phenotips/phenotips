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
package org.phenotips.ctakes;

/**Class for handling Extracted Text. */
public class ExtractedTerm
{
    /** Begin Index of annotation. */
    private int beginIndex;

    /** End Index of annotation. */
    private int endIndex;

    /** Extracted Text of annotation. */
    private String extractedText;

    
    
    /**Blank Constructor. */
    public ExtractedTerm() {
        
    }

    /**Constructor to create Object. */
    public ExtractedTerm(int beginIndex, int endIndex, String extractedText, String hpoTerm) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.extractedText = extractedText;
    }
    
    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
    public int getBeginIndex() {
        return this.beginIndex;
    }
    public int getEndIndex() {
        return this.endIndex;
    }
    public String getExtractedText() {
        return this.extractedText;
    }
}

