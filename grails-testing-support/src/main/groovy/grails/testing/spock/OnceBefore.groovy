package grails.testing.spock

import groovy.transform.AnnotationCollector
import org.junit.Before

/**
 * This annotation may be applied to fixture methods in a Spock Spec that should
 * be run once and only once before any test methods are run.  Methods marked
 * with this interface will automatically be marked with @org.junit.Before and
 * {@link grails.testing.spock.RunOnce}.
 *
 * @see org.junit.Before
 * @see RunOnce
 */
@AnnotationCollector([Before, RunOnce])
@interface OnceBefore {
}
