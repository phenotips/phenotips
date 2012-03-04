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
package org.xwiki.configuration.internal;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.HibernateConfigurator;
import org.xwiki.environment.Environment;

/**
 * Hibernate configurator that takes configuration values in hibernate.cfg.xml from the environment.
 * 
 * @version $Id$
 */
@Component
@Named("hibernate-environment")
public class EnvironmentHibernateConfigurator implements HibernateConfigurator, Initializable
{
    /**
     * The Hibernate configuration file.
     */
    private static final String HIBERNATE_CONFIGURATION_FILE = "/WEB-INF/hibernate.cfg.xml";

    /**
     * Configuration.
     */
    @Inject
    @Named("cloud")
    private ConfigurationSource configurationSource;

    /**
     * The environment.
     */
    @Inject
    private Environment environment;

    /**
     * The XML configuration document.
     */
    private Document configurationDocument;

    @Override
    public void initialize() throws InitializationException
    {
        InputStream is = environment.getResourceAsStream(HIBERNATE_CONFIGURATION_FILE);
        if (is == null) {
            throw new InitializationException(String.format("No hibernate configuration file '%s' ",
                HIBERNATE_CONFIGURATION_FILE));
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();

            configurationDocument = loader.parse(is);

            configureHibernateProperties(configurationDocument.getDocumentElement());
        } catch (Exception e) {
            throw new InitializationException("Error building Hibernate configuration", e);
        }

    }

    /**
     * Do a depth first visit of the XML and override all the &lt;property&gt; values to that passed as an argument.
     * 
     * @param node The node where to start the visit.
     */
    private void configureHibernateProperties(Node node)
    {
        if ("property".equals(node.getNodeName())) {
            Node name = node.getAttributes().getNamedItem("name");
            if (name != null) {
                String overrideValue = configurationSource.getProperty(name.getTextContent());
                if (overrideValue != null) {
                    node.setTextContent(overrideValue);
                }
            }
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            configureHibernateProperties(childNodes.item(i));
        }
    }

    @Override
    public Document getConfiguration()
    {
        return configurationDocument;
    }

}
