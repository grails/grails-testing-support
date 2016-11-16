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
package grails.testing.web.controllers

import grails.core.GrailsClass
import grails.core.GrailsControllerClass
import grails.util.GrailsMetaClassUtils
import grails.web.mime.MimeType
import grails.web.mvc.FlashScope
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.testing.ParameterizedGrailsUnitTest
import org.grails.web.pages.GroovyPagesUriSupport
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.context.MessageSource
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext

@CompileStatic
trait ControllerUnitTest<T> extends ParameterizedGrailsUnitTest<T> {

    static String FORM_CONTENT_TYPE = MimeType.FORM.name
    static String MULTIPART_FORM_CONTENT_TYPE = MimeType.MULTIPART_FORM.name
    static String ALL_CONTENT_TYPE = MimeType.ALL.name
    static String HTML_CONTENT_TYPE = MimeType.HTML.name
    static String XHTML_CONTENT_TYPE = MimeType.XHTML.name
    static String XML_CONTENT_TYPE = MimeType.XML.name
    static String JSON_CONTENT_TYPE = MimeType.JSON.name
    static String TEXT_XML_CONTENT_TYPE = MimeType.TEXT_XML.name
    static String TEXT_JSON_CONTENT_TYPE = MimeType.TEXT_JSON.name
    static String HAL_JSON_CONTENT_TYPE = MimeType.HAL_JSON.name
    static String HAL_XML_CONTENT_TYPE = MimeType.HAL_XML.name
    static String ATOM_XML_CONTENT_TYPE = MimeType.ATOM_XML.name

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
     * @return The model of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map getModel() {
        request.getAttribute(GrailsApplicationAttributes.CONTROLLER)?.modelAndView?.model ?: [:]
    }

    /**
     * @return The view of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    String getView() {
        final controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)

        final viewName = controller?.modelAndView?.viewName
        if (viewName != null) {
            return viewName
        }

        if (webRequest.controllerName && webRequest.actionName) {
            new GroovyPagesUriSupport().getViewURI(webRequest.controllerName, webRequest.actionName)
        } else {
            return null
        }
    }

    /**
     * The Grails 'flash' object
     * @return
     */
    FlashScope getFlash() {
        webRequest.getFlashScope()
    }

    /**
     * Mocks a Grails controller class, providing the needed behavior and defining it in the ApplicationContext
     *
     * @param controllerClass The controller class
     * @return An instance of the controller
     */
    @CompileDynamic
    void mockArtefact(Class<?> controllerClass) {
        GrailsClass controllerArtefact = createAndEnhance(controllerClass)
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
    private GrailsClass createAndEnhance(Class controllerClass) {
        final GrailsControllerClass controllerArtefact = (GrailsControllerClass) grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        controllerArtefact.initialize()
        return controllerArtefact
    }

    T getController() {
        getArtefactInstance()
    }

    MessageSource getMessageSource() {
        applicationContext.getBean("messageSource", MessageSource)
    }
}
