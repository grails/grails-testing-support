package org.grails.testing.spock

import grails.build.support.MetaClassRegistryCleaner
import grails.testing.spring.AutowiredTest
import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

@CompileStatic
class FreshRuntimeInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        MetaClassRegistryCleaner metaClassRegistryListener = MetaClassRegistryCleaner.createAndRegister()
        invocation.proceed()
        MetaClassRegistryCleaner.cleanAndRemove(metaClassRegistryListener)
    }
}
