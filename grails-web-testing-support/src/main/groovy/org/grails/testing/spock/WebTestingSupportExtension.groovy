package org.grails.testing.spock

import grails.testing.web.GrailsWebUnitTest
import grails.testing.web.UrlMappingTest
import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class WebTestingSupportExtension extends AbstractGlobalExtension {

    WebSetupSpecInterceptor webSetupSpecInterceptor = new WebSetupSpecInterceptor()
    WebSetupInterceptor webSetupInterceptor = new WebSetupInterceptor()
    WebCleanupInterceptor webCleanupInterceptor = new WebCleanupInterceptor()
    WebCleanupSpecInterceptor webCleanupSpecInterceptor = new WebCleanupSpecInterceptor()
    UrlMappingSetupSpecInterceptor urlMappingSetupSpecInterceptor = new UrlMappingSetupSpecInterceptor()

    void visitSpec(SpecInfo spec) {
        if (GrailsWebUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(webSetupSpecInterceptor)
            spec.addSetupInterceptor(webSetupInterceptor)
            spec.addCleanupInterceptor(webCleanupInterceptor)
            spec.addCleanupSpecInterceptor(webCleanupSpecInterceptor)
        }

        if (UrlMappingTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(urlMappingSetupSpecInterceptor)
        }
    }
}
