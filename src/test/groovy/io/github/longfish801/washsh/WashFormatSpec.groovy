/*
 * WashFormatSpec.groovy
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
 * WashFormatクラスのテスト。
 * @version 1.0.00 2018/09/22
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class WashFormatSpec extends Specification {
	def 'タグ付きテキストを整形します（replace）。'(){
		given:
		WashFormat format = new WashFormat();
		format.tag = 'format';
		format.name = '';
		WashFormat.WashReplace replace = format.newInstanceReplace();
		replace.tag = 'replace';
		replace.name = '置換'
		replace.text << "Hello\tGoodbye";
		format << replace;
		format.validateBasic();
		TagText.Node node = new TagText().newInstanceNode('');
		node << node.newInstanceLeaf();
		node.lowers.last().lines = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		List expected = '''\
			Goodbye, World.
			Goodbye, WashFormat.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		
		when:
		format.apply(node);
		then:
		node.lowers.last().lines == expected;
	}
	
	def 'タグ付きテキストを整形します（reprex）。'(){
		given:
		WashFormat format = new WashFormat();
		format.tag = 'format';
		format.name = '';
		WashFormat.WashReprex reprex = format.newInstanceReprex();
		reprex.tag = 'reprex';
		reprex.name = '正規表現置換'
		reprex.text << /W(.+)	w$1/;
		format << reprex;
		format.validateBasic();
		TagText.Node node = new TagText().newInstanceNode('');
		node << node.newInstanceLeaf();
		node.lowers.last().lines = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		List expected = '''\
			Hello, world.
			Hello, washFormat.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		
		when:
		format.apply(node);
		then:
		node.lowers.last().lines == expected;
	}
	
	def 'タグ付きテキストを整形します（call）。'(){
		given:
		WashFormat format = new WashFormat();
		format.tag = 'format';
		format.name = '';
		WashFormat.WashCall call = format.newInstanceCall();
		call.tag = 'call';
		call.name = 'クロージャ呼出'
		call.map.text = [ '{ List lines -> return lines.collect { it.toUpperCase() } }' ] as TpacText;
		format << call;
		format.validateBasic();
		TagText.Node node = new TagText().newInstanceNode('');
		node << node.newInstanceLeaf();
		node.lowers.last().lines = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		List expected = '''\
			HELLO, WORLD.
			HELLO, WASHFORMAT.
			'''.stripIndent().split(/\r\n|[\n\r]/);
		
		when:
		format.apply(node);
		then:
		node.lowers.last().lines == expected;
	}
}
