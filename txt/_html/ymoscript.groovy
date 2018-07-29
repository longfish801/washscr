/*
 * ymoscript.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */

ymoDocument.script {
	// 出力先フォルダ
	File outDir = new File(targetDir, '../../longfish801.github.io/gitdoc/washsh');
	
	// 設定を読みこむ
	configure('_bltxt', '_html');
	configure(conversionDir);
	
	// 出力先を設定する
	setIO(outDir, '.html');
	
	// 固定ファイルをコピーする
	assetHandler.setup(outDir, 'overwrite');
	assetHandler.remove('_html');
	assetHandler.gulp('common', find(targetDir, [], ['*.txt', '_*']));
	doLast {
		assetHandler.copy();
	}
}
