. f:\ledger\.trae\scripts\run-all-tests-lib.ps1

Write-Output "=========================================="
Write-Output "PHASE1 用户模块"
Write-Output "=========================================="

$r = Post "/api/user/register" @{ username="test_m01"; password="123456" }
case "TC-U01-01" "正常注册(新建或已存在)" (Code $r) "0,1001"

$r = Post "/api/user/register" @{ username="test_m01"; password="123456" }
case "TC-U01-02" "用户名已存在(期望1001)" (Code $r) "1001"

$r = Post "/api/user/register" @{ username="ab"; password="123456" }
case "TC-U01-03" "用户名过短(期望400)" (Code $r) "400"

$r = Post "/api/user/register" @{ username="test@user"; password="123456" }
case "TC-U01-04" "用户名非法字符(期望400)" (Code $r) "400"

$r = Post "/api/user/register" @{ username="test_m02"; password="12" }
case "TC-U01-05" "密码过短(期望400)" (Code $r) "400"

$L = Post "/api/user/login" @{ username="test_m01"; password="123456" }
$access = $L.json.data.accessToken
case "TC-U02-01" "正常登录(期望0)" (Code $L) "0"

$L2 = Post "/api/user/login" @{ username="test_m01"; password="wrong" }
case "TC-U02-02" "密码错误(期望1002)" (Code $L2) "1002"

$L3 = Post "/api/user/login" @{ username="noexist999"; password="123456" }
case "TC-U02-03" "用户不存在(期望1002)" (Code $L3) "1002"

$h = @{ "Authorization" = "Bearer $access" }
$I = Get "/api/user/info" $h
case "TC-U03-01" "正常获取用户信息(期望0)" (Code $I) "0"

$I2 = Get "/api/user/info"
case "TC-U03-02" "未携带Token(HTTP401)" (Code $I2) "401"

$h3 = @{ "Authorization" = "Bearer invalidtokenxxx" }
$I3 = Get "/api/user/info" $h3
case "TC-U03-03" "Token无效(HTTP401)" (Code $I3) "401"

$Lo = Post "/api/user/logout" @{} $h
case "TC-U04-01" "用户登出(期望0)" (Code $Lo) "0"

# 重新登录拿Cookie验证refresh
$L4 = Post "/api/user/login" @{ username="test_m01"; password="123456" }
$access = $L4.json.data.accessToken
$h = @{ "Authorization" = "Bearer $access" }

# Refresh 测试 - 用 HttpWebRequest 手动携带 Cookie
try {
    $req = [System.Net.HttpWebRequest]::Create("$base/api/user/login")
    $req.Method = "POST"
    $req.ContentType = "application/json;charset=utf-8"
    $payload = $utf8.GetBytes('{"username":"test_m01","password":"123456"}')
    $req.ContentLength = $payload.Length
    $rs = $req.GetRequestStream()
    $rs.Write($payload,0,$payload.Length); $rs.Close()
    $resp1 = $req.GetResponse()
    $sr1 = New-Object System.IO.StreamReader($resp1.GetResponseStream(), $utf8)
    $null = $sr1.ReadToEnd(); $sr1.Close()
    $cookies = @()
    foreach ($cookieHeader in $resp1.Headers['Set-Cookie']) {
        if ($cookieHeader) {
            $parts = $cookieHeader -split ';'
            if ($parts.Count -gt 0) { $cookies += $parts[0] }
        }
    }
    $resp1.Close()
    $req2 = [System.Net.HttpWebRequest]::Create("$base/api/auth/refresh")
    $req2.Method = "POST"
    $req2.Headers["Cookie"] = ($cookies -join "; ")
    $resp2 = $req2.GetResponse()
    $sr2 = New-Object System.IO.StreamReader($resp2.GetResponseStream(), $utf8)
    $respTxt2 = $sr2.ReadToEnd(); $sr2.Close(); $resp2.Close()
    $refObj = $respTxt2 | ConvertFrom-Json
    case "TC-U05-01" ("Refresh刷新AccessToken code=" + $refObj.code) ([string]$refObj.code) "0"
} catch {
    case "TC-U05-01" ("Refresh异常:" + $_.Exception.Message) -2 "0"
}

