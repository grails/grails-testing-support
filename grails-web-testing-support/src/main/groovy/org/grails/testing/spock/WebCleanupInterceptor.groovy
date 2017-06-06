package org.grails.testing.spock

import grails.testing.web.GrailsWebUnitTest
import groovy.transform.CompileStatic
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.springframework.web.context.request.RequestContextHolder

@CompileStatic
class WebCleanupInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        GrailsWebUnitTest test = (GrailsWebUnitTest)invocation.instance
        cleanup(test)
        invocation.proceed()
    }

    void cleanup(GrailsWebUnitTest test) {
        test.views.clear()
        RequestContextHolder.resetRequestAttributes()
        GrailsWebRequest webRequest = test.webRequest
        def ctx = webRequest?.applicationContext
        if (ctx?.containsBean("groovyPagesTemplateEngine")) {
            ctx.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine).clearPageCache()
        }
        if (ctx?.containsBean("groovyPagesTemplateRenderer")) {
            ctx.getBean("groovyPagesTemplateRenderer", GroovyPagesTemplateRenderer).clearCache()
        }
        test.webRequest = null
    }
}
