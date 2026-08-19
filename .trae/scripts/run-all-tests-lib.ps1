[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = "http://localhost:8080"
$utf8 = [System.Text.Encoding]::UTF8

function ReadText($bytesOrString) {
    if ($null -eq $bytesOrString) { return '' }
    if ($bytesOrString -is [string]) { return $bytesOrString }
    if ($bytesOrString -is [byte[]]) { return $utf8.GetString($bytesOrString) }
    try { return [string]$bytesOrString } catch { return '' }
}
function ParseJson($txt) {
    if ([string]::IsNullOrWhiteSpace($txt)) { return $null }
    try { return ($txt | ConvertFrom-Json) } catch { return $null }
}
function Post($path, $bodyObj, $headers=$null) {
    $body = $utf8.GetBytes((ConvertTo-Json $bodyObj -Depth 5 -Compress))
    $h = @{"Content-Type" = "application/json;charset=utf-8"}
    if ($headers) { foreach ($k in $headers.Keys) { $h[$k] = $headers[$k] } }
    try {
        $raw = Invoke-WebRequest -Uri "$base$path" -Method POST -Body $body -Headers $h -TimeoutSec 25 -UseBasicParsing
        $txt = ReadText $raw.Content
        return @{ ok=$true; status=$raw.StatusCode; json=(ParseJson $txt); raw=$txt }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream(), $utf8)
                $txt = $sr.ReadToEnd(); $sr.Close()
                return @{ ok=$false; status=[int]$resp.StatusCode; json=(ParseJson $txt); raw=$txt }
            } catch { }
        }
        return @{ ok=$false; status=-1; error=$_.Exception.Message }
    }
}
function Put($path, $bodyObj, $headers=$null) {
    $body = $utf8.GetBytes((ConvertTo-Json $bodyObj -Depth 5 -Compress))
    $h = @{"Content-Type" = "application/json;charset=utf-8"}
    if ($headers) { foreach ($k in $headers.Keys) { $h[$k] = $headers[$k] } }
    try {
        $raw = Invoke-WebRequest -Uri "$base$path" -Method PUT -Body $body -Headers $h -TimeoutSec 25 -UseBasicParsing
        $txt = ReadText $raw.Content
        return @{ ok=$true; status=$raw.StatusCode; json=(ParseJson $txt); raw=$txt }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream(), $utf8)
                $txt = $sr.ReadToEnd(); $sr.Close()
                return @{ ok=$false; status=[int]$resp.StatusCode; json=(ParseJson $txt); raw=$txt }
            } catch { }
        }
        return @{ ok=$false; status=-1; error=$_.Exception.Message }
    }
}
function Get($path, $headers=$null) {
    $h = @{}
    if ($headers) { foreach ($k in $headers.Keys) { $h[$k] = $headers[$k] } }
    try {
        $raw = Invoke-WebRequest -Uri "$base$path" -Method GET -Headers $h -TimeoutSec 25 -UseBasicParsing
        $ct = if($raw.Headers['Content-Type']) { [string]($raw.Headers['Content-Type']) } else { '' }
        if ($ct -match 'json') {
            $txt = ReadText $raw.Content
            return @{ ok=$true; status=$raw.StatusCode; json=(ParseJson $txt); raw=$txt; contentType=$ct }
        } else {
            $txt = ReadText $raw.Content
            return @{ ok=$true; status=$raw.StatusCode; contentType=$ct; raw=$txt; length=$txt.Length }
        }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream(), $utf8)
                $txt = $sr.ReadToEnd(); $sr.Close()
                return @{ ok=$false; status=[int]$resp.StatusCode; json=(ParseJson $txt); raw=$txt }
            } catch { }
        }
        return @{ ok=$false; status=-1; error=$_.Exception.Message }
    }
}
function DoDelete($path, $headers=$null) {
    $h = @{}
    if ($headers) { foreach ($k in $headers.Keys) { $h[$k] = $headers[$k] } }
    try {
        $raw = Invoke-WebRequest -Uri "$base$path" -Method DELETE -Headers $h -TimeoutSec 25 -UseBasicParsing
        $txt = ReadText $raw.Content
        return @{ ok=$true; status=$raw.StatusCode; json=(ParseJson $txt); raw=$txt }
    } catch {
        $resp = $_.Exception.Response
        if ($resp) {
            try {
                $sr = New-Object System.IO.StreamReader($resp.GetResponseStream(), $utf8)
                $txt = $sr.ReadToEnd(); $sr.Close()
                return @{ ok=$false; status=[int]$resp.StatusCode; json=(ParseJson $txt); raw=$txt }
            } catch { }
        }
        return @{ ok=$false; status=-1; error=$_.Exception.Message }
    }
}
function Code($r) {
    if ($r -and $r.json -and $null -ne $r.json.code) { return [string]$r.json.code }
    if ($r -and $r.status) { return [string]$r.status }
    return '-1'
}
function case($id, $desc, $actualCode, $expectedCodes='0') {
    $pass = $expectedCodes.Split(',') -contains [string]$actualCode
    $stamp = Get-Date -Format 'HH:mm:ss'
    Write-Output ("CASE|{0}|{1}|{2}|{3}|{4}" -f $id, $(if($pass){'通过'}else{'失败'}), $actualCode, $stamp, $desc)
    return $pass
}