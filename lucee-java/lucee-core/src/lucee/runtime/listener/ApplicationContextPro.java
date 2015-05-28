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
package lucee.runtime.listener;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import lucee.runtime.db.DataSource;
import lucee.runtime.exp.PageException;
import lucee.runtime.type.Collection;
import lucee.runtime.type.CustomType;
import lucee.runtime.type.Struct;
import lucee.runtime.type.dt.TimeSpan;

// FUTURE move to ApplicationContext

public interface ApplicationContextPro extends ApplicationContext {

	public static final short WS_TYPE_AXIS1=1;
	public static final short WS_TYPE_JAX_WS=2;
	public static final short WS_TYPE_CXF=4;
	
    public DataSource[] getDataSources();
    public DataSource getDataSource(String dataSourceName) throws PageException;
    public DataSource getDataSource(String dataSourceName, DataSource defaultValue);

    public void setDataSources(DataSource[] dataSources);
    
    /**
     * default datasource name (String) or datasource (DataSource Object)
     * @return
     */
	public Object getDefDataSource();
	/**
     * orm datasource name (String) or datasource (DataSource Object)
     * @return
     */
	public Object getORMDataSource();
	

	public void setDefDataSource(Object datasource);
	public void setORMDataSource(Object string);
	


	public abstract boolean getBufferOutput();
	public abstract void setBufferOutput(boolean bufferOutput);

	public Locale getLocale();
	public void setLocale(Locale locale);
	
	public short getScopeCascading();
	public void  setScopeCascading(short scopeCascading);

	public TimeZone getTimeZone();
	public void setTimeZone(TimeZone timeZone);

	public Charset getWebCharset();
	public void setWebCharset(Charset charset);

	public Charset getResourceCharset();
	public void setResourceCharset(Charset charset);

	public boolean getTypeChecking();
	public void setTypeChecking(boolean typeChecking);
	
	Map<Collection.Key, Map<Collection.Key, Object>> getTagAttributeDefaultValues();
	public Map<Collection.Key, Object> getTagAttributeDefaultValues(String fullName);
	public void setTagAttributeDefaultValues(Struct sct);

	public TimeSpan getRequestTimeout();
	public void setRequestTimeout(TimeSpan timeout);

	public CustomType getCustomType(String strType);

	public boolean getAllowCompression();
	public void setAllowCompression(boolean allowCompression);

	public boolean getSuppressContent();
	public void setSuppressContent(boolean suppressContent);
	


	public short getWSType();
	public void setWSType(short wstype);


	public abstract boolean getCGIScopeReadonly();
	public void setCGIScopeReadonly(boolean cgiScopeReadonly);
	
}
