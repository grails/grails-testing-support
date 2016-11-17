package demo

import grails.testing.gorm.DataTest

class OneTimeSetupTest implements DataTest {

    static int setupCounter = 0

    void oneTimeSetup() {
        ++setupCounter
        mockDomain Person
    }

    void "test basic persistence mocking"() {
        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        expect:
        Person.count() == 2
        setupCounter == 1
    }

    void "test basic persistence mocking again"() {
        setup:
        new Person(firstName: 'Robert', lastName: 'Fripp').save()
        new Person(firstName: 'Adrian', lastName: 'Belew').save()

        expect:
        Person.count() == 2
        setupCounter == 1
    }
}
