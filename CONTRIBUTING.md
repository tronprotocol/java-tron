# Contributing to java-tron

java-tron is an open source project.

It is the work of contributors. We appreciate your help!

Here are instructions to get you started. They are not perfect, so
please let us know if anything feels wrong or incomplete.

## Contribution guidelines

``master`` branch:  
This branch contains the latest code released to the production environment. It can only be merged, and can not be modified directly in this branch.

``develop`` branch:  
This branch is the main development branch. It contains the complete code that is going to release. It can only be merged, and can not be modified directly in this branch.

``feature`` branch:  
This branch is used to develop new features. It is created based on ``develop`` branch. Once the development is finished, it should be merged into ``develop`` branch, and then delete the branch.

``release`` branch:  
This is the branch that is going to be released. It is created based on ``develop`` branch. In this branch, small fix and modification of final version of metadata is allowed. When the code is released, this branch should be merged into ``master`` branch(tag needed) and ``develop`` branch. The final test before release uses this branch.

``hotfix`` branch:  
This branch is used to fix a bug when an online bug is found. It is created based on ``master`` branch. When bug fix is done, it should be merged into ``master`` branch(as a new release) and ``develop`` and then delete the branch. branch.

### Pull requests


If you'd like to contribute to java-tron, please fork a repository from tronprotocol/java-tron,
fix, commit, and send a pull request for the maintainers to review and merge into the main code base. 

Please open pull requests(PR) to the **develop** branch. After the PR is valided by our Sonar check or Travis CI check, 
we maintainers will review the code changed and give some advices for modifying if necessary.Once approved, 
we will close the PR and merge into the protocol/java-tron's develop branch.

We are always happy to receive pull requests, and do our best to
review them as fast as possible. Not sure if that typo is worth a pull
request? Do it! We would appreciate it.

If your pull request is not accepted on the first try, don't be
discouraged as it can be a possible oversight. Please explain your code as
detailed as possible to make it easier for us to understand.

Please make sure your contributions adhere to our coding guidelines:

- Code must be documented adhering to the [Google Style](https://google.github.io/styleguide/javaguide.html)
- Code must pass Sonar detection.
- Pull requests need to be based on and opened against the develop branch.
- Commit messages should be started with verb, and the first letter should be a lowercase.The length of commit message
must be limited in 50 words.
### Create issues

Any significant improvement should be documented as [a GitHub
issue](https://github.com/tronprotocol/java-tron/issues) before anyone
starts working on it.

When filing an issue, make sure to answer these three questions:

- What did you do?
- What did you expect to see?
- What did you see instead?

### Please check existing issues and docs first!

Please take a moment to check that your bug report or improvement proposal
doesn't already exist. If it does, please add a quick "+1" or "I have this problem too".
This will help prioritize the most common problems and requests.
