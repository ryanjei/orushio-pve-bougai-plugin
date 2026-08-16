param([Parameter(Mandatory=$true)][string]$Handoff)
$deadline=[DateTime]::UtcNow.AddSeconds(120)
while([DateTime]::UtcNow -lt $deadline){
    if(Test-Path -LiteralPath $Handoff){
        $url=Get-Content -LiteralPath $Handoff -Raw
        Remove-Item -LiteralPath $Handoff -Force -ErrorAction SilentlyContinue
        if($url -like 'http://127.0.0.1:8765/auth/bootstrap?token=*'){Start-Process $url}
        exit 0
    }
    Start-Sleep -Milliseconds 250
}
exit 1
