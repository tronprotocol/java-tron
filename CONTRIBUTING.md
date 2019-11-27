# Contributing to java-tron

java-tron is an open source project.

It is the work of contributors. We appreciate your help!

Here are instructions to get you started. They are not perfect, so
please let us know if anything feels wrong or incomplete.

## Contribution guidelines

### Pull requests

First of all, java-tron follows GitFlow, The overall flow of Gitflow is:

1. A develop branch is created from master
2. A release branch is created from develop
3. Feature branches are created from develop
4. When a feature is completed it is merged into the develop branch
5. When the release branch is done it is merged into develop and master
6. If an issue in master is detected a hotfix branch is created from master
7. Once the hotfix is complete it is merged to both develop and master


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
