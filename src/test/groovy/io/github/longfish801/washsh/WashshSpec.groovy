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
	/** washメソッドを実行するクロージャ */
	@Shared Closure doWash;
	/** 期待文字列を取得するクロージャ */
	@Shared Closure expected;
	
	def setup(){
		doWash = { String caseId, String comment ->
			LOG.debug('doWash {} {}', caseId, comment);
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
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| '最小限のスクリプト'
		'01'	| '全体の置換'
		'02'	| 'マスキング'
		'03'	| '範囲毎に異なる処理'
		'04'	| '範囲の入れ子'
		'05'	| '箇条書き'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（mask）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'rangeMask.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| '正規表現で指定'
		'01'	| 'endを省略かつ複数の範囲'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（divided）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'rangeDivided.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| '正規表現で指定かつ複数の範囲'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（enclosed）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'rangeEnclosed.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| '正規表現で指定かつ複数の範囲'
		'01'	| '入れ子'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（tree）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'rangeTree.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| '箇条書きと段落の混在'
		'01'	| 'clmapで指定'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（format）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'format.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'00'	| 'includeに複数指定'
		'01'	| 'excludeに複数指定'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（replace）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'formatReplace.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'01'	| '複数の検索語を置換'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（reprex）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'formatReprex.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'01'	| '複数の検索語を置換'
	}
	
	@Unroll
	def 'washsh記法に沿って文字列を変換します（call）。'(){
		given:
		dec = new TpacServer().soak(new File(testDir, 'formatCall.tpac')).getAt('tpac:');
		
		expect:
		doWash.call(caseNo, comment) == expected.call(caseNo);
		
		where:
		caseNo	| comment
		'01'	| 'テキストで指定'
		'02'	| 'clmapで指定'
	}
}
