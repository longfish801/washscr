/*
 * WashMakerSpec.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.tpac.element.TeaDec;
import io.github.longfish801.tpac.element.TeaHandle;
import io.github.longfish801.tpac.element.TpacDec;
import io.github.longfish801.tpac.element.TpacHandle;
import io.github.longfish801.tpac.parser.TeaMakerMakeException;
import spock.lang.Shared;
import spock.lang.Specification;

/**
 * WashMakerのテスト。
 * @version 1.0.00 2018/09/22
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashMakerSpec extends Specification {
	/** WashMaker */
	@Shared WashMaker maker;
	
	def setup(){
		maker = new WashMaker();
	}
	
	def 'TeaDecインスタンスを生成します。'(){
		given:
		TeaDec dec;
		
		when:
		dec = maker.newTeaDec('washsh', '');
		then:
		dec instanceof Washsh;
	}
	
	def 'TeaHandleインスタンスを生成します。'(){
		given:
		TeaHandle handle;
		TeaDec dec = new TpacDec();
		dec.tag = 'washsh';
		TeaHandle hndlRange = new TpacHandle();
		hndlRange.tag = 'range';
		TeaHandle hndlFormat = new TpacHandle();
		hndlFormat.tag = 'format';
		TeaMakerMakeException exc;
		
		when:
		handle = maker.newTeaHandle('range', '', dec);
		then:
		handle instanceof WashRange;
		
		when:
		handle = maker.newTeaHandle('format', '', dec);
		then:
		handle instanceof WashFormat;
		
		when:
		handle = maker.newTeaHandle('mask', '', hndlRange);
		then:
		handle instanceof WashRange.WashMask;
		
		when:
		handle = maker.newTeaHandle('divided', '', hndlRange);
		then:
		handle instanceof WashRange.WashDivided;
		
		when:
		handle = maker.newTeaHandle('enclosed', '', hndlRange);
		then:
		handle instanceof WashRange.WashEnclosed;
		
		when:
		handle = maker.newTeaHandle('tree', '', hndlRange);
		then:
		handle instanceof WashRange.WashTree;
		
		when:
		handle = maker.newTeaHandle('replace', '', hndlFormat);
		then:
		handle instanceof WashFormat.WashReplace;
		
		when:
		handle = maker.newTeaHandle('reprex', '', hndlFormat);
		then:
		handle instanceof WashFormat.WashReprex;
		
		when:
		handle = maker.newTeaHandle('call', '', hndlFormat);
		then:
		handle instanceof WashFormat.WashCall;
		
		when:
		handle = maker.newTeaHandle('nosuchtag', '', dec);
		then:
		exc = thrown(TeaMakerMakeException);
		exc.message == '不正なタグ名です。tag=nosuchtag, name=';
		
		when:
		handle = maker.newTeaHandle('call', '', dec);
		then:
		exc = thrown(TeaMakerMakeException);
		exc.message == 'タグの親子関係が不正です。tag=call, name=, upper=washsh';
	}
}
