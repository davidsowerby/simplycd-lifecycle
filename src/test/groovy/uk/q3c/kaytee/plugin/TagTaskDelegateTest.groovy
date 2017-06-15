package uk.q3c.kaytee.plugin

import org.apache.commons.codec.digest.DigestUtils
import org.eclipse.jgit.lib.PersonIdent
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import uk.q3c.build.gitplus.GitSHA
import uk.q3c.build.gitplus.local.GitBranch
import uk.q3c.build.gitplus.local.GitCommit
import uk.q3c.build.gitplus.local.GitLocalConfiguration
import uk.q3c.build.gitplus.local.Tag

import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * Created by David Sowerby on 06 Dec 2016
 */
class TagTaskDelegateTest extends Specification {

    @Rule
    TemporaryFolder temporaryFolder
    File temp
    CreateBuildInfoTaskDelegate delegate
    GitSHA gitSHA
    KayTeeVersion versionObject = Mock(KayTeeVersion)
    Project project = Mock(Project)
    Logger logger = Mock(Logger)
    File projectDir
    File buildDir
    File propertiesFile
    String version = '9.9.9.1000'
    String baseVersion = '9.9.9'
    KayTeeExtension ktConfig
    ExtensionContainer projectExtensions = Mock(ExtensionContainer)
    GitLocalConfiguration wikiConfiguration
    VersionCheckTaskDelegate versionCheck = Mock(VersionCheckTaskDelegate)
    ExtraPropertiesExtension ext = Mock(ExtraPropertiesExtension)

    def setup() {
        ktConfig = new KayTeeExtension()
        gitSHA = testSha()

        temp = temporaryFolder.getRoot()
        projectDir = new File(temp, "projectDir")
        buildDir = new File(projectDir, "build")

        project.property("baseVersion") >> baseVersion
        project.name >> "testProject"
        project.projectDir >> projectDir
        project.buildDir >> buildDir
        propertiesFile = new File(buildDir, "resources/main/buildInfo.properties")

        versionObject.toString() >> version
        project.version >> versionObject
        project.getLogger() >> logger
        project.extensions >> projectExtensions
        projectExtensions.getByName("kaytee") >> ktConfig
        projectExtensions.getExtraProperties() >> ext
        delegate = new CreateBuildInfoTaskDelegate(project, versionCheck)
    }

    def "Write build info"() {
        given:
        PersonIdent person = new PersonIdent("a", "b")
        GitCommit gitCommit = new GitCommit("x", testSha().sha, person, person)
        Tag existingTag = new Tag('9.9.9.1000', ZonedDateTime.now(), ZonedDateTime.now(), person, "msg", gitCommit, Tag.TagType.ANNOTATED)
        ktConfig.baseVersion = '9.9.9'

        when:
        delegate.writeInfo()


        then:
        1 * remoteConfiguration.active(false)
        1 * gitPlus.execute()
        1 * gitLocal.tag('9.9.9.1000', 'version 9.9.9.1000')
        1 * gitLocal.push(true, false)
        1 * gitLocal.currentBranch() >> new GitBranch('develop')
        1 * gitLocal.headCommitSHA(_) >> gitSHA
        propertiesFile.exists()

        then:
        Properties properties = new Properties()
        properties.load(new FileInputStream(propertiesFile))
        properties.get("version") == version
        properties.get("baseVersion") == baseVersion
        properties.get(CreateBuildInfoTaskDelegate.PROPERTY_NAME_COMMIT_ID) == gitSHA.sha
        properties.get("date") != null
        String dateAsString = properties.get("date")
        OffsetDateTime.parse(dateAsString).isBefore(OffsetDateTime.now())
        OffsetDateTime.parse(dateAsString).isAfter(OffsetDateTime.now().minusSeconds(1))
    }


    private GitSHA testSha() {
        return new GitSHA(DigestUtils.sha1Hex('42'))
    }

    private GitSHA testSha1() {
        return new GitSHA(DigestUtils.sha1Hex('423'))
    }


}