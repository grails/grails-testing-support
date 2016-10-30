package org.grails.testing

import grails.core.GrailsApplication
import grails.test.runtime.TestRuntime
import grails.test.runtime.TestRuntimeFactory
import org.grails.plugins.testing.GrailsMockHttpServletRequest
import org.grails.plugins.testing.GrailsMockHttpServletResponse
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ConfigurableApplicationContext

trait GrailsUnitTest<T> {

    private TestRuntime currentRuntime;

    ConfigurableApplicationContext getApplicationContext() {
        getMainContext()
    }

    ConfigurableApplicationContext getMainContext() {
        (ConfigurableApplicationContext)grailsApplication.mainContext
    }

    GrailsApplication getGrailsApplication() {
        (GrailsApplication)runtime.getValue("grailsApplication")
    }

    public TestRuntime getRuntime() {
        if(currentRuntime == null) {
            currentRuntime = TestRuntimeFactory.getRuntimeForTestClass(this.class);
        }
        if(currentRuntime == null) {
            throw new IllegalStateException("Current TestRuntime instance is null.");
        } else if (currentRuntime.isClosed()) {
            throw new IllegalStateException("Current TestRuntime instance is closed.");
        }
        return currentRuntime;
    }

    public void setRuntime(TestRuntime runtime) {
        this.currentRuntime = runtime;
    }

    void defineBeans(boolean immediateDelivery = true, Closure<?> closure) {
        runtime.publishEvent("defineBeans", [closure: closure], [immediateDelivery: immediateDelivery])
    }
}
