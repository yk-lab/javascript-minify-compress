javascript-minify-compress
==========================

「Javascript Minify & Compress （jsmc）」は、作成、変更したJavascriptをリアルタイムで最小化し、gzip形式で圧縮します。
例えるなら、Javascriptのコンパイラーです。

# WebPage
[Japanese](http://www.yk-lab.net/tools/%E3%80%90jsmc%E3%80%91javascript-minify-compress/)  
[English](http://www.yk-lab.net/en/tools/%E3%80%90jsmc%E3%80%91javascript-minify-compress_en/)  

# 使用例
`java -jar jsmc.jar –watch js-src:js`  
ディレクトリ「js-src」にある「.js」ファイルを、minifyし、ディレクトリ「js」に出力します。  その出力したファイルをgzip圧縮し、再度保存します。

    js-src  
    |- test.js
↓

    js
    |- test.js
    |- test.js.gz

## 解説
`java -jar jsmc.jar [options] from:to`  
`jsmc.exe [options] from:to`

from: 入力元のディレクトリ
to: 出力先のディレクトリ

### Option
`--watch`:入力元のディレクトリを監視し変更があればminify、gzipします。
