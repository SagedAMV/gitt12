pluginManagement { repositories { maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public"); maven("https://repo.huaweicloud.com/repository/maven"); google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public"); maven("https://repo.huaweicloud.com/repository/maven"); google(); mavenCentral() } }
rootProject.name = "GitHubVault"
include(":app")
