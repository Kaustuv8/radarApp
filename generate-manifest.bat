@echo off
setlocal enabledelayedexpansion

set MAINCLASS=Main
set LIBDIR=lib
set MANIFEST=manifest.txt

> %MANIFEST% echo Manifest-Version: 1.0
>> %MANIFEST% echo Main-Class: %MAINCLASS%

set "line=Class-Path:"
set "linelen=12"

for %%f in (%LIBDIR%\*.jar) do (
    set "jar=%%~f"
    set "jar=!jar:\=/!"
    set "jar=!jar:%CD%\\=!"
    set "jar=lib/%%~nxf"

    set /a newlen=!linelen! + 1 + 14
    if !newlen! GEQ 72 (
        >> %MANIFEST% echo !line!
        set "line= !jar!"
        set "linelen=1 + 14"
    ) else (
        set "line=!line! !jar!"
        set /a linelen=!newlen!
    )
)

>> %MANIFEST% echo !line!

echo Manifest created as %MANIFEST%
