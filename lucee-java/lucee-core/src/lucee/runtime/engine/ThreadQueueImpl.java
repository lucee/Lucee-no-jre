/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.runtime.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lucee.commons.io.SystemUtil;
import lucee.commons.lang.SerializableObject;
import lucee.runtime.PageContext;

public class ThreadQueueImpl implements ThreadQueue {
	private final SerializableObject token=new SerializableObject();
	
	
	public final List<PageContext> list=new ArrayList<PageContext>();
	private final int max;
	private long timeout;
	private int waiting=0;
	
	public ThreadQueueImpl(int max, long timeout){
		this.max=max;
		this.timeout=timeout;
	}
	
	
	@Override
	public void enter(PageContext pc) throws IOException {
		try{
			synchronized (token) {waiting++;}
			_enter(pc);
		}
		finally {
			synchronized (token) {waiting--;}
		}
	}
	
	private void _enter(PageContext pc) throws IOException {
		//print.e("enter("+Thread.currentThread().getName()+"):"+list.size());
		long start=System.currentTimeMillis();
		while(true) {
			synchronized (token) {
				if(list.size()<max) {
					//print.e("- ok("+Thread.currentThread().getName()+"):"+list.size());
					list.add(pc);
					return;
				}
			}
			if(timeout>0) SystemUtil.wait(token,timeout);
			else SystemUtil.wait(token);
			
			if(timeout>0 && (System.currentTimeMillis()-start)>=timeout)
				throw new IOException("timeout ("+(System.currentTimeMillis()-start)+") ["+timeout+"] is occured, server is busy handling requests");
		}
	}
	
	public void exit(PageContext pc){
		//print.e("exist("+Thread.currentThread().getName()+")");
		synchronized (token) {
			list.remove(pc);
			token.notify();
		}
	}
	
	public int size(){
		return waiting;
	}


	public void clear() {
		list.clear();
		token.notifyAll();
	}
	
	
	/*public static class Test extends Thread {
		private ThreadQueueImpl queue;
		public Test(ThreadQueueImpl queue){
			this.queue=queue;
		}
		public void run(){
			String name = Thread.currentThread().getName();
			try {
				queue.enter(name);
				queue.size();
				SystemUtil.sleep(50);
				queue.exit(name);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
	}
	
	public static void main(String[] args) {
		ThreadQueueImpl queue=new ThreadQueueImpl(4,1000);
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		new Test(queue).start();
		
		
	}*/
	
	
}
