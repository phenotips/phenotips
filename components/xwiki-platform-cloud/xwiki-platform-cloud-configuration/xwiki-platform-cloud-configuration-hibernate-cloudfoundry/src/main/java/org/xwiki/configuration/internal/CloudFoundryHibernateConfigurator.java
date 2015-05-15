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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.HibernateConfigurator;
import org.xwiki.environment.Environment;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Hibernate configurator that takes configuration values in hibernate.cfg.xml from the environment.
 *
 * @version $Id$
 */
@Component
@Named("hibernate-cloudfoundry")
public class CloudFoundryHibernateConfigurator implements HibernateConfigurator, Initializable
{
    /**
     * The Hibernate configuration file.
     */
    private static final String HIBERNATE_CONFIGURATION_FILE = "/WEB-INF/hibernate.cfg.xml";

    /**
     * The prefix for hibernate-related properties.
     */
    private static final String HIBERNATE_PROPERTY_PREFIX = "hibernate.";

    /**
     * Logger.
     */
    @Inject
    private Logger logger;

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

        Map<String, String> properties = getHibernatePropertiesFromEnv();
        if (properties.get(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "connection.url")) == null) {
            logger.warn("No CloudFoundry database configuration found. Using default");
        } else {
            logger.info("CloudFoundry database configuration: %s\n", properties);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();

            configurationDocument = loader.parse(is);

            configureHibernateProperties(properties, configurationDocument.getDocumentElement());
        } catch (Exception e) {
            throw new InitializationException("Error building Hibernate configuration", e);
        }
    }

    /**
     * Do a depth first visit of the XML and override all the &lt;property&gt; values to that passed as an argument.
     *
     * @param node The node where to start the visit.
     */
    private void configureHibernateProperties(Map<String, String> properties, Node node)
    {
        if ("property".equals(node.getNodeName())) {
            Node nameAttribute = node.getAttributes().getNamedItem("name");
            if (nameAttribute != null) {
                String propertyName = nameAttribute.getTextContent();
                String overrideValue =
                        properties.get(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, propertyName));
                if (overrideValue != null) {
                    logger.debug("Setting property '{}' to '{}'", propertyName, overrideValue);
                    node.setTextContent(overrideValue);
                }
            }
        }

        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            configureHibernateProperties(properties, childNodes.item(i));
        }
    }

    @Override
    public Document getConfiguration()
    {
        return configurationDocument;
    }

    /**
     * Retrieve the information about the MySQL service from the environment.
     *
     * @return a map containing the values to be overridden (could be empty if these values are not present in the
     *         environment).
     */
    protected Map<String, String> getHibernatePropertiesFromEnv()
    {
        Map<String, String> result = new HashMap<String, String>();

        String json = System.getenv("VMC_SERVICES");

        /* Return empty properties if VMC_SERVICES is not defined */
        if (json == null) {
            return result;
        }

        JSONArray array = JSONArray.fromObject(json);

        String jdbcString = null;
        String username = null;
        String password = null;
        Object entity;
        for (int i = 0; i < array.size(); i++) {
            entity = array.get(i);
            if (entity instanceof JSONObject) {
                JSONObject serviceObject = (JSONObject) entity;
                if ("database".equals(serviceObject.get("type")) && "mysql".equals(serviceObject.get("vendor"))) {
                    JSONObject optionsObject = (JSONObject) serviceObject.get("options");
                    String host = optionsObject.get("host").toString();
                    String port = optionsObject.get("port").toString();
                    String dbName = optionsObject.get("name").toString();
                    username = optionsObject.get("username").toString();
                    password = optionsObject.get("password").toString();

                    if (host == null || port == null || dbName == null) {
                        break;
                    }

                    jdbcString = String.format(
                            "jdbc:mysql://%s:%s/%s?useServerPrepStmts=false&amp;useUnicode=true&amp;"
                                    + "characterEncoding=UTF-8",
                            host, port, dbName);
                    break;
                }
            }
        }

        if (jdbcString == null || username == null || password == null) {
            return result;
        }

        result.put(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "connection.driver_class"),
                "com.mysql.jdbc.Driver");
        result.put(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "dialect"),
                "org.hibernate.dialect.MySQL5InnoDBDialect");
        result.put(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "connection.url"), jdbcString);
        result.put(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "connection.username"), username);
        result.put(String.format("%s%s", HIBERNATE_PROPERTY_PREFIX, "connection.password"), password);

        return result;
    }
}
