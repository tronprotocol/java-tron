#!/bin/bash

echo "Initializing "

apk add --no-progress -q gettext

for file in $(find /config-source); do
  if [[ -d $file ]]; then
    mkdir -p /config-destination${file#/config-source}
  fi
  if [[ -f $file ]]; then
    dest=/config-destination${file#/config-source}
    if [[ ${file##*.} == sh ]]; then
      echo "Copying executable $file -> $dest"
      cp -f $file $dest
      chmod a+x $dest
    else
      echo "Processing $file -> $dest"
      envsubst <$file >$dest
    fi

  fi
done
