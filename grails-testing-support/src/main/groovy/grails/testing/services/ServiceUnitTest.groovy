package grails.testing.services

import groovy.transform.CompileStatic
import org.grails.core.artefact.ServiceArtefactHandler
import org.grails.testing.GrailsUnitTest

trait ServiceUnitTest<T> extends GrailsUnitTest<T> {
    /**
     * Mocks a service class, registering it with the application context
     *
     * @param serviceClass The service class
     * @return An instance of the service
     */
    def <T> T mockService(Class<T> serviceClass) {
        final serviceArtefact = grailsApplication.addArtefact(ServiceArtefactHandler.TYPE, serviceClass)

        defineBeans(true) {
            "${serviceArtefact.propertyName}"(serviceClass) { bean ->
                bean.autowire = true
            }
        }

        applicationContext.getBean(serviceArtefact.propertyName, serviceClass)
    }

    T getService() {
        getCollaboratorInstance()
    }
}
