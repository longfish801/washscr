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
	def 'BufferedReaderから読みこんだ文字列を範囲別に分類します（mask）。'(){
		given:
		WashRange range = new WashRange();
		range.tag = 'range';
		range.name = '';
		WashRange.WashMask mask = range.newInstanceMask();
		mask.tag = 'mask'
		mask.name = ''
		mask.map.bgn = '／＊';
		mask.map.end = '＊／';
		range << mask;
		range.validateBasic();
		String target = '''\
			Hello, World.
			／＊
			Hello, WashRange.
			＊／
			Hello, Groovy.
			'''.stripIndent();
		List lines = [];
		target.split(/\r\n|[\n\r]/).each { lines << [ 'line' : it, 'kind' : ((it.empty)? 'empty' : '') ] }
		TagText.Node node = new TagText().newInstanceNode('');
		
		when:
		lines = range.kindof(lines);
		then:
		lines.collect { it.kind } == [ '', 'masked#bgn', '', 'masked#end', '' ];
		
		when:
		range.taggingSetup(lines);
		range.tagging(lines, node, 0, lines.size() - 1);
		then:
		node.lowers.size() == 3;
		node.lowers[0].lines == [ 'Hello, World.' ];
		node.lowers[1].lowers.size() == 1;
		node.lowers[1].bgn == null;
		node.lowers[1].end == null;
		node.lowers[1].lowers[0].lines == [ 'Hello, WashRange.' ];
		node.lowers[2].lines == [ 'Hello, Groovy.' ];
	}
	
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
		String target = '''\
			Hello, World.
			－－－
			Hello, WashRange.
			－－－
			Hello, Groovy.
			'''.stripIndent();
		List lines = [];
		target.split(/\r\n|[\n\r]/).each { lines << [ 'line' : it, 'kind' : ((it.empty)? 'empty' : '') ] }
		TagText.Node node = new TagText().newInstanceNode('');
		
		when:
		lines = range.kindof(lines);
		then:
		lines.collect { it.kind } == [ '', '囲み記事#div', '', '囲み記事#div', '' ];
		
		when:
		range.taggingSetup(lines);
		range.tagging(lines, node, 0, lines.size() - 1);
		then:
		node.lowers.size() == 3;
		node.lowers[0].lines == [ 'Hello, World.' ];
		node.lowers[1].lowers.size() == 1;
		node.lowers[1].bgn == '－－－';
		node.lowers[1].end == '－－－';
		node.lowers[1].lowers[0].lines == [ 'Hello, WashRange.' ];
		node.lowers[2].lines == [ 'Hello, Groovy.' ];
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
		String target = '''\
			Hello, World.
			【ここから】
			Hello, WashRange.
			【ここまで】
			Hello, Groovy.
			'''.stripIndent();
		List lines = [];
		target.split(/\r\n|[\n\r]/).each { lines << [ 'line' : it, 'kind' : ((it.empty)? 'empty' : '') ] }
		TagText.Node node = new TagText().newInstanceNode('');
		
		when:
		lines = range.kindof(lines);
		then:
		lines.collect { it.kind } == [ '', 'コラム#bgn', '', 'コラム#end', '' ];
		
		when:
		range.taggingSetup(lines);
		range.tagging(lines, node, 0, lines.size() - 1);
		then:
		node.lowers.size() == 3;
		node.lowers[0].lines == [ 'Hello, World.' ];
		node.lowers[1].lowers.size() == 1;
		node.lowers[1].bgn == '【ここから】';
		node.lowers[1].end == '【ここまで】';
		node.lowers[1].lowers[0].lines == [ 'Hello, WashRange.' ];
		node.lowers[2].lines == [ 'Hello, Groovy.' ];
	}
	
	def 'BufferedReaderから読みこんだ文字列を行毎に分類します（tree）。'(){
		given:
		WashRange range = new WashRange();
		range.tag = 'range';
		range.name = '';
		WashRange.WashTree tree = range.newInstanceTree();
		tree.tag = 'tree';
		tree.name = '箇条書き'
		tree.map.level = '''\
			import java.util.regex.Matcher;
			{ String line ->
				if (!(line ==~ /(\t*)・.*/)) return -1;
				return Matcher.getLastMatcher().group(1).length();
			}
			'''.stripIndent().split(/\r\n|[\n\r]/) as TpacText;
		tree.map.escape = '''\
			{ int idx, int level ->
				if (idx == lines.size() - 1) return true;
				if (lines[idx].kind != 'empty') return false;
				if (lines[idx + 1].kind != '箇条書き#bgn') return true;
				return (lines[idx + 1].kind == '箇条書き#bgn' || lines[idx + 1].level < level);
			}
			'''.stripIndent().split(/\r\n|[\n\r]/) as TpacText;
		range << tree;
		range.validateBasic();
		String target = '''\
			Hello, World.
			・Hello, WashRange.
			
			Hello, Groovy.
			'''.stripIndent();
		List lines = [];
		target.split(/\r\n|[\n\r]/).each { lines << [ 'line' : it, 'kind' : ((it.empty)? 'empty' : '') ] }
		TagText.Node node = new TagText().newInstanceNode('');
		
		when:
		lines = range.kindof(lines);
		then:
		lines.collect { it.kind } == [ '', '箇条書き#bgn', '', 'empty', '' ];
		lines.collect { it.level } == [ null, 0, null, null, null ];
		
		when:
		range.taggingSetup(lines);
		range.tagging(lines, node, 0, lines.size() - 1);
		then:
		node.lowers.size() == 3;
		node.lowers[0].lines == [ 'Hello, World.' ];
		node.lowers[1].lowers.size() == 1;
		node.lowers[1].bgn == null;
		node.lowers[1].end == null;
		node.lowers[1].lowers[0].lines == [ '・Hello, WashRange.', '' ];
		node.lowers[2].lines == [ 'Hello, Groovy.' ];
	}
}
