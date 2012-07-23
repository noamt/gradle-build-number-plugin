package main.groovy.org._10ne.gradle.plugin

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskValidationException
import org.gradle.api.tasks.bundling.Jar
import org.gradle.process.internal.BadExitCodeException

class BuildNumberPlugin implements Plugin<Project> {

    private Project project

    void apply(Project project) {

        this.project = project
        project.extensions.create('buildNumber', BuildNumberPluginExtension)

        def buildNumberTask = project.task('buildNumber')
        buildNumberTask << {

            String scmType = project.buildNumber.scmType
            String revision

            switch (scmType) {
                case 'git':
                    revision = execInProjectRoot('git rev-parse HEAD')
                    break
                case 'svn':
                    revision = execInProjectRoot('svnversion')
                    break
                default:
                    throw new TaskValidationException('Unsupported SCM type', [new InvalidUserDataException(scmType)])
            }

            if (project.buildNumber.writeToManifest) {
                project.manifest.attributes(['SCM-Revision': revision, 'SCM-Type': scmType])
            }

            if (project.buildNumber.output) {
                project.logger.lifecycle("SCM type: ${scmType} with revision: ${revision}")
            }
        }
        project.tasks.withType(Jar) {
            it.dependsOn(buildNumberTask.path)
        }
    }

    private String execInProjectRoot(String toExec) {
        Process process = toExec.execute(null, project.rootDir)
        String processOutput = process.text
        if (process.exitValue() != 0) {
            throw new BadExitCodeException("Unexpected exit value '${process.exitValue()}' for the command '${toExec}': ${processOutput}")
        }
        processOutput.trim()
    }
}
