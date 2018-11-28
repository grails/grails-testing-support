#!/bin/bash
set -e
./gradlew clean classes --no-daemon --stacktrace
./gradlew check assemble --no-daemon -x groovydoc

EXIT_STATUS=0
echo "Publishing archives for branch $TRAVIS_BRANCH"
if [[ -n $TRAVIS_TAG ]] || [[ $TRAVIS_BRANCH =~ ^master$ && $TRAVIS_PULL_REQUEST == 'false' ]]; then

  echo "Publishing archives"

  if [[ -n $TRAVIS_TAG ]]; then
      ./gradlew bintrayUpload || EXIT_STATUS=$?
  else
      ./gradlew publish || EXIT_STATUS=$?
      echo "Publish status: $EXIT_STATUS"
  fi

  ./gradlew docs || EXIT_STATUS=$?
  echo "Docs status: $EXIT_STATUS"

  git config --global user.name "$GIT_NAME"
  git config --global user.email "$GIT_EMAIL"
  git config --global credential.helper "store --file=~/.git-credentials"
  echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

  git clone https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git -b gh-pages gh-pages --single-branch > /dev/null
  cd gh-pages

  # If this is the master branch then update the snapshot
  if [[ $TRAVIS_BRANCH =~ ^master|[12]\..\.x$ ]]; then
     mkdir -p snapshot
     cp -r ../build/docs/. ./snapshot/

     git add snapshot/*
  fi

  # If there is a tag present then this becomes the latest
  if [[ -n $TRAVIS_TAG ]]; then
      mkdir -p latest
      cp -r ../build/docs/. ./latest/
      git add latest/*

      version="$TRAVIS_TAG"
      version=${version:1}
      majorVersion=${version:0:4}
      majorVersion="${majorVersion}x"

      mkdir -p "$version"
      cp -r ../build/docs/. "./$version/"
      git add "$version/*"

      mkdir -p "$majorVersion"
      cp -r ../build/docs/. "./$majorVersion/"
      git add "$majorVersion/*"

  fi

  git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID" && {
    git push origin HEAD || true
  }
  cd ..
  rm -rf gh-pages
fi
echo "Finished build with status: $EXIT_STATUS"
exit $EXIT_STATUS