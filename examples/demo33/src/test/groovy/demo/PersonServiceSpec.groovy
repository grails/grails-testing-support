package demo

import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import spock.lang.Ignore
import spock.lang.Issue
import spock.lang.Specification

@Issue('grails/grails-testing-support#18')
@Ignore('Cannot invoke "org.grails.orm.hibernate.HibernateGormEnhancer.registerEntity(org.grails.datastore.mapping.model.PersistentEntity)" because "this.this$0.gormEnhancer" is null')
class PersonServiceSpec extends Specification implements ServiceUnitTest<PersonService>, DataTest {

    void setupSpec() {
        mockDomain Person
    }

    void 'test one'() {
        expect:
        service.list().size() == 0
    }

    void 'test two'() {
        expect:
        service.list().size() == 0
    }
}
