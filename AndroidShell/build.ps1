param(
    [Parameter(Mandatory = $true)] [string] $BaseApk,
    [Parameter(Mandatory = $true)] [string] $ApktoolJar,
    [Parameter(Mandatory = $true)] [string] $OutputApk,
    [string] $Java = "java"
)

$ErrorActionPreference = "Stop"
$scriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$work = Join-Path ([System.IO.Path]::GetTempPath()) ("xivchat-aetherphone-" + [Guid]::NewGuid().ToString("N"))
$decoded = Join-Path $work "decoded"
$unsigned = Join-Path $work "unsigned.apk"

New-Item -ItemType Directory -Path $work | Out-Null
try {
    & $Java -jar $ApktoolJar d $BaseApk -o $decoded -f

    $apktoolYml = Join-Path $decoded "apktool.yml"
    $apktoolText = Get-Content -Raw $apktoolYml
    $apktoolText = [regex]::Replace($apktoolText, '(?m)^(\s*versionCode:\s*).+$', '${1}100')
    $apktoolText = [regex]::Replace($apktoolText, '(?m)^(\s*versionName:\s*).+$', '${1}0.1.0')
    [IO.File]::WriteAllText($apktoolYml, $apktoolText)

    Copy-Item (Join-Path $scriptRoot "home_shell.xml") (Join-Path $decoded "res\layout\home_shell.xml")
    Copy-Item (Join-Path $scriptRoot "values\strings_aetherphone.xml") (Join-Path $decoded "res\values\strings_aetherphone.xml")
    Get-ChildItem (Join-Path $scriptRoot "drawable") -Filter *.xml | ForEach-Object {
        Copy-Item $_.FullName (Join-Path $decoded "res\drawable\$($_.Name)")
    }

    $activity = Join-Path $decoded "res\layout\activity_main.xml"
    $activityText = Get-Content -Raw $activity
    $activityText = $activityText.Replace(
        '<include android:layout_width="match_parent" android:layout_height="match_parent" layout="@layout/app_bar_main" />',
        '<FrameLayout android:layout_width="match_parent" android:layout_height="match_parent"><include android:layout_width="match_parent" android:layout_height="match_parent" layout="@layout/app_bar_main" /><include android:layout_width="match_parent" android:layout_height="match_parent" layout="@layout/home_shell" /></FrameLayout>')
    [IO.File]::WriteAllText($activity, $activityText)

    $navigation = Join-Path $decoded "res\navigation\mobile_navigation.xml"
    $navigationText = (Get-Content -Raw $navigation).Replace('app:startDestination="@id/nav_servers"', 'app:startDestination="@id/nav_messages_tabs"')
    [IO.File]::WriteAllText($navigation, $navigationText)

    $manifest = Join-Path $decoded "AndroidManifest.xml"
    $manifestText = Get-Content -Raw $manifest
    $manifestText = $manifestText.Replace('package="io.annaclemens.xivchat"', 'package="io.annaclemens.xivchat.aetherphone"')
    $manifestText = $manifestText.Replace('android:label="@string/app_name"', 'android:label="@string/app_name_aetherphone"')
    $manifestText = $manifestText.Replace('io.annaclemens.xivchat.SentryInitProvider', 'io.annaclemens.xivchat.aetherphone.SentryInitProvider')
    $manifestText = $manifestText.Replace('io.annaclemens.xivchat.lifecycle-process', 'io.annaclemens.xivchat.aetherphone.lifecycle-process')
    [IO.File]::WriteAllText($manifest, $manifestText)

    $rId = Join-Path $decoded "smali\io\annaclemens\xivchat\R`$id.smali"
    $rIdText = Get-Content -Raw $rId
    $idValues = [regex]::Matches($rIdText, '0x7f0a[0-9a-fA-F]{4}') | ForEach-Object { [Convert]::ToInt32($_.Value.Substring(2), 16) }
    $homeId = [int](($idValues | Measure-Object -Maximum).Maximum + 1)
    $inventoryId = $homeId + 1
    $homeHex = ('0x{0:x8}' -f $homeId)
    $inventoryHex = ('0x{0:x8}' -f $inventoryId)
    $previousField = ".field public static final zero_corner_chip:I = 0x$('{0:x4}' -f ($homeId - 1))"
    $rIdText = $rIdText.Replace($previousField, "$previousField`n`n.field public static final home_shell:I = $homeHex`n.field public static final inventory_summary:I = $inventoryHex")
    [IO.File]::WriteAllText($rId, $rIdText)

    $main = Join-Path $decoded "smali\io\annaclemens\xivchat\MainActivity.smali"
    Add-Content -Path $main -Value @"

.method public final openXivchat(Landroid/view/View;)V
    .locals 2
    const-string v0, "view"
    invoke-static {p1, v0}, Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V
    sget v0, Lio/annaclemens/xivchat/R`$id;->home_shell:I
    invoke-virtual {p0, v0}, Lio/annaclemens/xivchat/MainActivity;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-eqz v0, :cond_0
    const/16 v1, 0x8
    invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
    :cond_0
    return-void
.end method

.method public final openInventory(Landroid/view/View;)V
    .locals 0
    return-void
.end method

.method public final updateInventorySummary(II)V
    .locals 4
    sget v0, Lio/annaclemens/xivchat/R`$id;->inventory_summary:I
    invoke-virtual {p0, v0}, Lio/annaclemens/xivchat/MainActivity;->findViewById(I)Landroid/view/View;
    move-result-object v0
    if-eqz v0, :done
    check-cast v0, Landroid/widget/TextView;
    new-instance v1, Ljava/lang/StringBuilder;
    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
    const-string v2, "\u80cc\u5305 "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1, p1}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    const-string v2, " \u4e2a\u7269\u54c1 / \u603b\u8ba1 "
    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    invoke-virtual {v1, p2}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;
    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v0, v1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V
    :done
    return-void
.end method
"@

    $smaliOverlay = Join-Path $scriptRoot "smali"
    Copy-Item -Path (Join-Path $smaliOverlay "*") -Destination (Join-Path $decoded "smali") -Recurse -Force

    & $Java -jar $ApktoolJar b $decoded -o $unsigned -f
    Move-Item -LiteralPath $unsigned -Destination $OutputApk -Force
    Write-Host "Built unsigned APK: $OutputApk"
}
finally {
    if (Test-Path -LiteralPath $work) {
        Remove-Item -LiteralPath $work -Recurse -Force
    }
}
