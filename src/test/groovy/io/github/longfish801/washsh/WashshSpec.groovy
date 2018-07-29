/*
 * WashshSpec.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.util.ClassDirectory;
import io.github.longfish801.clmap.Clinfo.ClinfoCallException;
import spock.lang.Specification;
import spock.lang.Timeout;
import spock.lang.Unroll;

/**
 * Washshクラスのテスト。
 * @version 1.0.00 2017/07/28
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashshSpec extends Specification {
	/** ファイル入出力のテスト用フォルダ */
	private static final File testDir = new ClassDirectory('src/test/resources').getDeepDir(WashshSpec.class);
	
	@Timeout(10)
	@Unroll
	def 'sliceタグに沿って文字列を変換します'(){
		expect:
		new Washsh(new File(testDir, "${dir}/${script}scr.tpac")).wash(new File(testDir, "${dir}/${fname}tgt.txt")) == new File(testDir, "${dir}/${fname}exp.txt").getText();
		
		where:
		dir		| script	| fname
		'slice'	| 'slice01'	| 'slice0101'
		'slice'	| 'slice01'	| 'slice0102'
		'slice'	| 'slice01'	| 'slice0103'
		'slice'	| 'slice02'	| 'slice0201'
		'slice'	| 'slice02'	| 'slice0202'
		'slice'	| 'slice03'	| 'slice0301'
		'slice'	| 'slice03'	| 'slice0302'
		'slice'	| 'slice04'	| 'slice0401'
		'slice'	| 'slice04'	| 'slice0402'
	}
	
	@Timeout(10)
	@Unroll
	def 'betweenタグに沿って文字列を変換します'(){
		expect:
		new Washsh(new File(testDir, "${dir}/${script}scr.tpac")).wash(new File(testDir, "${dir}/${fname}tgt.txt")) == new File(testDir, "${dir}/${fname}exp.txt").getText();
		
		where:
		dir		| script	| fname
		'between'	| 'between01'	| 'between0101'
		'between'	| 'between01'	| 'between0102'
		'between'	| 'between01'	| 'between0103'
		'between'	| 'between02'	| 'between0201'
		'between'	| 'between02'	| 'between0202'
		'between'	| 'between03'	| 'between0301'
		'between'	| 'between03'	| 'between0302'
		'between'	| 'between04'	| 'between0401'
		'between'	| 'between04'	| 'between0402'
	}
	
	@Timeout(10)
	@Unroll
	def '処理タグに沿って文字列を変換します'(){
		expect:
		new Washsh(new File(testDir, "${dir}/${script}scr.tpac")).wash(new File(testDir, "${dir}/${fname}tgt.txt")) == new File(testDir, "${dir}/${fname}exp.txt").getText();
		
		where:
		dir		| script		| fname
		'child'	| 'replace01'	| 'replace0101'
		'child'	| 'replace02'	| 'replace0201'
		'child'	| 'replace03'	| 'replace0301'
		'child'	| 'reprex01'	| 'reprex0101'
		'child'	| 'reprex02'	| 'reprex0201'
		'child'	| 'reprex03'	| 'reprex0301'
		'child'	| 'call01'		| 'call0101'
		'child'	| 'call02'		| 'call0201'
		'child'	| 'call03'		| 'call0301'
	}
	
	@Timeout(10)
	@Unroll
	def '暗黙の引数configに沿って文字列を変換します'(){
		expect:
		new Washsh(new File(testDir, "${dir}/${script}scr.tpac")).wash(new File(testDir, "${dir}/${fname}tgt.txt")) == new File(testDir, "${dir}/${fname}exp.txt").getText();
		
		where:
		dir			| script		| fname
		'config'	| 'config01'	| 'config0101'
		'config'	| 'config02'	| 'config0201'
	}
	
	@Timeout(10)
	def 'クロージャ実行で例外発生時に処理を終了します'(){
		when:
		new Washsh(new File(testDir, 'exception/slice01scr.tpac')).wash('dummy');
		then:
		thrown(ClinfoCallException);
		
		when:
		new Washsh(new File(testDir, 'exception/call01scr.tpac')).wash('dummy');
		then:
		thrown(ClinfoCallException);
	}
}
