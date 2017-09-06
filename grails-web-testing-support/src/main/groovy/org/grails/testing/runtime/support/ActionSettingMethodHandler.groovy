package org.grails.testing.runtime.support

import grails.web.Action
import groovy.transform.CompileStatic
import javassist.util.proxy.MethodHandler
import org.grails.web.servlet.mvc.GrailsWebRequest

import java.lang.reflect.Method

@CompileStatic
class ActionSettingMethodHandler implements MethodHandler {

    GrailsWebRequest request
    Object controller

    ActionSettingMethodHandler(Object controller, GrailsWebRequest request) {
        this.request = request
        this.controller = controller
    }

    @Override
    Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) throws Throwable {
        if (thisMethod.getAnnotation(Action) != null) {
            request.setActionName(thisMethod.name)
        }
        thisMethod.invoke(controller, args)
    }
}