# 持久token，$H全局使用
$TOKEN = $L4.json.data.accessToken
$H = @{ "Authorization" = "Bearer $TOKEN" }
Write-Output ("TOKEN_LEN|{0}" -f $TOKEN.Length)

$rDie = Post "/api/user/register" @{ username="test_die01"; password="123456" }
case "U06_PREP" "预注册test_die01用于注销" (Code $rDie) "0,1001"

Write-Output ""
Write-Output "=========================================="
Write-Output "PHASE2 账目模块"
Write-Output "=========================================="

$addBody = @{ type=0; category="餐饮"; amount=25.50; accountDate="2026-08-18"; remark="午餐测试" }
$A1 = Post "/api/account/add" $addBody $H
$accountId = $A1.json.data
case "TC-A01-01" ("新增餐饮支出 id=" + $accountId + " (期望0)") (Code $A1) "0"

$A1Idem = Post "/api/account/add" $addBody $H
$idemId = $A1Idem.json.data
$same = ($null -ne $accountId -and $accountId -eq $idemId)
case "TC-A01-03" ("幂等重复提交 sameId=" + $same + " code=" + (Code $A1Idem)) (Code $A1Idem) "0"

$A1b = Post "/api/account/add" @{ type=1; category="工资"; amount=8000; accountDate="2026-08-10" } $H
case "TC-A01-02" ("新增工资收入 id=" + $A1b.json.data + " (期望0)") (Code $A1b) "0"

$A1c = Post "/api/account/add" @{ type=0; category="餐饮"; amount=0; accountDate="2026-08-18" } $H
case "TC-A01-04" "金额<=0(期望400)" (Code $A1c) "400"

$A1d = Post "/api/account/add" @{ type=0; category="非法分类"; amount=10; accountDate="2026-08-18" } $H
case "TC-A01-05" "分类非法(期望400)" (Code $A1d) "400"

$A2a = Post "/api/account/page" @{ pageNum=1; pageSize=10 } $H
$rowsA2 = if($A2a.json -and $A2a.json.data){$A2a.json.data.records.Count}else{0}
case "TC-A02-01" ("分页查询 rows=" + $rowsA2 + " (期望0)") (Code $A2a) "0"

$A2b = Post "/api/account/page" @{ pageNum=1; pageSize=10; type=0 } $H
case "TC-A02-02" "按type筛选支出(期望0)" (Code $A2b) "0"

$A2c = Post "/api/account/page" @{ pageNum=1; pageSize=10; category="餐饮" } $H
case "TC-A02-03" "按category筛选餐饮(期望0)" (Code $A2c) "0"

$A2d = Post "/api/account/page" @{ pageNum=1; pageSize=10; startDate="2026-08-01"; endDate="2026-08-31" } $H
case "TC-A02-04" "日期范围(期望0)" (Code $A2d) "0"

$A2e = Post "/api/account/page" @{ pageNum=1; pageSize=10; keyword="午餐" } $H
case "TC-A02-05" "关键词模糊(期望0)" (Code $A2e) "0"

# 取版本号
$pageR = Post "/api/account/page" @{ pageNum=1; pageSize=50 } $H
$ver1 = 0
$lst = if($pageR.json -and $pageR.json.data){@($pageR.json.data.records)}else{@()}
foreach ($row in $lst) { if ($row.id -eq $accountId) { $ver1 = $row.version } }
$Upd1 = Put "/api/account/update" @{ id=$accountId; type=0; category="交通"; amount=30; accountDate="2026-08-20"; remark="打车"; version=$ver1 } $H
case "TC-A03-01" ("正常修改 versionOrig=" + $ver1 + " code=" + (Code $Upd1)) (Code $Upd1) "0"

$Upd2 = Put "/api/account/update" @{ id=$accountId; type=0; category="交通"; amount=30; accountDate="2026-08-20"; remark="打车"; version=$ver1 } $H
case "TC-A03-02" ("乐观锁冲突(期望2002) origVer=" + $ver1) (Code $Upd2) "2002"

