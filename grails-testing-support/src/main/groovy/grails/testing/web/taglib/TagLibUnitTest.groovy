/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package grails.testing.web.taglib

import grails.artefact.TagLibrary
import grails.core.GrailsTagLibClass
import grails.testing.web.controllers.ControllerUnitTest
import groovy.text.Template
import org.grails.buffer.GrailsPrintWriter
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.taglib.TagLibraryLookup

trait TagLibUnitTest<T> extends ControllerUnitTest<T> {
    /**
     * Renders a template for the given contents and model
     *
     * @param contents The contents
     * @param model The model
     * @return The rendered template
     */
    String applyTemplate(String contents, Map model = [:]) {
        def sw = new StringWriter()
        applyTemplate sw, contents, model
        return sw.toString()
    }

    void applyTemplate(StringWriter sw, template, params = [:]) {
        def engine = applicationContext.getBean(GroovyPagesTemplateEngine)

        def t = engine.createTemplate(template, "test_" + System.currentTimeMillis())
        renderTemplateToStringWriter(sw, t, params)
    }

    private renderTemplateToStringWriter(StringWriter sw, Template t, params) {
        if (!webRequest.controllerName) {
            webRequest.controllerName = 'test'
        }
        if (!webRequest.actionName) {
            webRequest.actionName = 'index'
        }
        def w = t.make(params)
        def previousOut = webRequest.out
        try {
            def out = new GrailsPrintWriter(sw)
            webRequest.out = out
            w.writeTo(out)

        }
        finally {
            webRequest.out = previousOut
        }
    }

    /**
     * Mocks a tag library, making it available to subsequent calls to controllers mocked via
     * {@link #mockArtefact(Class) } and GSPs rendered via {@link #applyTemplate(String, Map) }
     *
     * @param tagLibClass The tag library class
     * @return The tag library instance
     */

    void mockArtefact(Class<T> tagLibClass) {
        GrailsTagLibClass tagLib = grailsApplication.addArtefact(TagLibArtefactHandler.TYPE, tagLibClass)
        final tagLookup = applicationContext.getBean(TagLibraryLookup)


        defineBeans(true) {
            "${tagLib.fullName}"(tagLibClass) { bean ->
                bean.autowire = true
            }
        }

        tagLookup.registerTagLib(tagLib)

        def taglibObject = applicationContext.getBean(tagLib.fullName)
        if(taglibObject instanceof TagLibrary) {
            ((TagLibrary)taglibObject).setTagLibraryLookup(tagLookup)
        }
    }

    T getTagLib() {
        getArtefactInstance()
    }
}
