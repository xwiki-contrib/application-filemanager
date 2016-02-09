# File Manager

XWiki application to manage a hierarchy of folders and files. It embeds viewers for many known file types, such as office and PDF. See the [documentation](http://extensions.xwiki.org/xwiki/bin/view/Extension/File+Manager+Application) for more information.

* Extension Page: http://extensions.xwiki.org/xwiki/bin/view/Extension/File+Manager+Application.
* Bug Tracker: http://jira.xwiki.org/browse/FILEMAN.
* License: LGPL 2.1+.

## Status
[![Build Status](http://ci.xwiki.org/buildStatus/icon?job=Contrib%20-%20File%20Manager%20Application)](http://ci.xwiki.org/job/Contrib%20-%20File%20Manager%20Application/)

## Release Steps

We cannot release (perform) the API and UI modules at the same time because the API must use Java 6 in order to work with older versions of XWiki while the UI module requires at build time some recent XWiki tools that have been released with Java 7. As a consequene the release steps are:

    ## Update the translations.

    ## Prepare the tag for the new version.
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:prepare -DautoVersionSubmodules -Papi,ui

    ## Backup the release properties because we release (perform) the API and UI separately.
    cp pom.xml.releaseBackup pom.xml.releaseBackup.bak
    cp release.properties release.properties.bak

    ## Select Java 6 because the API needs to work on older versions of XWiki.
    sudo update-alternatives --config java
    sudo update-alternatives --config javac

    ## Perform the release for the API
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:perform -Papi

    ## Restore the release properties because they have been deleted by the previous step.
    mv release.properties.bak release.properties
    mv pom.xml.releaseBackup.bak pom.xml.releaseBackup

    ## Select Java 7 because the UI requires at build time some recent XWiki tools.
    sudo update-alternatives --config java
    sudo update-alternatives --config javac

    ## Perform the release for the UI (skipping the Java 6 enforcer)
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:perform -Pui -Darguments="-Dxwiki.enforcer.skip"
