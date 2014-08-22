#!/bin/sh
 
# 
# Strips the Google Play Services JAR archive and removes the folders that are specified below.
# This should lower the methods count of the final DEX file and should lower the chance of hitting
# the (dreaded) 64K methods limit.
#
 
#
# Author: Sebastiano Gottardo
# @rotxed - dextorer[at]gmail[dot]com
# 
 
AWS_SERVICES_FILENAME="aws-android-sdk-2.0.5-core.jar"
AWS_SERVICES_TEMP_DIR="aws-android-sdk-2.0.5-core-temp"
AWS_SERVICES_STRIP_FILE="strip_aws_core.conf"
AWS_SERVICES_NESTED_PATH="com/amazonaws"
AWS_SERVICES_OUTPUT_FILE="aws-android-sdk-2.0.5-core-STRIPPED.jar"
 
# Check if file exists in the same directory
if [ ! -f $AWS_SERVICES_FILENAME ]; then
	echo "\nPlease put this script in the Play Services JAR location, then run it again\n\n"
	exit -1
fi
 
# Preventive cleanup 
rm -rf $AWS_SERVICES_TEMP_DIR
 
# Create temporary work folder
mkdir $AWS_SERVICES_TEMP_DIR
cp $AWS_SERVICES_FILENAME $AWS_SERVICES_TEMP_DIR/
cd $AWS_SERVICES_TEMP_DIR
 
# Extract the content of the Play Services JAR archive
echo "Extracting archive, please wait.."
jar xvf $AWS_SERVICES_FILENAME > /dev/null
echo "Extracted.\n"
 
# If the configuration file doesn't exist, create it
if [ ! -f ../$AWS_SERVICES_STRIP_FILE ]; then
	# Create the file
	touch ../$AWS_SERVICES_STRIP_FILE
	FOLDERS=`ls $AWS_SERVICES_NESTED_PATH`
	for index in $FOLDERS
	do
    	echo "$index=true" >> ../$AWS_SERVICES_STRIP_FILE
	done
fi
 
# Read configuration from file
while read -r FOLDERS
do
    CURRENT_FOLDER=$FOLDERS
    CURRENT_FOLDER_NAME=`echo $CURRENT_FOLDER | awk -F'=' '{print $1}'`
    CURRENT_FOLDER_ENABLED=`echo $CURRENT_FOLDER | awk -F'=' '{print $2}'`
    if [ "$CURRENT_FOLDER_ENABLED" = false ]; then
    	echo "Removed $CURRENT_FOLDER_NAME folder"
		rm -rf "$AWS_SERVICES_NESTED_PATH/$CURRENT_FOLDER_NAME"
	fi
done < ../"$AWS_SERVICES_STRIP_FILE"
 
# Create final stripped JAR
jar cf $AWS_SERVICES_OUTPUT_FILE com/
cp $AWS_SERVICES_OUTPUT_FILE ../
cd ..
 
# Clean up
echo "\nFolders removed, cleaning up.."
rm -rf $AWS_SERVICES_TEMP_DIR
 
echo "All done, exiting!"
exit 0