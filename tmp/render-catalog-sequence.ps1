Add-Type -AssemblyName System.Drawing
$bmp = New-Object System.Drawing.Bitmap 1600,1740
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.Clear([System.Drawing.Color]::White)
$g.SmoothingMode = 'AntiAlias'
$g.TextRenderingHint = 'AntiAliasGridFit'
$pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::Black),1.6
$dash = New-Object System.Drawing.Pen ([System.Drawing.Color]::Gray),1.3
$dash.DashStyle = 'Dash'
$font = New-Object System.Drawing.Font 'Arial',15
$small = New-Object System.Drawing.Font 'Arial',13
$bold = New-Object System.Drawing.Font 'Arial',15,([System.Drawing.FontStyle]::Bold)
$title = New-Object System.Drawing.Font 'Arial',24,([System.Drawing.FontStyle]::Bold)
function Txt($s,$x,$y,$f=$font) { $g.DrawString($s,$f,[System.Drawing.Brushes]::Black,[single]$x,[single]$y) }
function Line($x,$y,$xx,$yy,$p=$pen) { $g.DrawLine($p,[single]$x,[single]$y,[single]$xx,[single]$yy) }
function Box($x,$y,$w,$h) { $g.FillRectangle([System.Drawing.Brushes]::White,$x,$y,$w,$h); $g.DrawRectangle($pen,$x,$y,$w,$h) }
function Msg($x,$xx,$y,$s,$ret=$false) {
 $p = if($ret){$dash}else{$pen}
 Line $x $y $xx $y $p
 $d=if($xx -gt $x){-1}else{1}
 Line $xx $y ($xx+12*$d) ($y-6)
 Line $xx $y ($xx+12*$d) ($y+6)
 $size=$g.MeasureString($s,$small)
 Txt $s (($x+$xx-$size.Width)/2) ($y-25) $small
}
Txt 'Catalog Management - Sequence Diagram' 390 24 $title
Txt 'Precondition: Head Librarian is signed in. Successful operations shown.' 380 76 $small
$xs=@(150,450,790,1110,1420)
Txt 'Head Librarian' 79 118 $bold
$g.DrawEllipse($pen,138,150,24,24)
Line 150 174 150 210; Line 122 188 178 188; Line 150 210 127 236; Line 150 210 173 236
Box 325 159 250 56; Txt ':CatalogUI' 387 174 $bold
Box 655 159 270 56; Txt ':CatalogController' 676 174 $bold
Box 980 159 260 56; Txt ':BookRepository' 1001 174 $bold
Box 1310 159 220 56; Txt ':AuditService' 1335 174 $bold
foreach($x in $xs){Line $x 240 $x 1640 $dash}
$g.DrawRectangle($pen,40,264,1510,1350)
Box 40 264 78 34; Txt 'loop' 52 269 $bold
Txt '[For each catalog management request]' 135 270 $small
$g.DrawRectangle($pen,65,321,1460,1080)
Box 65 321 62 32; Txt 'alt' 78 325 $bold
Txt '[Add book]' 150 326 $bold
Box 444 365 12 120; Box 784 406 12 117; Box 1104 449 12 67
Msg 150 444 390 'Enter new book details'
Msg 456 784 435 'save(bookDetails)'
Msg 796 1104 480 'save(new active Book)'
Msg 1104 796 520 'savedBook' $true
Line 65 550 1525 550 $dash
Txt '[Edit book]' 150 559 $bold
Box 444 600 12 430; Box 784 638 12 408; Box 1104 680 12 78; Box 1104 895 12 143
Msg 150 444 617 'Select book to edit'
Msg 456 784 661 'edit(bookId)'
Msg 796 1104 704 'findById(bookId)'
Msg 1104 796 746 'book' $true
Msg 784 456 790 'Display populated book form' $true
Msg 150 444 835 'Submit updated details'
Msg 456 784 878 'save(bookDetails)'
Msg 796 1104 920 'findById(bookId)'
Msg 1104 796 960 'existingBook' $true
Msg 796 1104 1001 'save(updated active Book)'
Msg 1104 796 1041 'savedBook' $true
Line 65 1070 1525 1070 $dash
Txt '[Archive book]' 150 1079 $bold
Box 444 1119 12 233; Box 784 1159 12 228; Box 1104 1205 12 175
Msg 150 444 1140 'Confirm archive(bookId)'
Msg 456 784 1182 'delete(bookId)'
Msg 796 1104 1225 'findById(bookId)'
Msg 1104 796 1267 'book' $true
Line 796 1290 835 1290; Line 835 1290 835 1320; Msg 835 796 1320 ''
Txt 'setActive(false)' 845 1296 $small
Msg 796 1104 1345 'save(archived Book)'
Msg 1104 796 1385 'savedBook' $true
Box 784 1421 12 136; Box 1414 1434 12 62
Msg 796 1414 1451 'record(CREATE / UPDATE / ARCHIVE)'
Msg 1414 796 1492 'completed' $true
Msg 784 450 1541 'Redirect to /catalog + success message' $true
Msg 450 150 1590 'Show catalog and confirmation' $true
Txt 'BookRepository abstracts database access. AuditService persists the audit log.' 290 1663 $small
Txt 'Archive marks a book inactive; it does not remove the database record.' 330 1693 $small
$bmp.Save((Join-Path $PSScriptRoot '../output/catalog-management-sequence.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose(); $bmp.Dispose()
