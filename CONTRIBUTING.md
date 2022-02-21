# java-tron Community Contribution

java-tron is an open-source project which needs the support of open-source contributors. We are grateful for your help!

Below are the instructions. We understand that there is much left to be desired, and if you see any room for improvement, please let us know. Thank you.

## How to Participate
1. **Issue submission**  
   If you have any suggestions or ideas to improve the java-tron protocol, you can submit an issue to the java-tron repository. If your idea is well-thought-out, you can submit an issue to the TIP repository to detail your motive and implementation plan, etc.

2. **Community discussion**  
   As the author of this issue, you are expected to encourage developers to discuss this issue, flesh out your issue by collecting their feedback, and eventually put your issue into practice.

3. **Code submission**  
   After community discussion, you can follow the development specs to submit your codes when your issue is fully mature.

## Contribution Guide
We hope that more developers can engage in the community discussion to contribute more ideas. We now provide a development guide to avoid developers getting into any trouble.

1. Participate in the community discussion
* [java-tron issues](#how-to-submit-java-tron-issue)
* [TIP issues](#how-to-submit-TIP-issue)

2. Development guide
* [Git Flow](#Git-flow)
* [Coding Style](#coding-style)


## Participate in the Community Discussion
`java-tron` is a global developer community where the discussions are carried out based on open questions.

If you encounter any problem or bug while using java-tron, you can submit an issue to the java-tron repository.If you have any suggestions for improving java-tron, you can submit an issue to the TIP repository.


You can start coding when the community comes up with an agreed solution after a full discussion of the issue. For the development specs, please refer to: [Development Specs](#Development-Specs)


### How to submit a java-tron issue

When you submit a issue, please make sure you answer the three questions below:

- What have you done?
- What results do you expect to see?
- What problems do you see?

### How to submit a TIP issue

For how to submit a TIP issue, please refer to [Specification](https://github.com/tronprotocol/tips#to-submit-a-tip)


## Development Specs

### Git flow
If you want to contribute codes to java-tron, please follow the following steps:

* Fork code repository
  Fork a new repository from tronprotocol/java-tron to your personal code repository

* Edit the code in the fork repository
    ```
    git clone https://github.com/yourname/java-tron.git

    git remote add upstream https://github.com/tronprotocol/java-tron.git     ("upstream" refers to upstream projects repositories, namely tronprotocol's repositories, and can be named as you like it. We usually call it "upstream" for convenience) 
    ```
  Before developing new features, please synchronize your fork repository with the upstream repository.
    ```
    git fetch upstream 
    git checkout develop 
    git merge upstream/develop --no-ff (Add --no-ff to turn off the default fast merge mode)
    ```

  Pull a new branch from the develop branch of your repository for local development. Please refer to [Branch Naming Conventions](#Branch-Naming-Conventions),
    ```
     git checkout -b feature/branch_name develop
     ```

  Write and commit the new code when it is completed. Please refer to [Commit Conventions](#Commit-Conventions)
     ```
     git add .
     git commit -m 'commit message'
     ```
  Commit the new branch to your personal remote repository
     ```
     git push origin feature/branch_name
     ```

* Push code

  Submit a pull request (PR) from your repository to `tronprotocol/java-tron`.
  Please be sure to click on the link in the red box shown below. Select the base branch for tronprotocol and the compare branch for your personal fork repository.
  ![](https://codimd.s3.shivering-isles.com/demo/uploads/e24435ab42e4287d9369a2136.png)

#### Managing tronprotocol/java-tron repository branches

The tronprotocol/java-tron repository adopts the "fork + pull request" approach to avoid excessive branches. Its existing branches are both meaningful and maintainable.

java-tron only has master, develop, `release-*`, `feature-*`, and `hotfix-*` branches, which are described below:

- ``develop`` branch:
  The `develop` branch only accept merge request from other forked branches or`release-*` branches. It is not allowed to directly push changes to the `develop` branch. A `release-*` branch has to be pulled from the develop branch when a new build is to be released.

- ``master`` branch:
  `release-*` branches and `hotfix-*` branches should only be merged into the master branch when a new build is released.

- ``release`` branch:
  `release-*` is a branch pulled from the `develop` branch for release. It should be merged into `master` after a regression test and will be permanently kept in the repository. If a bug is identified in a `release-*` branch, its fixes should be directly merged into the branch. After passing the regression test, the `release-*` branch should be merged back into the `develop` branch. Essentially, a `release-*` branch serves as a snapshot for each release.

- ``feature`` branch:
  `feature-*` is an important feature branch pulled from the `develop` branch. After the `feature-*` branch is code-complete, it should be merged back to the `develop` branch. The `feature-*` branch is maintainable.

- ``hotfix`` branch:
  It is pulled from the `master` branch and should be merged back into the master branch and the `develop` branch. Only pull requests of the fork repository (pull requests for bug fixes) should be merged into the `hotfix` branch. `hotfix` branches are used only for fixing bugs found after release.

#### Branch Naming Conventions
1. Always name the `Master` branch and `Develop` branch as "master" and "develop".
2. Name the `release-*` branch using version numbers, which are assigned by the project lead (e.g., Odyssey-v3.1.3, 3.1.3, etc.).
3. Use `hotfix/` as the prefix of the `hotfix` branch, briefly describe the bug in the name, and connect words with hyphens (e.g., hotfix/typo, hotfix/null-point-exception, etc.).

#### Commit Conventions
1. Limit the subject line, which briefly describes the purpose of the commit, to 50 characters.
2. Start with a verb and use first-person present-tense (e.g., use "change" instead of "changed" or "changes").
3. Do not capitalize the first letter.
4. Do not end the subject line with a period.
5. Avoid meaningless commits. It is recommended to use the git rebase command.

##### commit message template
Please refer to the message template to submit：
```
feat(block): Optimized product block

1. Optimization of production block threads
2. Improve transaction entry speed

Closes #1234
```
###### commit type
* feat     (new feature)
* fix      (bug fix)
* docs     (changes to documentation)
* style    (formatting, missing semi colons, etc; no code change)
* refactor (refactoring production code)
* test     (adding or refactoring tests; no production code change)
* chore    (updating grunt tasks etc; no production code change)

###### scope
Scope refers to the features affected, and you can specify which features are affected by this commit, described according to your actual impact, but not limited to the following types:

* protobuf
* api
* test
* docs

#### Pull Request Guidelines
1. Create one PR for one issue.
2. Avoid massive PRs.
3. Write an overview of the purpose of the PR in its title.
4. Write a description of the PR for future reviewers.
5. Elaborate on the feedback you need (if any).
6. Do not capitalize the first letter.
7. Do not put a period (.) in the end.

### Coding Style
We would like all developers to follow a standard development flow and coding style. Therefore, we suggest the following:
1. Review the code with coding style checkers.
2. Review the code before submission.
3. Run standardized tests.

`Sonar`-scanner and `Travis CI` continuous integration scanner will be automatically triggered when a pull request has been submitted. When a PR passes all the checks, the **java-tron** maintainers will then review the PR and offer feedback and modifications when necessary.  Once adopted, the PR will be closed and merged into the `develop` branch.

We are glad to receive your pull requests and will try our best to review them as soon as we can. Any pull request is welcome, even if it is for a typo.
Please kindly address the issue you find. We would appreciate your contribution.

Please do not be discouraged if your pull request is not accepted, as it may be an oversight. Please explain your code as detailed as possible to make it easier to understand.

Please make sure your submission meets the following code style:

- The code must conform to [Google Code Style](https://google.github.io/styleguide/javaguide.html);
- The code must have passed the Sonar scanner test;
- The code has to be pulled from the `develop` branch;
- The commit message should start with a verb, whose initial should not be capitalized;
- The commit message should be less than 50 characters in length.



## References

* [Documentation](https://github.com/tronprotocol/documentation-en)
* [The TRON Developer Hub](https://developers.tron.network/)
