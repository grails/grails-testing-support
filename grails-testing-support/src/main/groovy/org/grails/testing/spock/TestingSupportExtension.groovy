package org.grails.testing.spock

import grails.testing.runtime.FreshRuntime
import grails.testing.spring.AutowiredTest
import groovy.transform.CompileStatic
import org.grails.testing.GrailsUnitTest
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class TestingSupportExtension extends AbstractGlobalExtension {

    AutowiredInterceptor autowiredInterceptor = new AutowiredInterceptor()
    FreshRuntimeInterceptor freshRuntimeInterceptor = new FreshRuntimeInterceptor()
    CleanupContextInterceptor cleanupContextInterceptor = new CleanupContextInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (AutowiredTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupInterceptor(autowiredInterceptor)
        }
        if (spec.reflection.getAnnotation(FreshRuntime) != null) {
            spec.allFeatures.each {
                it.featureMethod.addInterceptor(freshRuntimeInterceptor)
            }
        } else {
            spec.allFeatures.each {
                if (it.featureMethod.getAnnotation(FreshRuntime) != null) {
                    it.featureMethod.addInterceptor(freshRuntimeInterceptor)
                }
            }
        }
        if (GrailsUnitTest.isAssignableFrom(spec.reflection)) {
            spec.addCleanupSpecInterceptor(cleanupContextInterceptor)
        }

    }
}
