param(
    [Parameter(Mandatory = $true)]
    [ValidateScript({ Test-Path -LiteralPath $_ -PathType Container })]
    [string] $BasePath,

    [switch] $Execute
)

$outputFile = Join-Path $BasePath "comandos_mvn.txt"
$libs = @(
    @{ file = "email-helper-3.2.0.jar"; groupId = "local.jarios"; artifactId = "email-helper"; version = "3.2.0" },
    @{ file = "properties-helper-4.2.0.jar"; groupId = "local.jarios"; artifactId = "properties-helper"; version = "4.2.0" },
    @{ file = "version-helper-2.2.0.jar"; groupId = "local.jarios"; artifactId = "version-helper"; version = "2.2.0" },
    @{ file = "encrypt-helper-2.1.0.jar"; groupId = "local.jarios"; artifactId = "encrypt-helper"; version = "2.1.0" }
)

$validPattern = '^[a-zA-Z0-9._-]+$'
Set-Content -LiteralPath $outputFile -Encoding utf8 -Value ""

foreach ($lib in $libs) {
    foreach ($field in @("file", "groupId", "artifactId", "version")) {
        if ($lib[$field] -notmatch $validPattern) {
            Write-Warning "$field invalido: $($lib[$field])"
            continue 2
        }
    }

    $filePath = Join-Path $BasePath $lib.file
    if ($filePath.Length -gt 260) {
        Write-Warning "La ruta del archivo es demasiado larga: $filePath"
        continue
    }

    if (-not (Test-Path -LiteralPath $filePath -PathType Leaf)) {
        Write-Warning "Archivo no encontrado: $filePath"
        continue
    }

    $mavenArgs = @(
        "install:install-file",
        "-Dfile=$filePath",
        "-DgroupId=$($lib.groupId)",
        "-DartifactId=$($lib.artifactId)",
        "-Dversion=$($lib.version)",
        "-Dpackaging=jar"
    )

    "mvn $($mavenArgs -join ' ')" | Out-File -LiteralPath $outputFile -Append -Encoding utf8

    if ($Execute) {
        & mvn @mavenArgs
        if ($LASTEXITCODE -ne 0) {
            throw "Fallo instalando $($lib.file)"
        }
    }
}

Write-Host "Comandos Maven guardados en: $outputFile"
