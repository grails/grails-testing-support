package org.grails.testing.gorm.spock

import grails.testing.gorm.DataTest
import grails.validation.ConstrainedProperty
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.core.DatastoreUtils
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.grails.datastore.mapping.simple.SimpleMapDatastore
import org.spockframework.runtime.extension.AbstractGlobalExtension
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.SpecInfo

@CompileStatic
class GormTestingSupportExtension extends AbstractGlobalExtension {

    DataTestSetupSpecInterceptor dataTestSetupSpecInterceptor = new DataTestSetupSpecInterceptor()
    DataTestSetupInterceptor dataTestSetupInterceptor = new DataTestSetupInterceptor()
    DataTestCleanupInterceptor dataTestCleanupInterceptor = new DataTestCleanupInterceptor()
    DataTestCleanupSpecInterceptor dataTestCleanupSpecInterceptor = new DataTestCleanupSpecInterceptor()

    @Override
    void visitSpec(SpecInfo spec) {
        if (DataTest.isAssignableFrom(spec.reflection)) {
            spec.addSetupSpecInterceptor(dataTestSetupSpecInterceptor)
            spec.addSetupInterceptor(dataTestSetupInterceptor)
            spec.addCleanupInterceptor(dataTestCleanupInterceptor)
            spec.addCleanupSpecInterceptor(dataTestCleanupSpecInterceptor)
        }
    }
}
