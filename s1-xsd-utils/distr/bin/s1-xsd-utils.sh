#!/bin/sh
java -classpath ../conf -server -Xms2048M -Xms1024M -jar ../lib/s1-xsd-utils.jar $@ 2>../log/log.txt