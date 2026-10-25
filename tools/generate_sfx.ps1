# Generates the synthesised sound effects used by Prime or Not.
#
# Everything is 44.1 kHz mono 16-bit PCM, written straight into res/raw so the
# APK has no audio licensing to worry about and the samples stay reproducible:
#
#   powershell -ExecutionPolicy Bypass -File tools/generate_sfx.ps1
#
# Re-run after changing any duration or frequency below.

param(
    [string]$OutputDir = (Join-Path $PSScriptRoot '..\app\src\main\res\raw')
)

$ErrorActionPreference = 'Stop'
$SampleRate = 44100

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

function Write-WavFile {
    param(
        [string]$Name,
        [double[]]$Samples,
        [double]$Peak = 0.85
    )

    $max = 0.0
    foreach ($sample in $Samples) {
        $magnitude = [math]::Abs($sample)
        if ($magnitude -gt $max) { $max = $magnitude }
    }
    $gain = if ($max -gt 0.0) { $Peak / $max } else { 0.0 }

    $dataSize = $Samples.Length * 2
    $bytes = [byte[]]::new(44 + $dataSize)

    [System.Text.Encoding]::ASCII.GetBytes('RIFF').CopyTo($bytes, 0)
    [BitConverter]::GetBytes([int](36 + $dataSize)).CopyTo($bytes, 4)
    [System.Text.Encoding]::ASCII.GetBytes('WAVE').CopyTo($bytes, 8)
    [System.Text.Encoding]::ASCII.GetBytes('fmt ').CopyTo($bytes, 12)
    [BitConverter]::GetBytes([int]16).CopyTo($bytes, 16)              # fmt chunk size
    [BitConverter]::GetBytes([int16]1).CopyTo($bytes, 20)             # PCM
    [BitConverter]::GetBytes([int16]1).CopyTo($bytes, 22)             # mono
    [BitConverter]::GetBytes([int]$SampleRate).CopyTo($bytes, 24)
    [BitConverter]::GetBytes([int]($SampleRate * 2)).CopyTo($bytes, 28)
    [BitConverter]::GetBytes([int16]2).CopyTo($bytes, 32)             # block align
    [BitConverter]::GetBytes([int16]16).CopyTo($bytes, 34)            # bits
    [System.Text.Encoding]::ASCII.GetBytes('data').CopyTo($bytes, 36)
    [BitConverter]::GetBytes([int]$dataSize).CopyTo($bytes, 40)

    for ($i = 0; $i -lt $Samples.Length; $i++) {
        $value = [math]::Max(-1.0, [math]::Min(1.0, $Samples[$i] * $gain))
        $pcm = [int16][math]::Round($value * 32767)
        [BitConverter]::GetBytes($pcm).CopyTo($bytes, 44 + $i * 2)
    }

    $path = Join-Path $OutputDir "$Name.wav"
    [System.IO.File]::WriteAllBytes($path, $bytes)
    Write-Host ("wrote {0} ({1:n0} ms)" -f $path, ($Samples.Length / $SampleRate * 1000))
}

# --- swipe.wav: a short filtered-noise sweep, like a card leaving the deck ----
$count = [int](0.13 * $SampleRate)
$swipe = [double[]]::new($count)
$random = [System.Random]::new(11)
$low = 0.0
for ($i = 0; $i -lt $count; $i++) {
    $t = $i / [double]$count
    $white = $random.NextDouble() * 2.0 - 1.0
    $cutoff = 0.55 - 0.45 * $t
    $low += $cutoff * ($white - $low)
    $high = $white - $low
    $envelope = [math]::Pow(1.0 - $t, 2.2)
    $swipe[$i] = ($low * 0.7 + $high * 0.5) * $envelope
}
Write-WavFile -Name 'swipe' -Samples $swipe -Peak 0.7

# --- correct.wav: an 880 Hz ding with two harmonics and a fast decay ---------
$count = [int](0.18 * $SampleRate)
$correct = [double[]]::new($count)
for ($i = 0; $i -lt $count; $i++) {
    $t = $i / [double]$SampleRate
    $envelope = [math]::Exp(-24.0 * $t) * [math]::Min(1.0, $t / 0.004)
    $correct[$i] = $envelope * (
        [math]::Sin(2.0 * [math]::PI * 880.0 * $t) +
        0.35 * [math]::Sin(2.0 * [math]::PI * 1320.0 * $t) +
        0.18 * [math]::Sin(2.0 * [math]::PI * 2640.0 * $t)
    )
}
Write-WavFile -Name 'correct' -Samples $correct -Peak 0.8

# --- wrong.wav: a low, slightly buzzy thud ----------------------------------
$count = [int](0.26 * $SampleRate)
$wrong = [double[]]::new($count)
for ($i = 0; $i -lt $count; $i++) {
    $t = $i / [double]$SampleRate
    $envelope = [math]::Exp(-9.0 * $t) * [math]::Min(1.0, $t / 0.006)
    $square = [math]::Sign([math]::Sin(2.0 * [math]::PI * 140.0 * $t))
    $wrong[$i] = $envelope * (
        0.7 * [math]::Sin(2.0 * [math]::PI * 140.0 * $t) +
        0.3 * $square
    )
}
Write-WavFile -Name 'wrong' -Samples $wrong -Peak 0.8

# --- tick.wav: an 18 ms click for the last two seconds ----------------------
$count = [int](0.018 * $SampleRate)
$tick = [double[]]::new($count)
for ($i = 0; $i -lt $count; $i++) {
    $t = $i / [double]$SampleRate
    $envelope = [math]::Exp(-220.0 * $t)
    $tick[$i] = $envelope * (
        [math]::Sin(2.0 * [math]::PI * 1800.0 * $t) +
        0.4 * [math]::Sin(2.0 * [math]::PI * 2600.0 * $t)
    )
}
Write-WavFile -Name 'tick' -Samples $tick -Peak 0.55

# --- levelup.wav: four rising notes, a small reward arpeggio -----------------
$notes = @(523.25, 659.25, 783.99, 1046.50)
$count = [int](0.55 * $SampleRate)
$levelUp = [double[]]::new($count)
$step = 0.085
for ($k = 0; $k -lt $notes.Count; $k++) {
    $start = $k * $step
    $firstIndex = [int]($start * $SampleRate)
    for ($i = $firstIndex; $i -lt $count; $i++) {
        $t = ($i / [double]$SampleRate) - $start
        if ($t -lt 0.0) { continue }
        $envelope = [math]::Exp(-13.0 * $t) * [math]::Min(1.0, $t / 0.004)
        $levelUp[$i] += $envelope * [math]::Sin(2.0 * [math]::PI * $notes[$k] * $t)
    }
}
Write-WavFile -Name 'levelup' -Samples $levelUp -Peak 0.75
