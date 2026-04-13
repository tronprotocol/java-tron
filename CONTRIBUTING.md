# Contributing to java-tron

java-tron is an open-source project which needs the support of open-source contributors.

Below are the instructions. We understand that there is much left to be desired, and if you see any room for improvement, please let us know. Thank you.

Here are some guidelines to get started quickly and easily:
- [Reporting An Issue](#Reporting-An-Issue)
- [Working on java-tron](#Working-on-java-tron)
  - [Key Branches](#Key-Branches)
  - [Submitting Code](#Submitting-Code)
- [Code Review Guidelines](#Code-Review-Guidelines)
  - [Terminology](#Terminology)
  - [The Process](#The-Process)
  - [Code Style](#Code-Style)
  - [Commit Messages](#Commit-Messages)
  - [Branch Naming Conventions](#Branch-Naming-Conventions)
  - [Pull Request Guidelines](#Pull-Request-Guidelines)
    - [PR Title Format](#PR-Title-Format)
    - [Type and Scope Reference](#Type-and-Scope-Reference)
    - [PR Description](#PR-Description)
  - [Special Situations And How To Deal With Them](#Special-Situations-And-How-To-Deal-With-Them)
- [Conduct](#Conduct)


## Reporting An Issue

If you have any question about java-tron, please search [existing issues](https://github.com/tronprotocol/java-tron/issues?q=is%3Aissue%20state%3Aclosed%20OR%20state%3Aopen) first to avoid duplicates. Your questions might already be under discussion or part of our roadmap. Checking first helps us streamline efforts and focus on new contributions.

### Ask a question
Feel free to ask any java-tron related question to solve your doubt. Please click **Ask a question** in GitHub Issues, using [Ask a question](.github/ISSUE_TEMPLATE/ask-a-question.md) template.

### Report a bug

If you think you've found a bug with java-tron, please click **Report a bug** in GitHub Issues, using [Report a bug](.github/ISSUE_TEMPLATE/report-a-bug.md) template.

### Request a feature

If you have any good feature suggestions for java-tron, please click **Request a feature** in GitHub Issues, using [Request a feature](.github/ISSUE_TEMPLATE/request-a-feature.md) template.


## Working on java-tron
Thank you for considering to help out with the source code! We welcome contributions from anyone on the internet, and are grateful for even the smallest of fixes!

If you’d like to contribute to java-tron, for small fixes, we recommend that you send a pull request (PR) for the maintainers to review and merge into the main code base, make sure the PR contains a detailed description. For more complex changes, you need to submit an issue to the TIP repository to detail your motive and implementation plan, etc. For how to submit a TIP issue, please refer to [TIP Specification](https://github.com/tronprotocol/tips#to-submit-a-tip).


As the author of TIP issue, you are expected to encourage developers to discuss this issue, flesh out your issue by collecting their feedback, and eventually put your issue into practice.


### Key Branches
java-tron only has `master`, `develop`, `release-*`, `feature-*`, and `hotfix-*` branches, which are described below:

- ``develop`` branch  
  The `develop` branch only accept merge request from other forked branches or`release_*` branches. It is not allowed to directly push changes to the `develop` branch. A `release_*` branch has to be pulled from the develop branch when a new build is to be released.

- ``master`` branch  
  `release_*` branches and `hotfix/*` branches should only be merged into the `master` branch when a new build is released.

- ``release`` branch  
  `release_*` is a branch pulled from the `develop` branch for release. It should be merged into `master` after a regression test and will be permanently kept in the repository. If a bug is identified in a `release_*` branch, its fixes should be directly merged into the branch. After passing the regression test, the `release_*` branch should be merged back into the `develop` branch. Essentially, a `release_*` branch serves as a snapshot for each release.

- ``feature`` branch  
  `feature/*` is an important feature branch pulled from the `develop` branch. After the `feature/*` branch is code-complete, it should be merged back to the `develop` branch. The `feature/*` branch is maintainable.

- ``hotfix`` branch  
  It is pulled from the `master` branch and should be merged back into the master branch and the `develop` branch. Only pull requests of the fork repository (pull requests for bug fixes) should be merged into the `hotfix/` branch. `hotfix/` branches are used only for fixing bugs found after release.


### Submitting Code

If you want to contribute code to java-tron, please follow the following steps.

* Fork the Repository

  Visit [tronprotocol/java-tron](https://github.com/tronprotocol/java-tron/) and click **Fork** to create a fork repository under your GitHub account.

* Setup Local Environment

  Clone your fork repository to local and add the official repository as **upstream**.
    ```
    git clone https://github.com/yourname/java-tron.git

    cd java-tron

    git remote add upstream https://github.com/tronprotocol/java-tron.git
    ```

* Synchronize and Develop

  Before developing new features, please synchronize your local `develop` branch with the upstream repository and update to your fork repository.
    ```
    git fetch upstream
    git checkout develop
    # `--no-ff` means to turn off the default fast merge mode
    git merge upstream/develop --no-ff
    git push origin develop
    ```

  Create a new branch for development. Please refer to [Branch Naming Conventions](#Branch-Naming-Conventions).
    ```
    git checkout -b feature/branch_name develop
    ```

* Commit and Push

  Write and commit the new code when it is completed. Please refer to [Commit Messages](#Commit-Messages).
     ```
     git add .
     git commit -m 'commit message'
     ```

  Push the new branch to your fork repository
     ```
     git push origin feature/branch_name
     ```

* Submit a pull request

  Submit a pull request (PR) from your fork repository to `tronprotocol/java-tron`.
  Please be sure to click on the link in the red box shown below. Select the base branch for `tronprotocol/java-tron` and the compare branch for your fork repository.
  ![image](https://raw.githubusercontent.com/tronprotocol/documentation-en/master/images/javatron_pr.png)



## Code Review Guidelines
The only way to get code into java-tron is to send a pull request. Those pull requests need to be reviewed by someone. The following guide explains our expectations around PRs for both authors and reviewers.

### Terminology
- The author of a pull request is the entity who wrote the diff and submitted it to GitHub.
- The team consists of people with commit rights on the java-tron repository.
- The reviewer is the person assigned to review the diff. The reviewer must be a team member.
- The code owner is the person responsible for the subsystem being modified by the PR.

### The Process
The first decision to make for any PR is whether it’s worth including at all. This decision lies primarily with the code owner, but may be negotiated with team members.

To make the decision we must understand what the PR is about. If there isn’t enough description content or the diff is too large, request an explanation. Anyone can do this part.

We expect that reviewers check the style and functionality of the PR, providing comments to the author using the GitHub review system. Reviewers should follow up with the PR until it is in good shape, then approve the PR. Approved PRs can be merged by any code owner.

When communicating with authors, be polite and respectful.

### Code Style
We would like all developers to follow a standard development flow and coding style. Therefore, we suggest the following:
1. Review the code with coding style checkers.
2. Review the code before submission.
3. Run standardized tests.

`Sonar`-scanner and CI checks (GitHub Actions) will be automatically triggered when a pull request has been submitted. When a PR passes all the checks, the **java-tron** maintainers will then review the PR and offer feedback and modifications when necessary.  Once adopted, the PR will be closed and merged into the `develop` branch.

We are glad to receive your pull requests and will try our best to review them as soon as we can. Any pull request is welcome, even if it is for a typo.

Please kindly address the issue you find. We would appreciate your contribution.

Please do not be discouraged if your pull request is not accepted, as it may be an oversight. Please explain your code as detailed as possible to make it easier to understand.

Please make sure your submission meets the following code style:

- The code must conform to [Google Code Style](https://google.github.io/styleguide/javaguide.html).
- The code must have passed the Sonar scanner test.
- The code has to be pulled from the `develop` branch.
- The commit message should start with a verb, whose initial should not be capitalized.
- The commit message title should be between 10 and 72 characters in length.



### Commit Messages

Commit messages should follow the rule below, we provide a template with corresponding instructions.

Template:
```
<commit type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

The message header is a single line that contains succinct description of the change containing a `commit type`, an optional `scope` and a subject.

`commit type` describes the kind of change that this commit is providing:
* feat     (new feature)
* fix      (bug fix)
* docs     (changes to documentation)
* style    (formatting, missing semi colons, etc. no code change)
* refactor (refactoring production code)
* test     (adding or refactoring tests. no production code change)
* chore    (updating grunt tasks etc. no production code change)
* ci       (CI/CD configuration)
* perf     (performance improvement)
* build    (build system changes)
* revert   (reverting a previous commit)

The `scope` can be anything specifying place of the commit change. For example: `framework`, `api`, `tvm`, `db`, `net`. For a full list of scopes, see [Type and Scope Reference](#type-and-scope-reference). You can use `*` if there isn't a more fitting scope.

The subject contains a succinct description of the change:
1. Limit the subject line, which briefly describes the purpose of the commit, to 72 characters (minimum 10).
2. Start with a verb and use first-person present-tense (e.g., use "change" instead of "changed" or "changes").
3. Do not capitalize the first letter.
4. Do not end the subject line with a period.
5. Avoid meaningless commits. It is recommended to use the git rebase command.

Message body uses the imperative, present tense: "change" not "changed" nor "changes". The body should include the motivation for the change and contrast this with previous behavior.

Here is an example:
```
feat(block): optimize the block-producing logic

1. increase the priority that block producing thread acquires synchronization lock
2. add the interruption exception handling in block-producing thread

Closes #1234
```
If the purpose of this submission is to modify one issue, you need to refer to the issue in the footer, starting with the keyword Closes, such as `Closes #1234`,if multiple bugs have been modified, separate them with commas,such as `Closes #123, #245, #992`.



### Branch Naming Conventions
1. Always name the `master` branch and `develop` branch as "master" and "develop".
2. Name the `release_*` branch using version numbers, which are assigned by the project lead (e.g., Odyssey-v3.1.3, 3.1.3, etc.).
3. Use `hotfix/` as the prefix of the `hotfix` branch, briefly describe the bug in the name, and connect words with underline (e.g., hotfix/typo, hotfix/null_point_exception, etc.).
4. Use `feature/` as the prefix of the `feature` branch, briefly describe the feature in the name, and connect words with underline (e.g., feature/new_resource_model, etc.).
### Pull Request Guidelines

#### PR Title Format

PR titles must follow the conventional commit format and will be checked by CI:

```
type(scope): description
```

| Rule | Requirement |
|------|-------------|
| Format | `type: description` or `type(scope): description` |
| Length | 10 ~ 72 characters |
| Type must be one of | `feat` `fix` `refactor` `docs` `style` `test` `chore` `ci` `perf` `build` `revert` |

#### Type and Scope Reference

**Type Reference**

| Type | Purpose | Example |
|------|---------|---------|
| `feat` | New feature | `feat(tvm): add blob opcodes` |
| `fix` | Bug fix | `fix(db): improve resource management` |
| `docs` | Documentation only | `docs: fix formatting issues in README` |
| `style` | Code style (no logic change) | `style: fix import order and line length` |
| `refactor` | Code refactoring (no behavior change) | `refactor(config): simplify parameters` |
| `test` | Adding or updating tests | `test(vm): add unit tests for opcodes` |
| `chore` | Build tooling, dependencies, etc. | `chore(version): bump to v4.7.8` |
| `ci` | CI/CD configuration | `ci: add PR check workflow` |
| `perf` | Performance improvement | `perf(trie): optimize query performance` |
| `build` | Build system changes | `build: add aarch64 support for RocksDB` |
| `revert` | Reverting a previous commit | `revert: restore ApiUtilTest.java` |

**Module Scopes**

| Scope | Description |
|-------|-------------|
| `framework` | Core framework, services, APIs, RPC interfaces |
| `chainbase` | Blockchain storage, state management, database layer |
| `actuator` | Transaction execution engine, smart contract operations |
| `consensus` | Consensus mechanism (DPoS, PBFT) |
| `common` | Common utilities, configuration, shared infrastructure |
| `crypto` | Cryptographic functions, key management, signatures |
| `plugins` | Node tools (Toolkit, ArchiveManifest, database plugins) |
| `protocol` | Protocol definitions, protobuf messages, gRPC contracts |

**Functional Domain Scopes**

| Scope | Description | Example |
|-------|-------------|---------|
| `net` | P2P networking, message handling, peer sync | `feat(net): optimize sync logic` |
| `db` | Database operations, queries, persistence | `fix(db): handle null pointer in query` |
| `vm` / `tvm` | Virtual machine, bytecode execution, EIP impl | `feat(tvm): implement eip-7823` |
| `api` | HTTP/gRPC API endpoints | `fix(api): handle null response` |
| `jsonrpc` | JSON-RPC interface (Ethereum-compatible) | `fix(jsonrpc): support blockHash param` |
| `rpc` | gRPC services and methods | `fix(rpc): handle timeout correctly` |
| `http` | HTTP server and endpoints | `feat(http): add new endpoint` |
| `event` | Event logging and event service | `feat(event): optimize concurrent writes` |
| `config` | Configuration management, feature flags | `refactor(config): simplify parameters` |
| `block` | Block processing, validation, structure | `fix(block): validate block header` |
| `proposal` | On-chain governance proposals | `feat(proposal): add Osaka proposal` |
| `trie` | Merkle tree, state trie operations | `perf(trie): optimize tree query` |
| `log` | Application logging | `refactor(log): reduce noise` |
| `metrics` | Performance monitoring, Prometheus | `feat(metrics): add Prometheus support` |
| `test` | Test infrastructure and utilities | `test(proposal): add unit test cases` |
| `docker` | Docker containerization and deployment | `feat(docker): add ARM64 support` |
| `version` | Version and release management | `chore(version): bump to v4.7.8` |

**Feature Scopes**

| Scope | Description |
|-------|-------------|
| `freezeV2` | Resource delegation / freeze-unfreeze V2 mechanism |
| `DynamicEnergy` | Dynamic energy pricing mechanism |
| `stable-coin` | Stable coin features and operations |
| `reward` | Block producer rewards distribution |
| `lite` | Lite fullnode functionality |
| `toolkit` | Node maintenance tools (Toolkit.jar) |

#### PR Description

- PR description must not be empty, minimum **20 characters**.
- Should explain **what** the PR does and **why**.

#### General Rules

1. Create one PR for one issue.
2. Avoid massive PRs.
3. Elaborate on the feedback you need (if any).
4. Do not capitalize the first letter of the description.
5. Do not put a period (.) at the end of the title.





### Special Situations And How To Deal With Them
As a reviewer, you may find yourself in one of the situations below. Here’s how to deal with those:

The author doesn’t follow up: ping them after a while (i.e. after a few days). If there is no further response, close the PR or complete the work yourself.

Author insists on including refactoring changes alongside bug fix: We can tolerate small refactorings alongside any change. If you feel lost in the diff, ask the author to submit the refactoring as an independent PR, or at least as an independent commit in the same PR.

Author keeps rejecting your feedback: reviewers have authority to reject any change for technical reasons. If you’re unsure, ask the team for a second opinion. You may close the PR if no consensus can be reached.

## Conduct
While contributing, please be respectful and constructive, so that participation in our project is a positive experience for everyone.

Examples of behavior that contributes to creating a positive environment include:

- Using welcoming and inclusive language
  Being respectful of differing viewpoints and experiences
- Gracefully accepting constructive criticism
- Focusing on what is best for the community
- Showing empathy towards other community members

Examples of unacceptable behavior include:

- The use of sexualized language or imagery and unwelcome sexual attention or advances
- Trolling, insulting/derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others’ private information, such as a physical or electronic address, without explicit permission
- Other conduct which could reasonably be considered inappropriate in a professional setting
