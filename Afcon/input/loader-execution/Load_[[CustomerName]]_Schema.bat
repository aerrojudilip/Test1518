@ECHO OFF
rem ***************************************************************************
rem Licensed Materials - Property of IBM
rem 
rem
rem 5725-D51, 5725-D52, 5725-D53, 5725-D54
rem
rem © Copyright IBM Corporation 2005,2014. All Rights Reserved. 
rem
rem US Government Users Restricted Rights- Use, duplication or 
rem disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
rem ***************************************************************************


title Loading Schema

set env_variables_bat_file=Environment_Variables.bat

setlocal
call %env_variables_bat_file%

set load_type=Schema

set log_file=%load_type%_Load.log
set loader_data_folder=%CD%
pushd "%openpages_domain_folder%" 2>nul



REM --------------------------------------
REM Check that the OPDomain folder exists
REM --------------------------------------
if %errorlevel%==0 goto checkForObjectManager
REM --------------------------------------------
REM Bad OP Domain - path or drive doesn't exist
REM --------------------------------------------
echo.
echo The openpages_domain_folder is incorrect.  Please open the
echo %env_variables_bat_file% file in a text editor
echo such as Notepad, and edit the openpages_domain_folder
echo variable to match the location of the OpenPages 
echo folder on your application server.
echo.
title Error!
color cf
goto end



REM -------------------------------------------------------
REM Check that the OPDomain folder contains ObjectManager
REM -------------------------------------------------------
:checkForObjectManager
if exist "ObjectManager.cmd" goto load
REM ----------------------------
REM ObjectManager wasn't found
REM ----------------------------
echo.
echo ObjectManager could not be found in "%CD%".
echo.
echo Please open the %env_variables_bat_file% file in a 
echo text editor such as Notepad, and make sure
echo the openpages_domain_folder variable correctly points
echo to the location of the OpenpagesDomain folder on your
echo application server.
echo.
title Error!
color cf
goto end



REM -------------
REM Load Files
REM -------------
:load
echo Loading Schema...
call ObjectManager l c %login_username% %login_password% "%loader_data_folder%" "[[schemaFile]]" >"%loader_data_folder%\%load_type%_Load_Schema.log" 2>&1
echo Loading Object Profile...
call ObjectManager l c %login_username% %login_password% "%loader_data_folder%" "[[profileFile]]" >"%loader_data_folder%\%load_type%_Load_Profile.log" 2>&1
echo Loading Object Text...
call ObjectManager l c %login_username% %login_password% "%loader_data_folder%" "[[stringsFile]]" >"%loader_data_folder%\%load_type%_Load_Object_Strings.log" 2>&1
echo Loading Rules Text...
call ObjectManager l c %login_username% %login_password% "%loader_data_folder%" "[[rulesFile]]" >"%loader_data_folder%\%load_type%_Load_Rules.log" 2>&1


REM -------------------------------------
REM Check for errors in all the log files together
REM -------------------------------------
findstr "ERROR EXCEPTION" "%loader_data_folder%\*.log">nul
if errorlevel 1 goto noerror


REM ------------------------------------
REM Check log for ObjectManager errors in Schema File
REM ------------------------------------
:checkForSchemaLoadErrors
findstr "ERROR EXCEPTION" "%loader_data_folder%\%load_type%_Load_Schema.log" >nul
if errorlevel 1 goto checkForProfileLoadErrors
REM ------------------------------------
REM An ObjectManager error occurred
REM ------------------------------------
echo.
echo Errors occurred!  Please see %load_type%_Load_Schema.log for more information.
echo.
title Error!
color cf
goto checkForProfileLoadErrors


REM ------------------------------------
REM Check log for ObjectManager errors in Profile File
REM ------------------------------------
:checkForProfileLoadErrors
findstr "ERROR EXCEPTION" "%loader_data_folder%\%load_type%_Load_Profile.log" >nul
if errorlevel 1 goto checkForObjectStringsLoadErrors
REM ------------------------------------
REM An ObjectManager error occurred in Profile File
REM ------------------------------------
echo.
echo Errors occurred!  Please see %load_type%_Load_Profile.log for more information.
echo.
title Error!
color cf
goto checkForObjectStringsLoadErrors


REM ------------------------------------
REM Check log for ObjectManager errors in Object Strings File
REM ------------------------------------
:checkForObjectStringsLoadErrors
findstr "ERROR EXCEPTION" "%loader_data_folder%\%load_type%_Load_Object_Strings.log" >nul
if errorlevel 1 goto checkForRulesLoadErrors
REM ------------------------------------
REM An ObjectManager error occurred in Object Strings File
REM ------------------------------------
echo.
echo Errors occurred!  Please see %load_type%_Load_Object_Strings.log for more information.
echo.
title Error!
color cf
goto checkForRulesLoadErrors


REM ------------------------------------
REM Check log for ObjectManager errors in Rules File
REM ------------------------------------
:checkForRulesLoadErrors
findstr "ERROR EXCEPTION" "%loader_data_folder%\%load_type%_Load_Rules.log" >nul
if errorlevel 1 goto end
REM ------------------------------------
REM An ObjectManager error occurred in Rules File
REM ------------------------------------
echo.
echo Errors occurred!  Please see %load_type%_Load_Rules.log for more information.
echo.
title Error!
color cf
goto end



REM ------------------------------------
REM No errors occurred
REM ------------------------------------
:noerror
echo.
echo Done!  No errors were detected.
echo.
title Done!
color 2f


:end
popd
endlocal
pause
