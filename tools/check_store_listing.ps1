# Validates the Play Store listing copy in docs/store/listing.md.
# Fails when the short or full description is missing, over the Play limit, or
# when both fields contain the same text (the Metadata policy rejection).
param(
    [string]$Path = (Join-Path $PSScriptRoot '..\docs\store\listing.md')
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path -LiteralPath $Path)) {
    Write-Error "Listing file not found: $Path"
    exit 1
}

$text = Get-Content -LiteralPath $Path -Raw -Encoding UTF8

function Get-CodeBlockAfterHeading([string]$heading) {
    $pattern = '## {0}[^\r\n]*\r?\n\r?\n```text\r?\n(?<body>.*?)\r?\n```' -f $heading
    $match = [regex]::Match($text, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if (-not $match.Success) {
        throw ('Could not find the {0} code block in {1}' -f $heading, $Path)
    }
    return $match.Groups['body'].Value.Trim()
}

try {
    $short = Get-CodeBlockAfterHeading 'Short description'
    $full = Get-CodeBlockAfterHeading 'Full description'
} catch {
    Write-Error $_.Exception.Message
    exit 1
}

$errors = @()

if ([string]::IsNullOrWhiteSpace($short)) {
    $errors += 'Short description is empty.'
} elseif ($short.Length -gt 80) {
    $errors += "Short description is $($short.Length) characters; the limit is 80."
}

if ([string]::IsNullOrWhiteSpace($full)) {
    $errors += 'Full description is empty.'
} elseif ($full.Length -gt 4000) {
    $errors += "Full description is $($full.Length) characters; the limit is 4000."
}

if ($short -eq $full) {
    $errors += 'Short and full descriptions are identical.'
}

if ($errors.Count -gt 0) {
    $errors | ForEach-Object { Write-Host "ERROR: $_" -ForegroundColor Red }
    exit 1
}

Write-Host "Store listing OK: short $($short.Length)/80 characters, full $($full.Length)/4000 characters, descriptions differ." -ForegroundColor Green
