package org.grails.testing.gorm.spock

import grails.validation.ConstrainedProperty
import groovy.transform.CompileStatic
import org.grails.datastore.mapping.reflect.ClassPropertyFetcher
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

@CompileStatic
class DataTestCleanupSpecInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        ClassPropertyFetcher.clearCache()
        ConstrainedProperty.removeConstraint("unique")
        invocation.proceed()
    }
}
