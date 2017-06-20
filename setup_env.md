 ### qTest Jenkins plugin
 
 #### Setup to push release to jenkins repository:
- Add to `settings.xml` in `~/.m2` folder
  - Copy content from [settins.jenkins.xml](#settings.jenkins.xml) to `~/m2/settings.xml`
  - Update password for server section configured, follow the link: [http://maven.apache.org/guides/mini/guide-encryption.html](http://maven.apache.org/guides/mini/guide-encryption.html)	
- Make release:
  - Login to jenkins account page: [https://accounts.jenkins.io](https://accounts.jenkins.io), then add ssh key.
  - Run `mvn -Dresume=false release:prepare release:perform`
	
- Rollback a release: `mvn release:rollback`
 
- Clean a release: `mvn release:clean`
- Repo store hpi versions: [here](http://repo.jenkins-ci.org/releases/com/qasymphony/ci/jenkins/qtest/)
- Cloudbees: [here](https://jenkins.ci.cloudbees.com/job/plugins/job/qtest-plugin/)
- See more: [here](https://wiki.jenkins-ci.org/display/JENKINS/Hosting+Plugins)