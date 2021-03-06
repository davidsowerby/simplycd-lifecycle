package uk.q3c.kaytee.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
/**
 * Created by David Sowerby on 19 Jan 2017
 */
class ConfigToJsonTaskDelegateTest extends JsonTest {

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    Logger logger = Mock(Logger)
    ConfigToJsonTaskDelegate delegate
    Project project = Mock(Project)
    ExtensionContainer extensionContainer = Mock(ExtensionContainer)
    KayTeeExtension ktConfig = new KayTeeExtension()
    ObjectMapper objectMapper = new ObjectMapper()

    def setup() {
        temp = temporaryFolder.getRoot()
        project.buildDir >> new File(temp, 'build')
        project.logger >> logger
        project.extensions >> extensionContainer
        extensionContainer.getByName('kaytee') >> ktConfig
        delegate = new ConfigToJsonTaskDelegate(project)
    }

//    @Ignore("see https://github.com/davidsowerby/kaytee-plugin/issues/60")
    def "write default info"() {
        given:
        File actualKayTeeFile = new File(temp, 'build/kaytee.json')

        when:
        delegate.writeInfo()

        then:
        actualKayTeeFile.exists()

        when: "run a second time, so that build dir already there"
        delegate.writeInfo()

        then:
        actualKayTeeFile.exists()

        when:
        resource = ktConfig

        then:
        asExpectedFromFile("kaytee.json")
    }

    def "write modified lifecycle info"() {
        given:
        File actualSimplyFile = new File(temp, 'build/kaytee.json')

        ktConfig.integrationTest.enabled = true
        ktConfig.integrationTest.qualityGate = true
        ktConfig.integrationTest.thresholds.complexity = 44
        ktConfig.integrationTest.thresholds.method = 73
        ktConfig.acceptanceTest.enabled = true
        ktConfig.acceptanceTest.taskType = TaskType.DELEGATED
        ktConfig.acceptanceTest.delegate.repoName = 'wiggly'
        ktConfig.acceptanceTest.delegate.branch = 'master'
        ktConfig.release.mergeToMaster = false

        resource = ktConfig

        when:
        delegate.writeInfo()
        resource2 = objectMapper.readValue(actualSimplyFile, KayTeeExtension)


        then:
        resource == resource2
    }



}
