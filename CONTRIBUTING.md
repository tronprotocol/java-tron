# Contributing to java-tron

java-tron is an open source project.

It is the work of contributors. We appreciate your help!

Here are instructions to get started. They are not perfect,
please let us know if you see any improvements, thanks. 

## Contribution guidelines
First of all, java-tron follows GitFlow, the branches description in the java-tron project are listed as follow:

- ``master`` branch:
This branch contains the latest code released to the production environment. It can only be merged, and can not be modified directly in this branch.

- ``develop`` branch:
This branch is the main development branch. It contains the complete code that is going to be released. It can only be merged, and can not be modified directly in this branch.

- ``feature`` branch:
This branch is used to develop new features. It is created based on ``develop`` branch. Once the development is finished, it should be merged into ``develop`` branch, and then delete the branch.

- ``release`` branch:
This is the branch that is going to be released. It is created based on ``develop`` branch. In this branch, small fix and modification of final version of metadata is allowed. When the code is released, this branch should be merged into ``master`` branch (tag needed) and ``develop`` branch. The final test before release uses this branch.

- ``hotfix`` branch:
This branch is used to fix a bug when an online bug is found. It is created based on ``master`` branch. When bug fix is done, it should be merged into ``master`` branch(as a new release) and ``develop`` and then delete the branch. branch.

## Pull requests

If you'd like to contribute to java-tron, you should follow the steps below:
- **Fork** a repository from **tronprotocol/java-tron** and working there without affecting the original project.
- **Finish** your code modification and **Commit** your changes to your own repository.
- **Submit** a Pull Request（PR）from your own repository to **tronprotocol/java-tron**. 
  *notice*：When you create a new PR，please choose the **tronprotocol/java-tron** as the base repository and choose **your fork/java-tron** as the head repository.
  And you must choose **develop** as the base repository branch, which means we will merge the PR into our **develop** branch after reviewed and approved.
  Additionally, if you are writing a new feature, please ensure you add appropriate test cases under ``/src/test``.

The Sonar check and Travis CI continuous-integration check will be triggerred automatically after PR submitted, once all check passed, **java-tron** maintainers will review the PR and give feedback for modifying if necessary. Once approved, we will close the PR and merge it into the `develop` branch.


We are always happy to receive pull requests, and do our best to review them as fast as possible. Not sure if A typo is worth a pull request? Do it! We would appreciate it.

If your pull request is not accepted on the first attempt, please don’t get discouraged, as this may be an oversight. Please explain your code as much as possible to make it easier for us to understand.

Please make sure your commit follows below coding guidelines:

- Code must be conformed to the [Google Style](https://google.github.io/styleguide/javaguide.html)
- Code must pass Sonar detection.
- Pull requests must be based on the `develop` branch.
- Commit messages should be started with verb, and the first letter should be a lowercase.The length of commit message
must be less than 50 words.

## Create issues

Any significant improvement must be documented in [a GitHub issue](https://github.com/tronprotocol/java-tron/issues)  before working on it.

When start an issue, make sure to answer these three questions:

- What did you do?
- What did you expect to see?
- What did you see instead?

## Please check existing issues and docs first!

Please take a moment to check that your bug report or improvement proposal doesn't already exist. If it does, please add a quick "+1" or "I have this problem too". 
This will help prioritize the most common problems and requests.

## Community Developers Incentives Programme(Paused)

Bonus point applies in TRON incentives programme. Developers can earn points by contributing to TRON.

The Top 5 scored developers (for every month, quarter and year) can win a cash reward.

For more details, please visit [Incentives Policy](https://tronprotocol.github.io/documentation-en/developers/incentives/).
