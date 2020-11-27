package demo

import grails.test.hibernate.HibernateSpec
import grails.testing.web.controllers.ControllerUnitTest

class PersonControllerHibernateSpec extends HibernateSpec implements ControllerUnitTest<PersonController> {

    void "test action which invokes GORM method"() {

        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        when:
        def model = controller.index()

        then:
        model.people.size() == 2
    }
}
