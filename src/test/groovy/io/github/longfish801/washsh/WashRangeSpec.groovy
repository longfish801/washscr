/*
 * WashRangeSpec.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh;

import groovy.util.logging.Slf4j;
import io.github.longfish801.shared.PackageDirectory;
import io.github.longfish801.tpac.element.TpacText;
import java.util.regex.Pattern;
import spock.lang.Specification;

/**
 * WashRangeクラスのテスト。
 * @version 1.0.00 2018/09/22
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashRangeSpec extends Specification {
	def 'BufferedReaderから読みこんだ文字列を範囲別に分類します（divide）。'(){
		given:
		WashRange range = new WashRange();
		range.tag = 'range';
		range.name = '';
		WashRange.WashDivided divided = range.newInstanceDivided();
		divided.tag = 'divided';
		divided.name = '囲み記事'
		divided.map.div = '－－－'
		range << divided;
		range.validateBasic();
		TagLines tagLines = new TagLines();
		List list;
		String target = '''\
			Hello, World.
			－－－
			Hello, WashRange.
			－－－
			Hello, Groovy.
			'''.stripIndent();
		
		when:
		target.split(/\r\n|[\n\r]/).each { String line -> tagLines.append(line, range.kindof(line))  }
		then:
		tagLines.kinds == [ 'plain', '囲み記事#div', 'plain', '囲み記事#div', 'plain' ];
		
		when:
		tagLines.lines.eachWithIndex { String line, int idx -> tagLines.appendTag(idx, null) }
		range.tagging(tagLines);
		then:
		tagLines.tags == [ [], [ '囲み記事#bgn' ], [ '囲み記事' ], [ '囲み記事#end' ], [] ];
	}
	
	def 'BufferedReaderから読みこんだ文字列を範囲別に分類します（enclosed）。'(){
		given:
		WashRange range = new WashRange();
		range.tag = 'range';
		range.name = '';
		WashRange.WashEnclosed enclosed = range.newInstanceEnclosed();
		enclosed.tag = 'enclosed';
		enclosed.name = 'コラム'
		enclosed.map.bgn = '【ここから】'
		enclosed.map.end = '【ここまで】'
		range << enclosed;
		range.validateBasic();
		TagLines tagLines = new TagLines();
		List list;
		String target = '''\
			Hello, World.
			【ここから】
			Hello, WashRange.
			【ここまで】
			Hello, Groovy.
			'''.stripIndent();
		
		when:
		target.split(/\r\n|[\n\r]/).each { String line -> tagLines.append(line, range.kindof(line))  }
		then:
		tagLines.kinds == [ 'plain', 'コラム#bgn', 'plain', 'コラム#end', 'plain' ];
		
		when:
		tagLines.lines.eachWithIndex { String line, int idx -> tagLines.appendTag(idx, null) }
		range.tagging(tagLines);
		then:
		tagLines.tags == [ [], [ 'コラム#bgn' ], [ 'コラム' ], [ 'コラム#end' ], [] ];
	}
	
	def 'BufferedReaderから読みこんだ文字列を行毎に分類します（complex）。'(){
		given:
		WashRange range = new WashRange();
		range.tag = 'range';
		range.name = '';
		WashRange.WashComplex complex = range.newInstanceComplex();
		complex.tag = 'complex';
		complex.name = '箇条書き'
		complex.map.kindof = [ '箇条書き開始': Pattern.compile(/・.+/) ];
		complex.map.enter = [ "{ int idx -> return (kinds[idx] == '箇条書き開始') }" ] as TpacText;
		complex.map.escape = [ "{ int idx -> return (kinds[idx] == 'plain') }" ] as TpacText;
		range << complex;
		range.validateBasic();
		TagLines tagLines = new TagLines();
		List list;
		String target = '''\
			Hello, World.
			・Hello, WashRange.
			Hello, Groovy.
			'''.stripIndent();
		
		when:
		target.split(/\r\n|[\n\r]/).each { String line -> tagLines.append(line, range.kindof(line))  }
		then:
		tagLines.kinds == [ 'plain', '箇条書き開始', 'plain' ];
		
		when:
		tagLines.lines.eachWithIndex { String line, int idx -> tagLines.appendTag(idx, null) }
		range.tagging(tagLines);
		then:
		tagLines.tags == [ [], [ '箇条書き' ], [] ];
	}
}
