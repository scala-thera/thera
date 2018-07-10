---
layout: post
title: How to split monolithic commit history in git
categories:
- blog
---
# Problem
Suppose you have a fork of some GitHub repository. You have made some changes in it that fix multiple bugs and add some new features. Now you want to send your work to the main repository.

If you send your work as a single pull request, it may be hard to process for the core team, since it spans multiple issues and multiple features are added. Moreover, the core team may have its own opinions regarding which features are needed and which not; they may want to partially accept your work.

It is better to split the work into multiple pull requests. But what to do if your commits are highly monolithic? That is, commits for issue A, followed by commits for issue B, then some commits for A again, and then some for B again?

How to restructure your work, so that it can be sent in separate, modular pull requests?

# Solution
The idea is to make several branches, each one containing commits related to a single particular issue. Moreover, since the commits may be monolithic, we will restructure them.

First, we will bring our local repository to the state of the upstream we're going to send pull requests to. Then, we will create the branches, one for each issue we solved. Finally, to each branch we will apply the entire diff between it and our master with our work (effectively bringing it to the state of our master) and hand-pick the changes to commit for a given issue.

Assuming there's a remote to the main repository named `upstream` in your local repository, your own work is in the `master` branch of your local repository and you want to send pull requests to `upstream/master`, here is how to do that:

1. Get your local repo to the state of the upstream repo: `git fetch upstream && git checkout upstream/master`.
2. Create a branch for the first issue: `git checkout -b issue1`
3. Calculate the diff between `issue1` and `master` (assuming `master` contains your work) and apply it to the current branch: `git diff head master | git apply`
4. Now stage only the changes related to issue1 using [interactive stage](https://git-scm.com/book/en/v2/Git-Tools-Interactive-Staging): `git add -i`. Use the interactive menu to select which files and hunks to stage.
5. Commit the changes: `git commit`
6. Clean up the work directory from the changes you didn't need: `git clean -f; git reset --hard`
7. If you forgot to add some changes to the issue1 branch, go to step (3)