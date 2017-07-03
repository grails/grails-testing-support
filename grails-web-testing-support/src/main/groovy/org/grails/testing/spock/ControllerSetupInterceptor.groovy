package org.grails.testing.spock

import grails.testing.web.controllers.ControllerUnitTest
import grails.util.GrailsNameUtils
import groovy.transform.CompileStatic
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.web.util.GrailsApplicationAttributes
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation

@CompileStatic
class ControllerSetupInterceptor implements IMethodInterceptor {

    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        ControllerUnitTest test = (ControllerUnitTest)invocation.instance
        def controller = ((ControllerUnitTest)test).artefactInstance
        test.webRequest.request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
        test.webRequest.controllerName = GrailsNameUtils.getLogicalPropertyName(controller.class.name, ControllerArtefactHandler.TYPE)
        invocation.proceed()
    }
}
