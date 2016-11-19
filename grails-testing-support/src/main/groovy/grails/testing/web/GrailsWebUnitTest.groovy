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
package grails.testing.web

import grails.artefact.TagLibrary
import grails.core.GrailsClass
import grails.core.GrailsControllerClass
import grails.core.GrailsTagLibClass
import grails.util.GrailsMetaClassUtils
import grails.web.mvc.FlashScope
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.taglib.TagLibraryLookup
import org.grails.testing.GrailsUnitTest
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext

@CompileStatic
trait GrailsWebUnitTest implements GrailsUnitTest {
    GrailsWebRequest getWebRequest() {
        (GrailsWebRequest) runtime.getValue("webRequest")
    }

    GrailsMockHttpServletRequest getRequest() {
        return (GrailsMockHttpServletRequest) getWebRequest().getCurrentRequest()
    }

    GrailsMockHttpServletResponse getResponse() {
        return (GrailsMockHttpServletResponse) getWebRequest().getCurrentResponse()
    }

    MockServletContext getServletContext() {
        (MockServletContext) runtime.getValue("servletContext")
    }

    Map<String, String> getGroovyPages() {
        (Map<String, String>) runtime.getValue("groovyPages")
    }

    Map<String, String> getViews() {
        getGroovyPages()
    }

    /**
     * The {@link org.springframework.mock.web.MockHttpSession} instance
     */
    MockHttpSession getSession() {
        (MockHttpSession) request.session
    }

    /**
     * @return The status code of the response
     */
    int getStatus() {
        response.status
    }

    /**
     * The Grails 'params' object which is an instance of {@link grails.web.servlet.mvc.GrailsParameterMap}
     */
    GrailsParameterMap getParams() {
        webRequest.getParams()
    }

    /**
     * The Grails 'flash' object
     * @return
     */
    FlashScope getFlash() {
        webRequest.getFlashScope()
    }

    @CompileDynamic
    void mockTagLib(Class<?> tagLibClass) {
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

    @CompileDynamic
    void mockController(Class<?> controllerClass) {
        GrailsClass controllerArtefact = createAndEnhanceController(controllerClass)
        defineBeans(true) {
            "$controllerClass.name"(controllerClass) { bean ->
                bean.scope = 'prototype'
                bean.autowire = true
            }
        }

        def callable = { ->
            final controller = applicationContext.getBean(controllerClass.name)
            webRequest.controllerName = controllerArtefact.logicalPropertyName
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
            controller
        }

        GrailsMetaClassUtils.getExpandoMetaClass(controllerClass).constructor = callable

        callable.call()
    }

    @CompileStatic
    private GrailsClass createAndEnhanceController(Class controllerClass) {
        final GrailsControllerClass controllerArtefact = (GrailsControllerClass) grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        controllerArtefact.initialize()
        return controllerArtefact
    }


    void mockTagLibs(Class<?>... tagLibClasses) {
        for(Class c : tagLibClasses) {
            mockTagLib c
        }
    }
}