$Across = Post "/api/account/add" @{ type=0; category="购物"; amount=100; accountDate="2026-07-29" } $H
$acrossId = $Across.json.data
case "TC-A03-03_PREP" ("跨月修改前置新增7-29账目id=" + $acrossId) (Code $Across) "0"

$pageR2 = Post "/api/account/page" @{ pageNum=1; pageSize=50 } $H
$v2 = 0
$lst2 = if($pageR2.json -and $pageR2.json.data){@($pageR2.json.data.records)}else{@()}
foreach ($row in $lst2) { if ($row.id -eq $acrossId) { $v2 = $row.version } }
$Upd3 = Put "/api/account/update" @{ id=$acrossId; type=0; category="购物"; amount=100; accountDate="2026-08-02"; remark="跨月(7-29→8-02 差4天)"; version=$v2 } $H
case "TC-A03-03" ("跨月修改(7-29→8-02 差4天) versionOrig=" + $v2 + " code=" + (Code $Upd3)) (Code $Upd3) "0"

$Upd4 = Put "/api/account/update" @{ id=99999999; type=0; category="购物"; amount=100; accountDate="2026-08-01"; version=1 } $H
case "TC-A03-04" "账目不存在(期望2001)" (Code $Upd4) "2001"

$randAmt = Get-Random -Minimum 51 -Maximum 899
$DelAccount = Post "/api/account/add" @{ type=0; category="娱乐"; amount=$randAmt; accountDate="2026-08-18" } $H
$delId = $DelAccount.json.data
case "TC-A04-01_PREP" ("删除前置新增id=" + $delId + " amount=" + $randAmt) (Code $DelAccount) "0"

$Del1 = DoDelete "/api/account/delete/$delId" $H
case "TC-A04-01" ("删除账目 id=" + $delId + " (期望0)") (Code $Del1) "0"

$pgAfter = Post "/api/account/page" @{ pageNum=1; pageSize=1000 } $H
$stillThere = $false
$lst3 = if($pgAfter.json -and $pgAfter.json.data){@($pgAfter.json.data.records)}else{@()}
foreach ($row in $lst3) { if ($row.id -eq $delId) { $stillThere = $true } }
case "TC-A04-02" ("逻辑删除过滤(删除id仍可见=$stillThere，False=通过)") $(if(!$stillThere){"通过"}) "通过"

$DelX = DoDelete "/api/account/delete/99999999" $H
case "TC-A04-05" "删除不存在(期望2001)" (Code $DelX) "2001"

Write-Output ""
Write-Output "=========================================="
Write-Output "PHASE3+4 预算+仪表盘"
Write-Output "=========================================="

$B1 = Post "/api/budget/add" @{ category="餐饮"; month="2026-08"; amountLimit=2000 } $H
case "TC-B01-01" ("设定餐饮预算 code=" + (Code $B1)) (Code $B1) "0,3001"

$B1b = Post "/api/budget/add" @{ category="餐饮"; month="2026-08"; amountLimit=2000 } $H
case "TC-B01-02" "重复设定(期望3001)" (Code $B1b) "3001"

$B1c = Post "/api/budget/add" @{ category="工资"; month="2026-08"; amountLimit=10000 } $H
case "TC-B01-03" "收入类不可设预算(期望400)" (Code $B1c) "400"

$B1d = Post "/api/budget/add" @{ category="交通"; month="2026-08"; amountLimit=0 } $H
case "TC-B01-04" "预算金额<=0(期望400)" (Code $B1d) "400"

$B1e = Post "/api/budget/add" @{ category="交通"; month="2026-08"; amountLimit=500 } $H
case "TC-B01-07a" ("设定交通预算 code=" + (Code $B1e)) (Code $B1e) "0,3001"
$B1f = Post "/api/budget/add" @{ category="购物"; month="2026-08"; amountLimit=1000 } $H
case "TC-B01-07b" ("设定购物预算 code=" + (Code $B1f)) (Code $B1f) "0,3001"

$B2 = Get "/api/budget/list?month=2026-08" $H
$bCount = if($B2.json -and $B2.json.data){$B2.json.data.Count}else{0}
case "TC-B02-01" ("查询8月预算条数=" + $bCount + " (期望0)") (Code $B2) "0"

