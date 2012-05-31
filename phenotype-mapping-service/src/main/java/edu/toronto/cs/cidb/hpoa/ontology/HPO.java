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
package edu.toronto.cs.cidb.hpoa.ontology;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.environment.Environment;

@Component
@Named("hpo")
@Singleton
public class HPO extends AbstractOntology implements Initializable
{
    @Inject
    private Environment environment;

    private static HPO instance;

    // @Inject
    // @Named("solr")
    // private ScriptService service;

    public void initialize()
    {
        // if (this.service != null) {
        // this.load((SolrScriptService) this.service);
        // } else {
        this.load(getInputFileHandler(
            "http://compbio.charite.de/svn/hpo/trunk/src/ontology/human-phenotype-ontology.obo", false));
        // }
        instance = this;
    }

    public File getInputFileHandler(String inputLocation, boolean forceUpdate)
    {
        try {
            File result = new File(inputLocation);
            if (!result.exists()) {
                String name = inputLocation.substring(inputLocation.lastIndexOf('/') + 1);
                result = getTemporaryFile(name);
                if (!result.exists()) {
                    result.createNewFile();
                    BufferedInputStream in = new BufferedInputStream((new URL(inputLocation)).openStream());
                    OutputStream out = new FileOutputStream(result);
                    IOUtils.copy(in, out);
                    out.flush();
                    out.close();
                }
            }
            return result;
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected File getTemporaryFile(String name)
    {
        return getInternalFile(name, "tmp");
    }

    protected File getInternalFile(String name, String dir)
    {
        File parent = new File(this.environment.getTemporaryDirectory(), dir);
        if (!parent.exists()) {
            parent.mkdirs();
        }
        return new File(parent, name);
    }

    public static HPO getInstance()
    {
        return instance;
    }
}
