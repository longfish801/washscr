/*
 * ConveyerSystem.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh.conveyer;

import groovy.util.logging.Slf4j;
import groovyx.gpars.GParsPool;
import io.github.longfish801.shared.lang.ArgmentChecker;
import java.lang.Thread as JavaThread;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 複数の工程から成る流れ作業を実施します。<br>
 * ある特定の要素からなるリストに対して、順番に各工程の処理をします。<br>
 * 各工程は並列に動作するため、効率的に処理ができます。<br>
 * 各工程は{@link Conveyer}を継承することで実装します。<br>
 * 工程は{@link Conveyer#convey(Product<E>)}メソッドをオーバーライドしてください。<br>
 * {@link Conveyer#pass(Product<E>)}メソッドを呼んで、次の工程に加工後の要素を渡してください。
 * @version 1.0.00 2016/01/07
 * @author io.github.longfish801
 */
@Slf4j('LOG')
class ConveyerSystem<E> {
	/** 工程リスト */
	List<Conveyer<E>> conveyers = [];
	/** 各工程のスレッド一覧 */
	List<JavaThread> threads = Collections.synchronizedList(new ArrayList<>());
	/** どこかの工程で発生した例外 */
	Throwable exc = null;
	/** 出力キュー */
	BlockingQueue<Product<E>> queueOut = new LinkedBlockingQueue<E>();
	
	/**
	 * BufferedReaderから読みこむ文字列を行毎に各工程で処理し BufferedWriterに書きこみます。
	 * @param conveyers 工程リスト
	 * @param reader BufferedReader
	 * @param writer BufferedWriter
	 * @return 処理結果リスト
	 */
	static void conveyBuffer(List<Conveyer<String>> conveyers, BufferedReader reader, BufferedWriter writer){
		ConveyerSystem<String> system = new ConveyerSystem<E>(conveyers);
		JavaThread conveyThread = system.conveyAsync();
		Thread.start {
			try {
				String line;
				while ((line = reader.readLine()) != null) system.pass(line);
			} catch (exc){
				LOG.error('要素の格納時に例外が発生しました。', exc);
			} finally {
				system.complete();
				reader.close();
			}
		}
		JavaThread takeThread = Thread.start {
			try {
				String line;
				while ((line = system.queueOut.take().elem) != null){
					writer.write(line);
					writer.write("\n");
				}
			} catch (InterruptedException exc){
				LOG.info('割り込みが発生したため、要素の取得を中断します。');
			} catch (exc){
				LOG.error('要素の取得時に例外が発生しました。', exc);
			} finally {
				try {
					writer.flush();
					writer.close();
				} catch (exc){
					LOG.error('終了時に例外が発生しました。', exc);
				} finally {
				}
			}
		}
		conveyThread.join();
		takeThread.join();
		if (system.exc != null) throw system.exc;
	}
	
	/**
	 * リスト内の要素を各工程で処理した結果を返します。
	 * @param conveyers 工程リスト
	 * @param elems 処理対象リスト
	 * @return 処理結果リスト
	 */
	static List<E> conveyList(List<Conveyer<E>> conveyers, List<E> elems){
		ConveyerSystem<E> system = new ConveyerSystem<E>(conveyers);
		JavaThread thread = system.conveyAsync();
		elems.each { system.pass(it) }
		system.complete();
		thread.join();
		if (system.exc != null) throw system.exc;
		List list = [];
		E elem;
		while ((elem = system.queueOut.take().elem) != null) list << elem;
		return list;
	}
	
	/**
	 * コンストラクタ。<br>
	 * 工程リストを初期化します。各工程に本インスタンス、前工程、次工程を設定します。<br>
	 * @param conveyers 工程リスト
	 */
	ConveyerSystem(List<Conveyer<E>> conveyers){
		ArgmentChecker.checkNotEmptyList('conveyers', conveyers);
		conveyers.each { ArgmentChecker.checkClass('工程', it, Conveyer.class) }
		// 工程リストを初期化します
		Conveyer<E> preConveyer = null;
		conveyers.each { Conveyer<E> conveyer ->
			conveyer.system = this;
			if (preConveyer != null) preConveyer.nextConveyer = conveyer;
			preConveyer = conveyer;
		}
		this.conveyers = conveyers;
	}
	
	/**
	 * 先頭の工程に要素を渡します。<br>
	 * すべての要素を渡し終えたならば、最後に {@link #complete()}を呼んでください。
	 * @param elem 要素
	 */
	void pass(E elem){
		pass(new Product(elem));
	}
	
	/**
	 * 先頭の工程に、処理の対象としない要素を渡します。
	 * @param elem 要素
	 */
	void passThrough(E elem){
		pass(new Product(elem, false));
	}
	
	/**
	 * 先頭の工程にnullを渡すことで、全工程の終了を試みます。<br>
	 * すべての要素を渡し終えたならば、必ず最後に呼んでください。
	 */
	void complete(){
		pass(new Product(null));
	}
	
	/**
	 * 先頭の工程にProductを渡します。
	 * @param product Product
	 */
	void pass(Product<E> product){
		try {
			conveyers.first().queue.put(product);
		} catch (InterruptedException exc){
			LOG.info('割り込みが発生したため、先頭の工程への要素の格納を中断します。 name={}', conveyers.first().name);
		}
	}
	
	/**
	 * 流れ作業を非同期で並列実行します。
	 * @return 流れ作業を実行しているスレッド
	 */
	JavaThread conveyAsync(){
		JavaThread thread = Thread.start {
			GParsPool.withPool { conveyers.eachParallel { it.convey() } }
		}
		return thread;
	}
}
