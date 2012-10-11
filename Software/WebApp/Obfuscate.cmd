echo off
REM Script to obfuscate the JavaScript files.
REM Source folder: src
REM Output folder: js
REM %%-nx
REM @author	Man Hoang

REM for %%f in (src\*.js) do java -jar yuicompressor-2.4.2.jar --type js %%f >> js\TIUTracking.js
for %%f in (src\*.js) do java -jar yuicompressor-2.4.2.jar --type js %%f -o js\%%~nxf

REM set f=Temp.js
REM set g=Temp2.js

REM Merge all JS files into one single file.
REM echo on
REM echo ^(function(){ > %f%
REM copy src\*.js %f%
REM echo })(); >> %f%

REM java -jar yuicompressor-2.4.2.jar --type js %f% -o js\TIUTracking.js
REM del %f%
