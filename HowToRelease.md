How to create a new release
---------------------------

* Update changelog.md (and commit)
* Prepare release
		
		mvn -Psign release:clean release:prepare -DreleaseVersion=... -DdevelopmentVersion=...-SNAPSHOT

* (Remark: the release plugin not work with the Maven installation embedded in Eclipse)
* Perform the release:
		
		mvn -Psign release:perform -DlocalCheckout=true
		
* Create new milestone in Github
* Move open tickets to new milestone
* Close milestone
* Checkout release commit
* Push to maven-central (configure the settings for ossrh before):

		mvn -Psign -Possrh-release clean deploy
		
* Deploy site:

		mvn site-deploy
		
* Checkout latest commit
* Adjust website and wiki to new version
* Upload website