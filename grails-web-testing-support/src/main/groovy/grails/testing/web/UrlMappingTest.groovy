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

import grails.core.GrailsControllerClass
import grails.web.mapping.UrlMappingsHolder
import groovy.transform.CompileDynamic
import junit.framework.AssertionFailedError
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.core.artefact.UrlMappingsArtefactHandler
import org.grails.testing.ParameterizedGrailsUnitTest
import org.grails.web.mapping.UrlMappingsHolderFactoryBean
import org.junit.Before

import static junit.framework.Assert.assertEquals
import static junit.framework.Assert.assertNotNull

trait UrlMappingTest<T> implements ParameterizedGrailsUnitTest<T>, GrailsWebUnitTest {

    Class[] getControllersToMock() {
        []
    }

    @Before
    void configuredMockedControllers() {
        for(Class c : controllersToMock) {
            mockController c
        }
        getArtefactInstance()
    }

    void assertUrlMapping(Map assertions, url) {
        assertUrlMapping(assertions, url, null)
    }

    void assertUrlMapping(Map assertions, url, Closure paramAssertions) {
        assertForwardUrlMapping(assertions, url, paramAssertions)
        if (assertions.controller && !(url instanceof Integer)) {
            assertReverseUrlMapping(assertions, url, paramAssertions)
        }
    }

    void assertForwardUrlMapping(assertions, url, paramAssertions) {

        UrlMappingsHolder mappingsHolder = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)
        if (assertions.action && !assertions.controller) {
            throw new AssertionFailedError("Cannot assert action for url mapping without asserting controller")
        }

        if (assertions.controller) assertController(assertions.controller, url)
        if (assertions.action) assertAction(assertions.controller, assertions.action, url)
        if (assertions.view) assertView(assertions.controller, assertions.view, url)

        def mappingInfos
        if (url instanceof Integer) {
            mappingInfos = []
            def mapping
            if (assertions."$KEY_EXCEPTION") {
                mapping = mappingsHolder.matchStatusCode(url, assertions."$KEY_EXCEPTION" as Throwable)
            } else {
                mapping = mappingsHolder.matchStatusCode(url)
            }
            if (mapping) mappingInfos << mapping
        }
        else {
            mappingInfos = mappingsHolder.matchAll(url, request.method)
        }

        if (mappingInfos.size() == 0) throw new AssertionFailedError("url '$url' did not match any mappings")

        def mappingMatched = mappingInfos.any {mapping ->
            mapping.configure(webRequest)
            for (key in ["controller", "action", "view"]) {
                if (assertions.containsKey(key)) {
                    def expected = assertions[key]
                    def actual = mapping."${key}Name"

                    switch (key) {
                        case "controller":
                            if (actual && !getControllerClass(actual)) return false
                            break
                        case "view":
                            if (actual[0] == "/") actual = actual.substring(1)
                            if (expected[0] == "/") expected = expected.substring(1)
                            break
                        case "action":
                            if (key == "action" && actual == null) {
                                final controllerClass = getControllerClass(assertions.controller)
                                actual = controllerClass?.defaultAction
                            }
                            break
                    }

                    assertEquals("Url mapping $key assertion for '$url' failed", expected, actual)
                }
            }
            if (paramAssertions) {
                def params = [:]
                paramAssertions.delegate = params
                paramAssertions.resolveStrategy = Closure.DELEGATE_ONLY
                paramAssertions.call()
                params.each {name, value ->
                    assertEquals("Url mapping '$name' parameter assertion for '$url' failed", value, mapping.parameters[name])
                }
            }
            return true
        }

        if (!mappingMatched) throw new IllegalArgumentException("url '$url' did not match any mappings")
    }
    void assertReverseUrlMapping(Map assertions, url, Closure paramAssertions) {
        UrlMappingsHolder mappingsHolder = applicationContext.getBean("grailsUrlMappingsHolder", UrlMappingsHolder)
        def controller = assertions.controller
        def action = assertions.action
        def method = assertions.method
        def params = [:]
        if (paramAssertions) {
            paramAssertions.delegate = params
            paramAssertions.resolveStrategy = Closure.DELEGATE_ONLY
            paramAssertions.call()
        }
        def urlCreator = mappingsHolder.getReverseMapping(controller, action, null, null, method, params)
        assertNotNull("could not create reverse mapping of '$url' for {controller = $controller, action = $action, params = $params}", urlCreator)
        def createdUrl = urlCreator.createRelativeURL(controller, action, params, "UTF-8")
        assertEquals("reverse mapping assertion for {controller = $controller, action = $action, params = $params}", url, createdUrl)
    }

    void assertController(controller, url) {
        final controllerClass = getControllerClass(controller)
        if (!controllerClass) {
            throw new AssertionFailedError("Url mapping assertion for '$url' failed, '$controller' is not a valid controller")
        }
    }
    void assertAction(controller, action, url) {
        final controllerClass = getControllerClass(controller)

        if (!controllerClass?.mapsToURI("/$controller/$action")) {
            throw new AssertionFailedError("Url mapping assertion for '$url' failed, '$action' is not a valid action of controller '$controller'")
        }
    }

    public GrailsControllerClass getControllerClass(controller) {
        return grailsApplication.getArtefactByLogicalPropertyName(ControllerArtefactHandler.TYPE, controller)
    }

    @CompileDynamic
    void mockArtefact(Class<?> urlMappingsClass) {
        grailsApplication.addArtefact(UrlMappingsArtefactHandler.TYPE, urlMappingsClass)

        defineBeans(true) {
            grailsUrlMappingsHolder(UrlMappingsHolderFactoryBean) {
                getDelegate().grailsApplication = grailsApplication
            }
        }
    }

}
