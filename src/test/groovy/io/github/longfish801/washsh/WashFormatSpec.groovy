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
		TagText tagText = new TagText([]);
		tagText.text = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent();
		String expected = '''\
			Goodbye, World.
			Goodbye, WashFormat.
			'''.stripIndent();
		
		when:
		format.apply([ tagText ]);
		then:
		tagText.text == expected;
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
		TagText tagText = new TagText([]);
		tagText.text = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent();
		String expected = '''\
			Hello, world.
			Hello, washFormat.
			'''.stripIndent();
		
		when:
		format.apply([ tagText ]);
		then:
		tagText.text == expected;
	}
	
	def 'タグ付きテキストを整形します（call）。'(){
		given:
		WashFormat format = new WashFormat();
		format.tag = 'format';
		format.name = '';
		WashFormat.WashCall call = format.newInstanceCall();
		call.tag = 'call';
		call.name = 'クロージャ呼出'
		call.text << '{ String text -> return text.toUpperCase() }';
		format << call;
		format.validateBasic();
		TagText tagText = new TagText([]);
		tagText.text = '''\
			Hello, World.
			Hello, WashFormat.
			'''.stripIndent();
		String expected = '''\
			HELLO, WORLD.
			HELLO, WASHFORMAT.
			'''.stripIndent();
		
		when:
		format.apply([ tagText ]);
		then:
		tagText.text == expected;
	}
	
	def 'タグ付きテキストを整形します（delete）。'(){
		given:
		WashFormat format = new WashFormat();
		format.tag = 'format';
		format.name = '';
		WashFormat.WashDelete delete = format.newInstanceDelete();
		delete.tag = 'delete';
		delete.name = '削除';
		delete.map['include'] = [ 'コラム' ];
		format << delete;
		format.validateBasic();
		TagText tagText1 = new TagText([]);
		tagText1.text = '''\
			Hello, World.
			'''.stripIndent();
		TagText tagText2 = new TagText([ 'コラム' ]);
		tagText2.text = '''\
			Hello, WashFormat.
			'''.stripIndent();
		TagText tagText3 = new TagText([]);
		tagText3.text = '''\
			Hello, Groovy.
			'''.stripIndent();
		String expected = '''\
			Hello, World.
			Hello, Groovy.
			'''.stripIndent();
		List tagTexts = [ tagText1, tagText2, tagText3 ];
		when:
		format.apply(tagTexts);
		then:
		tagTexts.collect { it.text ?: '' }.join() == expected;
	}
}
