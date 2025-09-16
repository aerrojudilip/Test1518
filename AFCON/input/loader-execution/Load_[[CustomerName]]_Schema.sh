#!/bin/bash

#*******************************************************************************
# Licensed Materials - Property of IBM
# 
#
# 5725-D51, 5725-D52, 5725-D53, 5725-D54
#
# © Copyright IBM Corporation 2005, 2023. All Rights Reserved. 
#
# US Government Users Restricted Rights- Use, duplication or 
# disclosure restricted by GSA ADP Schedule Contract with IBM Corp. 
#*******************************************************************************

env_variables_file="./Environment_Variables.sh"
object_manager_file="./ObjectManager.sh"

. $env_variables_file

log_file="./Schema_Load.log"
loader_data_folder=$(pwd)

# check for openpages/bin directory
ls -l $openpages_domain_folder > /dev/null 2>&1

if [[ $? -ne 0 ]]; then
	__PRINT__ "The openpages_domain_folder is incorrect. Please open the $env_variables_file file in a text editor, and edit the openpages_domain_folder variable to match the location of the OpenPages bin folder on your application server."
else
	cd $openpages_domain_folder
	
	#check for objectmanager.sh file
	ls -l $object_manager_file > /dev/null 2>&1
	if [[ $? -ne 0 ]]; then
		__PRINT__ "ObjectManager could not be found in $openpages_domain_folder. Please open the $env_variables_file file in a text editor, and make sure the openpages_domain_folder variable correctly points to the location of the OpenPages bin folder on your application server."
	else
	
		#load the files
		__PRINT__ "Loading Schema..."
		./ObjectManager.sh l c $login_username $login_password $loader_data_folder "[[schemaFile]]" > "${loader_data_folder}/${log_file}" 2>&1
		__PRINT__ "Loading Object Profile..."
		./ObjectManager.sh l c $login_username $login_password $loader_data_folder "[[profileFile]]" >>"${loader_data_folder}/${log_file}" 2>&1
		__PRINT__ "Loading Object Text..."
		./ObjectManager.sh l c $login_username $login_password $loader_data_folder "[[stringsFile]]" >>"${loader_data_folder}/${log_file}" 2>&1
		__PRINT__ "Loading Rules Text..."
		./ObjectManager.sh l c $login_username $login_password $loader_data_folder "[[rulesFile]]" >>"${loader_data_folder}/${log_file}" 2>&1
	
		#check log file for errors
		grep -q "ERROR" "${loader_data_folder}/${log_file}"
		if [[ $? -eq 0 ]]; then
			__PRINT__ "Errors occurred!  Please see $log_file for more information."
		else
			grep -q "EXCEPTION" "${loader_data_folder}/${log_file}"
			
			if [[ $? -eq 0 ]]; then
				__PRINT__ "Errors occurred!  Please see $log_file for more information."
			else
				__PRINT__ "Done!  No errors were detected."
			fi
		fi
	fi
fi

cd $loader_data_folder
