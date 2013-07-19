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

import java.io.File;

import org.apache.ctakes.core.resource.FileLocator;
import org.apache.log4j.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.uima.resource.DataResource;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.SharedResourceObject;
import org.apache.uima.resource.metadata.ConfigurationParameterSettings;

public class Lucene4IndexReaderResourceImpl 
             implements Lucene4IndexReaderResource, SharedResourceObject {
    // LOG4J logger based on class name
    private Logger iv_logger = Logger.getLogger(getClass().getName());

    private IndexReader iv_indexReader;

    /**
     * Loads a Lucene index for reading.
     */
    public void load(DataResource dr) throws ResourceInitializationException {

     ConfigurationParameterSettings cps = 
                dr.getMetaData().getConfigurationParameterSettings();
     Boolean useMemoryIndex = (Boolean) cps.getParameterValue("UseMemoryIndex");

        String indexDirStr = (String) cps.getParameterValue("IndexDirectory");
        try {

            File indexDir = FileLocator.locateFile(indexDirStr);

            if (!indexDir.exists()) {
              iv_logger.info("indexDir=" + indexDirStr + "  does not exist!");
            } else {
              iv_logger.info("indexDir=" + indexDirStr + "  exists.");
            }
            if (useMemoryIndex.booleanValue()) {

                iv_logger.info("Loading Lucene Index into memory: " + indexDir);
                FSDirectory fsd = FSDirectory.open(indexDir);                
                Directory d = new RAMDirectory(fsd, IOContext.READONCE);
                iv_indexReader = DirectoryReader.open(d);
            } else {
                iv_logger.info("Loading Lucene Index: " + indexDir);
                FSDirectory fsd = FSDirectory.open(indexDir);
                iv_indexReader = DirectoryReader.open(fsd);
            }
        iv_logger.info("Loaded Lucene Index, # docs=" + iv_indexReader.numDocs());
        } catch (Exception e) {
            throw new ResourceInitializationException(e);
        }
    }

    public final IndexReader getIndexReader() {
        return iv_indexReader;
    }
}
