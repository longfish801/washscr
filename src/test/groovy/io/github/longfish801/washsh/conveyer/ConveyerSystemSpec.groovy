/*
 * ConveyerSystemSpec.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh.conveyer;

import groovy.util.logging.Slf4j;
import java.lang.Thread as JavaThread;
import java.util.concurrent.BlockingQueue;
import spock.lang.Specification;
import spock.lang.Timeout;

/**
 * ConveyerSystemクラスのテスト。
 * 
 * @version 1.0.00 2016/01/19
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class ConveyerSystemSpec extends Specification {
	@Timeout(10)
	def 'BufferedReaderから読みこむ文字列を行毎に各工程で処理し BufferedWriterに書きこみます'(){
		given:
		String text = '''\
			a
			b
			c'''.stripIndent();
		String expect = '''\
			aaTEST
			bbTEST
			ccTEST
			zTEST
			'''.stripIndent();
		StringWriter writer = new StringWriter();
		
		when:
		ConveyerSystem.conveyBuffer([ new TwiceConveyer<String>(), new TestConveyer<String>() ], new BufferedReader(new StringReader(text)), new BufferedWriter(writer));
		then:
		writer.toString() == expect;
	}
	
	@Timeout(10)
	def 'リスト内の要素を各工程で処理した結果を返します'(){
		given:
		List list = null;
		
		when:
		list = ConveyerSystem.conveyList([ new TwiceConveyer<String>(), new TestConveyer<String>() ], [ 'a', 'b', 'c' ]);
		then:
		list == [ 'aaTEST', 'bbTEST', 'ccTEST', 'zTEST' ];
	}
	
	def 'コンストラクタ'(){
		given:
		ConveyerSystem sytem = null;
		
		when:
		new ConveyerSystem([ new TestConveyer<String>() ]);
		then:
		noExceptionThrown();
		
		when:
		new ConveyerSystem([]);
		then:
		thrown(IllegalArgumentException);
		
		when:
		new ConveyerSystem([ 'aaa' ]);
		then:
		thrown(IllegalArgumentException);
	}
	
	@Timeout(10)
	def '流れ作業を非同期で並列実行します'(){
		given:
		List list = null;
		ConveyerSystem<String> system = null;
		JavaThread thread = null;
		String elem;
		
		when:
		system = new ConveyerSystem<String>([ new TestConveyer<String>() ]);
		thread = system.conveyAsync();
		Thread.start {
			[ 'a', 'b', 'c' ].each { system.pass(it) }
			system.complete();
		}
		thread.join();
		list = [];
		while ((elem = system.queueOut.take().elem) != null) list << elem;
		then:
		system.exc == null;
		list == [ 'aTEST', 'bTEST', 'cTEST' ];
		
		when:
		system = new ConveyerSystem<String>([ new TwiceConveyer<String>(), new TestConveyer<String>() ]);
		thread = system.conveyAsync();
		Thread.start {
			[ 'a', 'b', 'c' ].each { system.pass(it) }
			system.complete();
		}
		thread.join();
		list = [];
		while ((elem = system.queueOut.take().elem) != null) list << elem;
		then:
		system.exc == null;
		list == [ 'aaTEST', 'bbTEST', 'ccTEST', 'zTEST' ];
		
		when:
		system = new ConveyerSystem<String>([ new TwiceConveyer<String>(), new TestConveyer<String>() ]);
		thread = system.conveyAsync();
		Thread.start {
			system.pass('a');
			system.passThrough('b');
			system.pass('c');
			system.complete();
		}
		thread.join();
		list = [];
		while ((elem = system.queueOut.take().elem) != null) list << elem;
		then:
		system.exc == null;
		list == [ 'aaTEST', 'b', 'ccTEST', 'zTEST' ];
		
		when:
		system = new ConveyerSystem<String>([ new ExcConveyer<String>(), new TestConveyer<String>() ]);
		thread = system.conveyAsync();
		Thread.start {
			[ 'a', 'b', 'c' ].each { system.pass(it) }
			system.complete();
		}
		thread.join();
		then:
		system.exc != null;
	}
	
	/**
	 * 各要素の文字列の末尾に文字列"TEST"を付加する工程です。
	 */
	class TestConveyer<String> extends Conveyer {
		/** {@inheritDoc} */
		@Override
		String process(String elem){
			return elem + 'TEST';
		}
	}
	
	/**
	 * 各要素の文字列を二重にする工程です。
	 */
	class TwiceConveyer<String> extends Conveyer {
		/** {@inheritDoc} */
		@Override
		String process(String elem){
			return elem * 2;
		}
		
		/** {@inheritDoc} */
		@Override
		void conveyLast(){
			pass(new Product('z'));
		}
	}
	
	/**
	 * 意図的に例外を発生させる工程です。
	 */
	class ExcConveyer<String> extends Conveyer {
		/** {@inheritDoc} */
		@Override
		String process(String elem){
			if (elem == 'b') Integer.parseInt('abc');
			return elem;
		}
	}
}
