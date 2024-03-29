Name "MPPatch"
SetCompressor /SOLID /FINAL lzma
OutFile ..\..\target\mppatch-installer-unmodified.exe
CRCCheck off ; We will be altering the final binary in a way that invalidates the CRC

; Mutex code, from https://nsis.sourceforge.io/Allow_only_one_installer_instance
!define INSTALLERMUTEXNAME "MPPatch NSIS Wrapper / 24d1f759-689d-4707-8fb9-3508574253e7"
!macro SingleInstanceMutex
    System::Call 'KERNEL32::CreateMutex(p0, i1, t"${INSTALLERMUTEXNAME}")?e'
    Pop $0
    IntCmpU $0 183 "" launch launch ; ERROR_ALREADY_EXISTS?
        MessageBox MB_ICONSTOP "MPPatch Installer is already running!"
        Abort
    launch:
!macroend
; End mutex code

Function .onInit
    SetSilent silent
    !insertmacro SingleInstanceMutex
FunctionEnd

Function un.onInit
    !insertmacro SingleInstanceMutex
FunctionEnd

Section "Extract and execute wrapped installer"
    RMDir /r $TEMP\MPPatchInstaller
    SetOutPath $TEMP\MPPatchInstaller

    File ..\..\target\native-image-win32\*

    System::Call 'Kernel32::SetEnvironmentVariable(t, t)i ("NSIS_LAUNCH_MARKER", "018c6bba-54e0-7cf2-b16a-7b6abb9215e0").r0'
    System::Call 'Kernel32::SetEnvironmentVariable(t, t)i ("NSIS_LAUNCH_EXE", "$EXEPATH").r0'
    System::Call 'Kernel32::SetEnvironmentVariable(t, t)i ("NSIS_LAUNCH_TEMPDIR", "$TEMP\MPPatchInstaller").r0'

    ExecWait '"$OUTDIR\mppatch-installer.exe"'

    SetOutPath $TEMP
    RMDir /r $TEMP\MPPatchInstaller
SectionEnd
