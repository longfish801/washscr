/*
 * WashshSpec.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.PackageDirectory;
import io.github.longfish801.tpac.TpacServer;
import io.github.longfish801.tpac.element.TeaDec;
import spock.lang.Shared;
import spock.lang.Specification;
import spock.lang.Unroll;

/**
 * Washshクラスのテスト。
 * @version 1.0.00 2018/09/22
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashshSpec extends Specification {
	/** ファイル入出力のテスト用フォルダ */
	static final File testDir = PackageDirectory.deepDir('src/test/resources', WashshSpec.class);
	/** 試験用のスクリプト、対象文字列、期待文字列格納ハンドル */
	@Shared TeaDec dec;
	/** wash実行用クロージャ */
	@Shared Closure doWash;
	/** 期待文字列取得用クロージャ */
	@Shared Closure expected;
	
	def setup(){
		doWash = { String caseId ->
			WashServer server = new WashServer();
			server.soak(dec.lowers["case:${caseId}"].map.script.toString());
			return server["washsh:"].wash(dec.lowers["case:${caseId}"].map.target.toString());
		}
		expected = { String caseId ->
			return dec.lowers["case:${caseId}"].map.expected.toString();
		}
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（basic）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'basic.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'01'	| '最小限のスクリプト'
		'02'	| 'マスキング'
		'03'	| 'マスキング（正規表現指定）'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（replace）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'replace.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'01'	| '固定文字列での置換'
	}
}
