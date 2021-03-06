package uk.q3c.kaytee.plugin

import org.gradle.api.Project
import uk.q3c.build.gitplus.GitPlusFactory
import uk.q3c.build.gitplus.gitplus.GitPlus
/**
 * Created by David Sowerby on 24 May 2017
 */
class DelegateWithGitPlus extends DelegateWithConfig {

    GitPlus gitPlus


    DelegateWithGitPlus(Project project) {
        super(project)
        gitPlus = GitPlusFactory.instance
        gitPlus.propertiesFromGradle()

    }

    DelegateWithGitPlus(Project project, GitPlus gitPlus) {
        super(project)
        this.gitPlus = gitPlus
        gitPlus.propertiesFromGradle()
    }

    @Override
    protected void prepare() {
        super.prepare()
        project.logger.debug("configuring GitPlus for ${this.getClass().getName()}")
        gitPlus.local.configuration.copyFrom(config.gitLocalConfiguration)
        gitPlus.wikiLocal.configuration.copyFrom(config.wikiLocalConfiguration)
        gitPlus.remote.configuration.copyFrom(config.gitRemoteConfiguration)
        gitPlus.execute()

    }

}
