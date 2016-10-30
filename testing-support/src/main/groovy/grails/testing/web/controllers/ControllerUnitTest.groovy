package grails.testing.web.controllers

import grails.core.GrailsClass
import grails.core.GrailsControllerClass
import grails.util.GrailsMetaClassUtils
import grails.web.mvc.FlashScope
import grails.web.servlet.mvc.GrailsParameterMap
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.grails.core.artefact.ControllerArtefactHandler
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.testing.GrailsUnitTest
import org.grails.web.pages.GroovyPagesUriSupport
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.mock.web.MockHttpSession
import org.springframework.mock.web.MockServletContext

trait ControllerUnitTest<T>  extends GrailsUnitTest<T> {

    GrailsWebRequest getWebRequest() {
        (GrailsWebRequest)runtime.getValue("webRequest")
    }

    GrailsMockHttpServletRequest getRequest() {
        return (GrailsMockHttpServletRequest)getWebRequest().getCurrentRequest()
    }

    GrailsMockHttpServletResponse getResponse() {
        return (GrailsMockHttpServletResponse)getWebRequest().getCurrentResponse()
    }

    MockServletContext getServletContext() {
        (MockServletContext)runtime.getValue("servletContext")
    }

    Map<String, String> getGroovyPages() {
        (Map<String, String>)runtime.getValue("groovyPages")
    }

    Map<String, String> getViews() {
        getGroovyPages()
    }

    /**
     * The {@link org.springframework.mock.web.MockHttpSession} instance
     */
    MockHttpSession getSession() {
        (MockHttpSession)request.session
    }

    /**
     * @return The status code of the response
     */
    int getStatus() {
        response.status
    }

    /**
     * The Grails 'params' object which is an instance of {@link grails.web.servlet.mvc.GrailsParameterMap}
     */
    GrailsParameterMap getParams() {
        webRequest.getParams()
    }

    /**
     * @return The model of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    Map getModel() {
        request.getAttribute(GrailsApplicationAttributes.CONTROLLER)?.modelAndView?.model ?: [:]
    }

    /**
     * @return The view of the current controller
     */
    @CompileStatic(TypeCheckingMode.SKIP)
    String getView() {
        final controller = request.getAttribute(GrailsApplicationAttributes.CONTROLLER)

        final viewName = controller?.modelAndView?.viewName
        if (viewName != null) {
            return viewName
        }

        if (webRequest.controllerName && webRequest.actionName) {
            new GroovyPagesUriSupport().getViewURI(webRequest.controllerName, webRequest.actionName)
        }
        else {
            return null
        }
    }

    /**
     * The Grails 'flash' object
     * @return
     */
    FlashScope getFlash() {
        webRequest.getFlashScope()
    }

    /**
     * Mocks a Grails controller class, providing the needed behavior and defining it in the ApplicationContext
     *
     * @param controllerClass The controller class
     * @return An instance of the controller
     */
    def <T> T mockController(Class<T> controllerClass) {
        GrailsClass controllerArtefact = createAndEnhance(controllerClass)
        defineBeans(true) {
            "$controllerClass.name"(controllerClass) { bean ->
                bean.scope = 'prototype'
                bean.autowire = true
            }
        }

        def callable = {->
            final controller = applicationContext.getBean(controllerClass.name)
            webRequest.controllerName = controllerArtefact.logicalPropertyName
            request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
            controller
        }

        GrailsMetaClassUtils.getExpandoMetaClass(controllerClass).constructor = callable

        return callable.call()
    }

    @CompileStatic
    private GrailsClass createAndEnhance(Class controllerClass) {
        final GrailsControllerClass controllerArtefact = (GrailsControllerClass)grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, controllerClass)
        controllerArtefact.initialize()
        return controllerArtefact
    }

    T getController() {
        getCollaboratorInstance()
    }

}
