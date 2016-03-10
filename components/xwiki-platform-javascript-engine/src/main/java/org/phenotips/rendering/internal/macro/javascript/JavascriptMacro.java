/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */

package org.phenotips.rendering.internal.macro.javascript;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.AbstractJSR223ScriptMacro;
import org.xwiki.rendering.macro.script.JSR223ScriptMacroParameters;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptEngineFactory;

/**
 * @version $Id$ *
 */
@Component
@Named("javascript")
@Singleton
public class JavascriptMacro extends AbstractJSR223ScriptMacro<JSR223ScriptMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Execute a Javascript script.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the Javascript script to execute";

    /**
     * A specific XWiki Javascript Script Engine Factory.
     */
    @Inject
    @Named("javascript")
    private ScriptEngineFactory javascriptEngineFactory;


    /**
     * Create and initialize the descriptor of the macro.
     */
    public JavascriptMacro() {
        super("Javascript", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION));
    }

    @Override
    public void initialize() throws InitializationException {
        super.initialize();

        // Register Javascript Compilation Customizers by registering the XWiki JavaScript Script Engine Factory which
        // extends the default Javascript Script Engine Factory and registers Compilation Customizers.
        this.scriptEngineManager.registerEngineName("javascript", this.javascriptEngineFactory);
    }

}
