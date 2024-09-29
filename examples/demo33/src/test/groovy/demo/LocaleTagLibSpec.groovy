package demo

import grails.testing.web.taglib.TagLibUnitTest
import grails.validation.Validateable
import org.springframework.context.i18n.LocaleContextHolder
import spock.lang.PendingFeature
import spock.lang.Specification

class LocaleTagLibSpec extends Specification implements TagLibUnitTest<SampleTagLib> {

    @PendingFeature(reason = 'Field error in object \'demo.Widget\' on field \'title\': rejected value [null]; codes [demo.Widget.title.nullable.error.demo.Widget.title,demo.Widget.title.nullable.error.title,demo.Widget.title.nullable.error.java.lang.String,demo.Widget.title.nullable.error,widget.title.nullable.error.demo.Widget.title,widget.title.nullable.error.title,widget.title.nullable.error.java.lang.String,widget.title.nullable.error,demo.Widget.title.nullable.demo.Widget.title,demo.Widget.title.nullable.title,demo.Widget.title.nullable.java.lang.String,demo.Widget.title.nullable,widget.title.nullable.demo.Widget.title,widget.title.nullable.title,widget.title.nullable.java.lang.String,widget.title.nullable,nullable.demo.Widget.title,nullable.title,nullable.java.lang.String,nullable]; arguments [title,class demo.Widget]; default message [Property [{0}] of class [{1}] cannot be null] grails_validation_Validateable__beforeValidateHelper=org.grails.datastore.gorm.support.BeforeValidateHelper@d7da0ae>')
    void 'test customizing messageSource'() {
        given:
        def w = new Widget()
        LocaleContextHolder.setLocale(Locale.US)
        messageSource.addMessage("demo.Widget.title.label", Locale.US, "Title Of Widget")
        messageSource.addMessage("demo.Widget.label", Locale.US, "Widget")

        when:
        w.validate()

        then:
        w.hasErrors()

        when:
        def template = '<g:renderErrors bean="${widget}" />'

        then:
        applyTemplate(template, [widget: w]).contains("<li>Property [Title Of Widget] of class [Widget] cannot be null</li>")
    }
}


class Widget implements Validateable {
    String title
}
