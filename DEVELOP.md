# Jenkins Line Notify Plugin

## how to run local

### Unix

```sh
export MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n"
mvn hpi:run
```

### Windows

```bat
set MAVEN_OPTS=-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n
mvn hpi:run
```

## More document

[Jenkins plugin development guideline](https://wiki.jenkins.io/pages/viewpage.action?pageId=67567923#Plugintutorial-SettingUpEnvironment)
