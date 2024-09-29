package demo

import grails.testing.gorm.DataTest
import grails.testing.web.controllers.ControllerUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class PersonControllerSpec extends Specification implements ControllerUnitTest<PersonController>, DataTest {

    void setupSpec() {
        mockDomain Person
    }

    void "test action which invokes GORM method"() {
        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        when:
        def model = controller.index()

        then:
        model.people.size() == 2
        model.keySet().contains('people')
    }
}
