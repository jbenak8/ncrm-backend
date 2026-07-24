<#
.SYNOPSIS
    Builds the nCRM backend and installs it as a background Windows service
    (standalone "db-auth" deployment: no Keycloak, MailHog or bundled PostgreSQL).

.DESCRIPTION
    1. Builds the executable jar with the Maven wrapper (tests skipped).
    2. Copies the jar to the installation directory (default C:\Program Files\ncrm-backend).
    3. Downloads the WinSW service wrapper (unless already present) and generates its XML
       configuration; the environment variables are read from the "ncrm-backend.env" file
       in the installation directory (created from the template on first install).
    4. Installs and configures the "ncrm-backend" Windows service (automatic start).

    After the installation, edit <InstallDir>\ncrm-backend.env (database, SMTP, NCRM_JWT_SECRET)
    and start the service with:  Start-Service ncrm-backend

.PARAMETER InstallDir
    Target installation directory. Default: C:\Program Files\ncrm-backend

.PARAMETER Uninstall
    Removes the Windows service (the installation directory is kept).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File deploy\standalone\build-and-install.ps1

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File deploy\standalone\build-and-install.ps1 -Uninstall
#>
[CmdletBinding()]
param(
    [string]$InstallDir = "$env:ProgramFiles\ncrm-backend",
    [switch]$Uninstall
)

$ErrorActionPreference = 'Stop'
$ServiceName = 'ncrm-backend'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = (Resolve-Path (Join-Path $ScriptDir '..\..')).Path
$WinswUrl = 'https://github.com/winsw/winsw/releases/download/v2.12.0/WinSW-x64.exe'
$WinswExe = Join-Path $InstallDir "$ServiceName-service.exe"

function Assert-Administrator {
    $identity = [Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
    if (-not $identity.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        throw 'This script must be run from an elevated (Administrator) PowerShell session.'
    }
}

if ($Uninstall) {
    Assert-Administrator
    if (Test-Path $WinswExe) {
        & $WinswExe stop | Out-Null
        & $WinswExe uninstall
        Write-Host "Service '$ServiceName' uninstalled. The directory '$InstallDir' was kept."
    } else {
        Write-Host "Nothing to uninstall: '$WinswExe' not found."
    }
    return
}

Write-Host "==> Building $ServiceName (tests skipped)..."
Push-Location $ProjectDir
try {
    & .\mvnw.cmd -q package '-DskipTests'
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE." }
} finally {
    Pop-Location
}

$JarFile = Get-ChildItem (Join-Path $ProjectDir 'target\*.jar') |
    Where-Object { $_.Name -notmatch '(sources|javadoc)\.jar$' } |
    Select-Object -First 1
if (-not $JarFile) { throw "No jar found in $ProjectDir\target." }
Write-Host "==> Built $($JarFile.FullName)"

Assert-Administrator

Write-Host "==> Installing to $InstallDir..."
New-Item -ItemType Directory -Force -Path $InstallDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $InstallDir 'logs') | Out-Null
Copy-Item $JarFile.FullName (Join-Path $InstallDir "$ServiceName.jar") -Force

$EnvFile = Join-Path $InstallDir "$ServiceName.env"
if (-not (Test-Path $EnvFile)) {
    Copy-Item (Join-Path $ScriptDir "$ServiceName.env.example") $EnvFile
    Write-Host "==> Created environment file template: $EnvFile"
} else {
    Write-Host "==> Keeping existing $EnvFile"
}

if (-not (Test-Path $WinswExe)) {
    Write-Host "==> Downloading WinSW service wrapper..."
    Invoke-WebRequest -Uri $WinswUrl -OutFile $WinswExe -UseBasicParsing
}

# Translate the KEY=VALUE lines of the env file into WinSW <env> elements.
$EnvXml = (Get-Content $EnvFile |
    Where-Object { $_ -match '^\s*[^#\s][^=]*=' } |
    ForEach-Object {
        $name, $value = $_ -split '=', 2
        $escaped = [System.Security.SecurityElement]::Escape($value.Trim())
        "  <env name=""$($name.Trim())"" value=""$escaped"" />"
    }) -join "`r`n"

$ConfigXml = @"
<service>
  <id>$ServiceName</id>
  <name>nCRM backend</name>
  <description>nCRM backend (standalone, database authentication) running as a background service.</description>
  <executable>java</executable>
  <arguments>-XX:MaxRAMPercentage=75 -jar "$InstallDir\$ServiceName.jar"</arguments>
  <workingdirectory>$InstallDir</workingdirectory>
  <startmode>Automatic</startmode>
  <onfailure action="restart" delay="10 sec" />
  <logpath>$InstallDir\logs</logpath>
  <log mode="roll-by-size">
    <sizeThreshold>10240</sizeThreshold>
    <keepFiles>5</keepFiles>
  </log>
$EnvXml
  <stoptimeout>30 sec</stoptimeout>
</service>
"@
Set-Content -Path (Join-Path $InstallDir "$ServiceName-service.xml") -Value $ConfigXml -Encoding UTF8

Write-Host "==> Installing the Windows service..."
if (Get-Service -Name $ServiceName -ErrorAction SilentlyContinue) {
    & $WinswExe stop | Out-Null
    & $WinswExe refresh
} else {
    & $WinswExe install
}
if ($LASTEXITCODE -ne 0) { throw "WinSW installation failed with exit code $LASTEXITCODE." }

Write-Host ''
Write-Host 'Installation finished.'
Write-Host "1. Edit $EnvFile (database, SMTP, NCRM_JWT_SECRET), then re-run this script to apply changes."
Write-Host "2. Start the service:   Start-Service $ServiceName"
Write-Host "3. Check the status:    Get-Service $ServiceName"
Write-Host "4. Logs are written to: $InstallDir\logs"
