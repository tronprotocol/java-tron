#!/bin/bash
set -x 
currentBranch=`git rev-parse --abbrev-ref HEAD`

if [ "$currentBranch"x != "master"x ]
then
  echo The current branch is not master. Please checkout into master.
  exit 1
else
  git pull origin master
fi

versionName=`git describe --tags`
versionCode=`git rev-list HEAD --count`
versionBranch=version/$versionName

git checkout -b $versionBranch
if [ $? -ne 0 ]
then
  echo A branch named $versionBranch already exists. Will delete the local and remote branch and re run.
  git branch -D $versionBranch
  git push origin :$versionBranch
  git checkout -b $versionBranch
fi


versionPath="src/main/java/org/tron/program/Version.java"
sed -i -e "s/VERSION_NAME.*$/VERSION_NAME = \"$versionName\";/g;s/VERSION_CODE.*$/VERSION_CODE = \"$versionCode\";/g" $versionPath
git add $versionPath
git commit -m "update a new version. version name:$versionName,version code:$versionCode"
git push origin $versionBranch
