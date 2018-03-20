#!/bin/bash
# run all apks in target dir one by one.
DIR=$(dirname $(readlink -f ${BASH_SOURCE[0]}))
[ "$DIR" != "$(pwd)" ] && { echo "Must be invoked from $DIR" ; exit 1 ; }
echo $@
for file in `ls $1`
do
	path=$1"/"$file
	echo $path
	eval './gradlew run -Pargs="'$path'"'
done

#eval './gradlew run -Pargs="'$@'"'