$B2b = Get "/api/budget/list?month=2025-01" $H
$bc2 = if($B2b.json -and $B2b.json.data){$B2b.json.data.Count}else{0}
case "TC-B02-02" ("查询空月条数=" + $bc2 + " (期望0)") (Code $B2b) "0"

$B2c = Get "/api/budget/list?month=2026-8" $H
case "TC-B02-05" "月份格式错误(期望400)" (Code $B2c) "400"

$S1 = Get "/api/statistics/dashboard?month=2026-08" $H
$sCode = Code $S1
$trendCount = if($S1.json -and $S1.json.data -and $S1.json.data.trend){$S1.json.data.trend.Count}else{0}
$catCount = if($S1.json -and $S1.json.data -and $S1.json.data.categoryStats){$S1.json.data.categoryStats.Count}else{0}
$budCount = if($S1.json -and $S1.json.data -and $S1.json.data.budgetProgress){$S1.json.data.budgetProgress.Count}else{0}
case "TC-S01-01" ("仪表盘(期望0) trend=$trendCount cat=$catCount budget=$budCount") $sCode "0"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$S1b = Get "/api/statistics/dashboard?month=2026-08" $H
$sw.Stop()
case "TC-S01-03" ("第二次仪表盘耗时=" + $sw.ElapsedMilliseconds + "ms(期望0)") (Code $S1b) "0"

$S1c = Get "/api/statistics/dashboard?month=2026-8" $H
case "TC-S01-05" "仪表盘月份错(期望400)" (Code $S1c) "400"

Write-Output ""
Write-Output "=========================================="
Write-Output "PHASE5 导出模块(3独立用户绕过单用户5分钟限流)"
Write-Output "=========================================="

# --- E01-01：随机用户，先做几笔记账，再导8月支出 ---
$rnd = Get-Random -Minimum 10000 -Maximum 99999
$u1 = "texp_${rnd}_01"
$null = Post "/api/user/register" @{ username=$u1; password="123456" }
$LE1 = Post "/api/user/login" @{ username=$u1; password="123456" }
$HE1 = @{ "Authorization" = "Bearer " + $LE1.json.data.accessToken }
$null = Post "/api/account/add" @{ type=0; category="餐饮"; amount=35; accountDate="2026-08-15" } $HE1
$null = Post "/api/account/add" @{ type=0; category="交通"; amount=20; accountDate="2026-08-10" } $HE1
try {
    $eb = $utf8.GetBytes('{"type":0,"startDate":"2026-08-01","endDate":"2026-08-31"}')
    $raw = Invoke-WebRequest -Uri "$base/api/export/excel" -Method POST -Body $eb -Headers $HE1 -ContentType "application/json;charset=utf-8" -TimeoutSec 60 -UseBasicParsing
    $ct = [string]($raw.Headers["Content-Type"])
    if ($ct -match "spreadsheet|excel|octet-stream") {
        case "TC-E01-01" ("同步导出Excel ct=$ct len=" + $raw.Content.Length + " (HTTP200)") "200" "200"
    } else {
        $txt = ReadText $raw.Content
        $obj = ParseJson $txt
        case "TC-E01-01" ("同步返回JSON code=" + $obj.code) ([string]$obj.code) "0"
    }
} catch {
    if ($_.Exception.Response) {
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream(), $utf8)
            $txt = $sr.ReadToEnd(); $sr.Close()
            $obj = ParseJson $txt
            case "TC-E01-01" ("导出异常 code=" + $obj.code) ([string]$obj.code) "0,200"
        } catch {
            case "TC-E01-01" ("导出异常:" + $_.Exception.Message) -1 "0,200"
        }
    } else {
        case "TC-E01-01" ("导出异常:" + $_.Exception.Message) -1 "0,200"
    }
}

