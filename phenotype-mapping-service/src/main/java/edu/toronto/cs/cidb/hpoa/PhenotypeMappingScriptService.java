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
package edu.toronto.cs.cidb.hpoa;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;
import org.xwiki.script.service.ScriptService;

import edu.toronto.cs.cidb.hpoa.annotation.OmimHPOAnnotations;
import edu.toronto.cs.cidb.hpoa.annotation.SearchResult;
import edu.toronto.cs.cidb.hpoa.ontology.Ontology;
import edu.toronto.cs.cidb.hpoa.prediction.Predictor;

@Component
@Named("hpoa")
@Singleton
public class PhenotypeMappingScriptService implements ScriptService, Initializable
{
    @Inject
    private Environment environment;

    @Inject
    @Named("hpo")
    private Ontology hpo;

    @Inject
    @Named("ic")
    private Predictor predictor;

    public List<SearchResult> getMatches(Collection<String> phenotypes)
    {
        return this.predictor.getMatches(phenotypes);
    }

    public List<SearchResult> getMatches(Collection<String> phenotypes, int limit)
    {
        List<SearchResult> results = this.predictor.getMatches(phenotypes);
        if (limit < results.size()) {
            return results.subList(0, limit);
        }
        return results;
    }

    public List<SearchResult> getDifferentialPhenotypes(Collection<String> phenotypes)
    {
        return this.predictor.getDifferentialPhenotypes(phenotypes);
    }

    public List<SearchResult> getDifferentialPhenotypes(Collection<String> phenotypes, int limit)
    {
        List<SearchResult> results = this.predictor.getDifferentialPhenotypes(phenotypes);
        if (limit < results.size()) {
            return results.subList(0, limit);
        }
        return results;
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

    @Override
    public void initialize() throws InitializationException
    {
        OmimHPOAnnotations ann = new OmimHPOAnnotations(this.hpo);
        ann.load(getInputFileHandler("http://compbio.charite.de/hudson/job/hpo.annotations/" +
                "lastStableBuild/artifact/misc/phenotype_annotation.tab", false));

        this.predictor.setAnnotation(ann);
    }
}
