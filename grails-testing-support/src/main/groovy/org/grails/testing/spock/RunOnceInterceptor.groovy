package org.grails.testing.spock

import groovy.transform.CompileStatic
import org.spockframework.runtime.extension.AbstractMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

@CompileStatic
class RunOnceInterceptor extends AbstractMethodInterceptor {
    protected Set<String> processedMethods = [] as Set

    @Override
    void interceptSetupMethod(IMethodInvocation invocation) throws Throwable {
        String testClassName = invocation.instance.getClass().name
        if(!processedMethods.contains(testClassName)) {
            processedMethods.add testClassName
            invocation.proceed()
        }
    }
}