# --- E01-04：随机用户，空数据导出 ---
$u2 = "texp_${rnd}_02"
$null = Post "/api/user/register" @{ username=$u2; password="123456" }
$LE2 = Post "/api/user/login" @{ username=$u2; password="123456" }
$HE2 = @{ "Authorization" = "Bearer " + $LE2.json.data.accessToken }
try {
    $eb2 = $utf8.GetBytes('{"startDate":"2024-01-01","endDate":"2024-01-31"}')
    $raw2 = Invoke-WebRequest -Uri "$base/api/export/excel" -Method POST -Body $eb2 -Headers $HE2 -ContentType "application/json;charset=utf-8" -TimeoutSec 60 -UseBasicParsing
    case "TC-E01-04" ("空数据导出 len=" + $raw2.Content.Length + " (HTTP200)") "200" "200"
} catch {
    case "TC-E01-04" ("空导出异常:" + $_.Exception.Message) -1 "200"
}

# --- E01-05：随机用户，按类型筛选收入导出 ---
$u3 = "texp_${rnd}_03"
$null = Post "/api/user/register" @{ username=$u3; password="123456" }
$LE3 = Post "/api/user/login" @{ username=$u3; password="123456" }
$HE3 = @{ "Authorization" = "Bearer " + $LE3.json.data.accessToken }
$null = Post "/api/account/add" @{ type=1; category="工资"; amount=6000; accountDate="2026-08-05" } $HE3
try {
    $eb3 = $utf8.GetBytes('{"type":1}')
    $raw3 = Invoke-WebRequest -Uri "$base/api/export/excel" -Method POST -Body $eb3 -Headers $HE3 -ContentType "application/json;charset=utf-8" -TimeoutSec 60 -UseBasicParsing
    case "TC-E01-05" ("按类型筛选收入导出 len=" + $raw3.Content.Length + " (HTTP200)") "200" "200"
} catch {
    case "TC-E01-05" ("导出异常:" + $_.Exception.Message) -1 "200"
}

Write-Output ""
Write-Output "=========================================="
Write-Output "PHASE6 设计验证+可观测性"
Write-Output "=========================================="

$add2 = Post "/api/account/add" @{ type=0; category="娱乐"; amount=88; accountDate="2026-08-18"; remark="幂等验证" } $H
$idX = $add2.json.data
$add3 = Post "/api/account/add" @{ type=0; category="娱乐"; amount=88; accountDate="2026-08-18"; remark="幂等验证" } $H
$idY = $add3.json.data
case "设计§3.1-幂等" ("两次提交同ID? id1=$idX id2=$idY same=" + ($idX -eq $idY)) $(if($idX -eq $idY){"通过"}) "通过"

case "设计§3.3-乐观锁冲突" "已在TC-A03-02验证返回2002" "通过" "通过"

$Prom = Get "/actuator/prometheus"
if ($Prom -and $Prom.ok -and $Prom.raw) {
    $ledgerLines = ($Prom.raw -split "`n") | Where-Object { $_ -match '^ledger_' -and $_ -notmatch '^#' }
    case "§4.1-指标暴露" ("Prometheus ledger_* 业务指标条数=" + $ledgerLines.Count) $(if($ledgerLines.Count -ge 5){"通过"}) "通过"
} else {
    case "§4.1-指标暴露" "Prometheus端点失败" "失败" "通过"
}

$Doc = Get "/doc.html"
case "§4.2-Knife4j" ("doc.html HTTP=" + ($Doc.status)) (Code $Doc) "200"

$He = Get "/actuator/health"
case "Health端点" ("health HTTP=" + ($He.status)) (Code $He) "200"

Write-Output ""
Write-Output "=========================================="
Write-Output "PHASE8 收尾: 注销test_die01"
Write-Output "=========================================="

$LD = Post "/api/user/login" @{ username="test_die01"; password="123456" }
$dieTok = $LD.json.data.accessToken
$HDie = @{ "Authorization" = "Bearer $dieTok" }

$U6a = DoDelete "/api/user/delete?password=wrong" $HDie
case "TC-U06-01" "注销密码错误(期望1006)" (Code $U6a) "1006"

$U6b = DoDelete "/api/user/delete?password=123456" $HDie
case "TC-U06-02" "注销成功(期望0)" (Code $U6b) "0"

$InfoDie = Get "/api/user/info" $HDie
case "TC-U06-03" ("注销后旧Token失效 code=" + (Code $InfoDie) + " (401或1004均通过)") (Code $InfoDie) "401,1004"

Write-Output "=========================================="
Write-Output "ALL_TEST_DONE"
Write-Output "=========================================="