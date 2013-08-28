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

/** 
 * Class for handling Extracted Text.
 * @version $Id$
 * */
public class ExtractedTerm
{
    /** Begin Index of annotation. */
    private int beginIndex;

    /** End Index of annotation. */
    private int endIndex;

    /** Extracted Text of annotation. */
    private String extractedText;

    /** Extracted HPO Id of annotation. */
    private String id;
    
    /** Extracted HPO Term of annotation. */
    private String term;


    /**Blank Constructor. */
    public ExtractedTerm() {

    }

    /**Constructor to create Object. 
     * @param beginIndex see {@link #beginIndex}
     * @param endIndex see {@link #endIndex}
     * @param extractedText see {@link #extractedText}
     * @param id see {@link #id}
     * @param term see {@link #term}
     * */
    public ExtractedTerm(int beginIndex, int endIndex,
            String extractedText, String id, String term) {
        this.beginIndex = beginIndex;
        this.endIndex = endIndex;
        this.extractedText = extractedText;
        this.id = id;
        this.term = term;
    }
    
    /** Sets the value of Begin Index. 
     * @param beginIndex see {@link #beginIndex}
     * */
    public void setBeginIndex(int beginIndex) {
        this.beginIndex = beginIndex;
    }
    
    /** Sets the value of End Index. 
     * @param endIndex see {@link #endIndex}
     * */
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    /** Sets the value of Extracted Text. 
     * @param extractedText see {@link #extractedText}
     * */
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
    }
    
    /** Sets the value of HPO Id.  
     * @param id see {@link #id}
     * */
    public void setId(String id) {
        this.id = id;
    }
    
    /** Sets the value of Actual HPO Term. 
     * @param term see {@link #term}
     * */
    public void setTerm(String term) {
        this.term = term;
    }
    
    /**
     * @return the begin index of the extracted text.
     */
    public int getBeginIndex() {
        return this.beginIndex;
    }
    
    /**
     * @return the end index of the extracted text.
     */
    public int getEndIndex() {
        return this.endIndex;
    }
    
    /**
     * @return the text actually extracted.
     */
    public String getExtractedText() {
        return this.extractedText;
    }
    
    /**
     * @return the HPO id of the phenotype corresponding to the extracted text.
     */
    public String getId() {
        return this.id;
    }
    
    /**
     * @return the HPO term of the phenotype corresponding to the extracted text.
     */
    public String getTerm() {
        return this.term;
    }
}
