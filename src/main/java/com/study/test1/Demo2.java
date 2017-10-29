package com.study.test1;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Demo2 {

	public static void main(String[] args) {
		/*Callable<String> call = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return null;
			}
		};*/
		final ExecutorService exec = Executors.newFixedThreadPool(1);
		Future<String> future = exec.submit(()->{
			Thread.sleep(1000*5);
			System.out.println("test");
			return "线程执行完成。";
		});
		try {
			//任务处理超时设为1秒
			String obj = future.get(1000*1, TimeUnit.MILLISECONDS);
			System.out.println("任务成功返回："+obj);
		} catch (TimeoutException e) {
			System.out.println("任务超时");
			future.cancel(true);
			e.printStackTrace();
		} catch(Exception e) {
			System.out.println("任务处理失败");
			e.printStackTrace();
		}
		future = exec.submit(()->{
			Thread.sleep(1000*5);
			System.out.println("test");
			return "线程执行完成。";
		});
		try {
			//任务处理超时设为1秒
			String obj = future.get(1000*1, TimeUnit.MILLISECONDS);
			System.out.println("任务成功返回："+obj);
		} catch (TimeoutException e) {
			System.out.println("任务超时");
			future.cancel(true);
			e.printStackTrace();
		} catch(Exception e) {
			System.out.println("任务处理失败");
			e.printStackTrace();
		}
		exec.shutdown();
	}
	
	public void test2() {
		new Thread(()->{
			
		}).start();
	}
}
