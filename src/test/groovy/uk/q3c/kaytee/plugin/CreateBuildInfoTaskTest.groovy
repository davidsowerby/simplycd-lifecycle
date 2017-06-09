package uk.q3c.kaytee.plugin

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import uk.q3c.build.gitplus.gitplus.GitPlus
/**
 * Created by David Sowerby on 06 Dec 2016
 */
class CreateBuildInfoTaskTest extends Specification {


    CreateBuildInfoTask task
    CreateBuildInfoTaskDelegate delegate = Mock(CreateBuildInfoTaskDelegate)
    GitPlus gitPlus = Mock(GitPlus)
    KayTeeExtension ktConfig

    def setup() {
        Project project = ProjectBuilder.builder().build()
        ktConfig = new KayTeeExtension()
        project.extensions.add("kaytee", ktConfig)
        task = project.getTasks().create("buildInfo", CreateBuildInfoTask.class)
        task.delegate = delegate
    }

    def "Write build info"() {
        given:
        delegate.gitPlus >> gitPlus

        when:
        task.writeInfo()

        then:
        1 * delegate.writeInfo()

        then:
        1 * gitPlus.close()
    }

    def "No fail if gitPlus is null"() {
        given:
        delegate.gitPlus >> null

        when:
        task.writeInfo()

        then:
        1 * delegate.writeInfo()
    }

    def "exception from delegate"() {
        given:
        delegate.gitPlus >> gitPlus

        when:
        task.writeInfo()

        then:
        1 * delegate.writeInfo() >> { throw new NullPointerException() }
        1 * gitPlus.close()
        thrown(GradleException)
    }
}