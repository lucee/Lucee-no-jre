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
package lucee.commons.io.res.type.compress;

import java.io.IOException;
import java.io.OutputStream;

public final class CompressOutputStreamSynchronizer extends OutputStream {

	private final OutputStream os;
	private final Compress zip;
	private final boolean async;

	public CompressOutputStreamSynchronizer(OutputStream os, Compress zip,boolean async) {
		this.os=os;
		this.zip=zip;
		this.async=async;
	}

	@Override
	public void close() throws IOException {
		os.close();
		zip.synchronize(async);
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		os.write(b, off, len);
	}

	@Override
	public void write(byte[] b) throws IOException {
		os.write(b);
	}

}