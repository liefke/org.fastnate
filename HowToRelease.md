How to create a new release
---------------------------

* Update changelog.md (and commit)
* Prepare release:
		
		mvn -Psign release:clean release:prepare -DreleaseVersion=... -DdevelopmentVersion=...
		
* Perform the release:
		
		mvn -Psign release:perform
		
* Create new milestone in Github
* Move open tickets to new milestone
* Close milestone
* Checkout release commit
* Push to OSSRH:

		mvn -Psign -Possrh-release clean deploy
		
* Deploy site:

		mvn site-deploy
		
* Checkout latest commit
* Adjust website and wiki to new version
* Upload website