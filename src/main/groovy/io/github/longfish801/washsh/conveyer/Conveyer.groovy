/*
 * Conveyer.groovy
 *
 * Copyright (C) io.github.longfish801 All Rights Reserved.
 */
package io.github.longfish801.washsh.conveyer;

import groovy.util.logging.Slf4j;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 複数の工程から成る流れ作業のひとつを実行します。<br>
 * 詳細は {@link ConveyerSystem}を参照してください。
 * @version 1.0.00 2016/01/07
 * @author io.github.longfish801
 */
@Slf4j('LOG')
abstract class Conveyer<E> {
	/** 工程名 */
	String name = null;
	/** ConveyerSystem */
	ConveyerSystem<E> system = null;
	/** 入力キュー */
	BlockingQueue<Product<E>> queue = new LinkedBlockingQueue<E>();
	/** 次の工程 */
	Conveyer<E> nextConveyer = null;
	
	/**
	 * 工程を実行します。<br>
	 * 本メソッドは {@link ConveyerSystem#conveyAsync()}から呼ばれることを想定しています。
	 */
	void convey(){
		if (name == null) name = this.class.simpleName;
		LOG.debug('BGN CONVEY {}', name);
		try {
			system.threads << Thread.currentThread();
			while (true){
				Product<E> product = queue.take();
				if (product.elem == null) break;
				if (product.target){
					convey(product);
				} else {
					pass(product);
				}
			}
			conveyLast();
		} catch (InterruptedException exc){
			LOG.info('割り込みが発生したため、工程を中断します。 name={}', name);
		} catch (Throwable exc){
			LOG.error('工程の実行中に例外が発生しました。 name={} exc={}', name, exc.toString());
			system.exc = exc;
			system.threads.remove(Thread.currentThread());
			system.threads.each { it.interrupt() }
		} finally {
			try {
				terminate();
			} catch (Throwable exc){
				LOG.error('終期化中に例外が発生しました。 name={} exc={}', name, exc.toString());
				system.exc = exc;
			} finally {
				pass(new Product(null));
				LOG.debug('END CONVEY {}', name);
			}
		}
	}
	
	/**
	 * キューの要素を加工します。<br>
	 * 本メソッドをオーバライドし、次の工程へ要素を渡してください。<br>
	 * 次の工程へ値を渡すには{@link #pass(E)}を使用してください。<br>
	 * 単純にひとつの要素を加工して次の工程へ渡すだけならば {@link #process(E)}のほうをオーバーライドしてください。
	 * @param product Product
	 */
	void convey(Product<E> product){
		pass(product.store(process(product.elem)));
	}
	
	/**
	 * 要素を加工して返します。<br>
	 * 本メソッドをオーバライドして要素を加工してください。次の工程へ渡す要素を返してください。<br>
	 * 要素を新しく追加するなど複雑な処理をしたい場合は、{@link #convey(Product)}のほうをオーバーライドしてください。
	 * @param elem 要素
	 */
	E process(E elem){
		return elem;
	}
	
	/**
	 * 工程終了時の処理をします。<br>
	 * 工程終了時の処理が必要であればオーバーライドしてください。<br>
	 * 工程実行中に例外が発生した場合は呼ばれません。
	 */
	void conveyLast(){
		LOG.debug('conveyLast {}', name);
	}
	
	/**
	 * 次の工程へ要素を渡します。<br>
	 * 終了時は nullを渡してください。<br>
	 * 全工程を終了したならば、{@link ConveyerSystem#queueOut}に格納します。
	 * @param product Product
	 */
	void pass(Product<E> product){
		try {
			if (nextConveyer == null){
				system.queueOut.put(product);
			} else {
				nextConveyer.queue.put(product);
			}
		} catch (InterruptedException exc){
			LOG.info('割り込みが発生したため、要素の格納を中断します。 name={}', name);
			throw exc;
		}
	}
	
	/**
	 * 終期化処理をします。<br>
	 * 終期化処理が必要であればオーバーライドしてください。<br>
	 * 工程実行中に例外が発生した場合も必ず呼びます。<br>
	 * キューの参照／格納はしないでください。
	 */
	void terminate(){
		LOG.debug('terminate {}', name);
	}
}
