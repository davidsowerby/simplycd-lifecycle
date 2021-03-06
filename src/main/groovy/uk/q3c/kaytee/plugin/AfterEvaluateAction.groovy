package uk.q3c.kaytee.plugin

import com.jfrog.bintray.gradle.BintrayExtension
import org.apache.commons.lang.StringUtils
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.unbrokendome.gradle.plugins.testsets.internal.DefaultTestSet
import org.unbrokendome.gradle.plugins.testsets.internal.DefaultTestSetContainer
import uk.q3c.build.gitplus.remote.DefaultGitRemoteUrlMapper
import uk.q3c.util.version.Scheme
import uk.q3c.util.version.VersionNumber
import uk.q3c.util.version.VersionNumberKt

/**
 * Created by David Sowerby on 24 Dec 2016
 */
class AfterEvaluateAction implements Action<Project> {

    final Project project

    AfterEvaluateAction(Project project) {
        this.project = project
    }

    @Override
    void execute(Project project) {
        project.getLogger().debug('Kaytee after evaluate')
        KayTeeExtension config = confirmConfiguration()
        createTestSets(config)
        config.validate() // will throw exception if invalid

        ExtraPropertiesExtension ext = project.getExtensions().getExtraProperties()
        ext.set(KayTeePlugin.KAYTEE_CONFIG_FLAG, true)
        bintrayConfig(config)
    }

    /**
     * Set any required config that has not already been set, or fail.  See https://github.com/davidsowerby/kaytee-plugin/issues/19
     */
    private KayTeeExtension confirmConfiguration() {
        Logger logger = project.getLogger()
        logger.lifecycle("confirming configuration")
        KayTeeExtension config = project.extensions.getByName("kaytee") as KayTeeExtension

        config.wikiLocalConfiguration.active = true
        config.wikiLocalConfiguration.cloneFromRemote = false
        config.wikiLocalConfiguration.projectName = project.name

        config.gitRemoteConfiguration.repoName = project.name
        config.gitRemoteConfiguration.repoUser = config.remoteRepoUserName

        config.gitLocalConfiguration.cloneFromRemote = false

        config.changelog.projectName = project.name
        config.changelog.projectDirParent = project.projectDir.parentFile

        config.changelog.remoteRepoUser = config.remoteRepoUserName
        config.changelog.maxVersions = 10



        config.gitRemoteConfiguration.validate(config.gitLocalConfiguration)
        config.gitLocalConfiguration.validate(config.gitRemoteConfiguration)
        config.wikiLocalConfiguration.validate(config.gitRemoteConfiguration)
        config.changelog.validate()


        VersionNumber versionNumber = versionFromConfig()
        project.version = versionNumber
        config.changelog.autoTagLatestCommit = true
        config.changelog.currentBuildTagName = versionNumber.toString()

        logConfig(project)
        return config
    }

    private VersionNumber versionFromConfig() {
        KayTeeExtension config = project.extensions.getByName("kaytee") as KayTeeExtension
        String version = config.version.number
        String qualifier = config.version.qualifier
        String buildMetaData = config.version.buildMetaData
        Scheme scheme = config.version.scheme
        return VersionNumberKt.parseVersion(version, qualifier, buildMetaData, scheme)
    }


    private void createTestSets(KayTeeExtension config) {
        // create the other test sets
        logDebug("Creating test sets for enabled test groups")
        DefaultTestSetContainer container = project.testSets
        for (TaskKey ts : TaskKey.testTasks) {
            if (TaskKey.Unit_Test != ts) {
                logDebug("TaskKey is $ts, adding test set for ${ts.gradleTask()}")
                if (config.testConfig(ts).enabled) {
                    container.add(new DefaultTestSet(ts.gradleTask()))
                }
            }
        }
    }

    private void logDebug(String msg) {
        project.logger.debug(msg)
    }

    private void logConfig(Project project) {
        Logger logger = project.getLogger()
        if (logger.isDebugEnabled()) {
            new ConfigWriter().writeOutConfig(project, "afterEvaluateActionConfig.json")
            logger.debug("Configuration settings on completion of AfterEvaluateAction exported to 'afterEvaluateActionConfig.json' ")
        }
    }

    /**
     * We could make this simpler by declaring these at the start of the {@link KayTeePlugin}, but if we did that, the order in which the KayTee and Bintray
     * configurations are declared becomes important.  This way, it does not matter
     *
     * @param config
     */
    void bintrayConfig(KayTeeExtension config) {
        project.logger.debug("Checking existing bintray config values, and replacing nulls where possible")

        DefaultGitRemoteUrlMapper mapper = new DefaultGitRemoteUrlMapper()
        mapper.owner = config.gitRemoteConfiguration
        BintrayExtension bintray = project.extensions.getByName("bintray") as BintrayExtension

        if (StringUtils.isEmpty(bintray.pkg.name)) {
            bintray.pkg.name = config.gitLocalConfiguration.projectName
        }
        if (StringUtils.isEmpty(bintray.pkg.repo)) {
            bintray.pkg.repo = 'maven'
        }
        if (StringUtils.isEmpty(bintray.pkg.githubRepo)) {
            bintray.pkg.githubRepo = config.gitRemoteConfiguration.remoteRepoFullName()
        }

        if (StringUtils.isEmpty(bintray.pkg.websiteUrl)) {
            bintray.pkg.websiteUrl = mapper.repoBaselUrl()
        }
        if (StringUtils.isEmpty(bintray.pkg.issueTrackerUrl)) {
            bintray.pkg.issueTrackerUrl = mapper.issuesUrl()
        }
        if (StringUtils.isEmpty(bintray.pkg.vcsUrl)) {
            bintray.pkg.vcsUrl = mapper.cloneUrl()
        }
        if (StringUtils.isEmpty(bintray.key)) {
            try {
                bintray.key = project.bintrayKey
            } catch (Exception exception) {
                bintray.key = "not a real key"
                project.logger.warn("No 'bintrayKey property' is available.  The Bintray plugin has been applied and a mock (and therefore invalid) key provided to allow the build to continue, but the bintrayUpload task would fail if called. This will not affect the build unless you wanted to push the artifacts to Bintray ")
            }
        }
        bintray.pkg.version.name = project.version.toString()

        if (StringUtils.isEmpty(bintray.pkg.version.released)) {
            bintray.pkg.version.released = new Date()
        }

        if (StringUtils.isEmpty(bintray.pkg.version.vcsTag)) {
            bintray.pkg.version.vcsTag = project.version.toString()
        }

        if (bintray.pkg.licenses == null || bintray.pkg.licenses.length == 0) {
            bintray.pkg.setLicenses('Apache-2.0')
        }

        if (project.logger.isDebugEnabled()) {
            BintrayConfigWrapper wrapper = new BintrayConfigWrapper(bintray)
            project.logger.debug("Bintray config is:\n")
            project.logger.debug(wrapper.toString())
            project.logger.debug("project version is: " + project.version.toString())
        }
    }

}
