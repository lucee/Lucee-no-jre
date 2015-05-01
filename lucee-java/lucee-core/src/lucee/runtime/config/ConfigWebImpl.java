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
package lucee.runtime.config;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lucee.commons.digest.HashUtil;
import lucee.commons.io.SystemUtil;
import lucee.commons.io.res.Resource;
import lucee.commons.io.res.ResourceProvider;
import lucee.commons.io.res.ResourcesImpl;
import lucee.commons.lang.StringUtil;
import lucee.commons.lock.KeyLock;
import lucee.runtime.CFMLFactoryImpl;
import lucee.runtime.Mapping;
import lucee.runtime.MappingImpl;
import lucee.runtime.Page;
import lucee.runtime.PageContext;
import lucee.runtime.PageSourceImpl;
import lucee.runtime.cache.tag.CacheHandlerFactoryCollection;
import lucee.runtime.cfx.CFXTagPool;
import lucee.runtime.compiler.CFMLCompilerImpl;
import lucee.runtime.debug.DebuggerPool;
import lucee.runtime.engine.ThreadQueue;
import lucee.runtime.exp.ExpressionException;
import lucee.runtime.exp.PageException;
import lucee.runtime.exp.SecurityException;
import lucee.runtime.gateway.GatewayEngineImpl;
import lucee.runtime.gateway.GatewayEntry;
import lucee.runtime.lock.LockManager;
import lucee.runtime.lock.LockManagerImpl;
import lucee.runtime.monitor.ActionMonitorCollector;
import lucee.runtime.monitor.IntervallMonitor;
import lucee.runtime.monitor.RequestMonitor;
import lucee.runtime.net.http.ReqRspUtil;
import lucee.runtime.security.SecurityManager;
import lucee.runtime.security.SecurityManagerImpl;
import lucee.runtime.tag.TagHandlerPool;
import lucee.runtime.type.scope.Cluster;
import lucee.runtime.writer.CFMLWriter;
import lucee.runtime.writer.CFMLWriterImpl;
import lucee.runtime.writer.CFMLWriterWS;
import lucee.runtime.writer.CFMLWriterWSPref;

import org.apache.commons.collections.map.ReferenceMap;
import org.xml.sax.SAXException;

/**
 * Web Context
 */
public final class ConfigWebImpl extends ConfigImpl implements ServletConfig, ConfigWeb {
    
    private ServletConfig config;
    private ConfigServerImpl configServer;
    private SecurityManager securityManager;
    private static final LockManager lockManager= LockManagerImpl.getInstance(false);
	private Resource rootDir;
    private CFMLCompilerImpl compiler=new CFMLCompilerImpl();
    private Page baseComponentPage;
	private MappingImpl serverTagMapping;
	private MappingImpl serverFunctionMapping;
	private KeyLock<String> contextLock;
	private GatewayEngineImpl gatewayEngine;
    private DebuggerPool debuggerPool;
    private CacheHandlerFactoryCollection cacheHandlerFactoryCollection;
    
    

    //private File deployDirectory;

    /**
     * constructor of the class
     * @param configServer
     * @param config
     * @param configDir
     * @param configFile
     * @param cloneServer 
     */
    protected ConfigWebImpl(CFMLFactoryImpl factory,ConfigServerImpl configServer, ServletConfig config, Resource configDir, Resource configFile) {
    	super(factory,configDir, configFile,configServer.getTLDs(),configServer.getFLDs());
    	this.configServer=configServer;
        this.config=config;
        factory.setConfig(this);
    	ResourceProvider frp = ResourcesImpl.getFileResourceProvider();
        
        this.rootDir=frp.getResource(ReqRspUtil.getRootPath(config.getServletContext()));
        
        
        // Fix for tomcat
        if(this.rootDir.getName().equals(".") || this.rootDir.getName().equals(".."))
        	this.rootDir=this.rootDir.getParentResource();
    }
    
    public void reset() {
    	super.reset();
    	tagHandlerPool.reset();
    	contextLock=null;
    	baseComponentPage=null;
    }
    
    /* *
     * Constructor of the class, used for configserver dummy instance
     * @param factory
     * @param configServer
     * @param configx
     * @param configDir
     * @param configFile
     * /
    protected ConfigWebImpl(CFMLFactoryImpl factory,ConfigServerImpl configServer, Resource configDir, Resource configFile,Resource rootDir) {
    	super(factory,configDir, configFile,configServer.getTLDs(),configServer.getFLDs());
    	this.configServer=configServer;
        factory.setConfig(this);
    	
        this.rootDir=rootDir;
        
        // Fix for tomcat
        if(this.rootDir.getName().equals(".") || this.rootDir.getName().equals(".."))
        	this.rootDir=this.rootDir.getParentResource();
    }*/
    
    

    @Override
    public String getServletName() {
        return config.getServletName();
    }

    @Override
    public ServletContext getServletContext() {
        return config.getServletContext();
    }

    @Override
    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }

    @Override
    public Enumeration getInitParameterNames() {
        return config.getInitParameterNames();
    }

    protected ConfigServerImpl getConfigServerImpl() {
        return configServer;
    }
    
    @Override
    public ConfigServer getConfigServer(String password) throws ExpressionException {
        configServer.checkAccess(password);
    	return configServer;
    }
    
    // FUTURE add to public interface
    public ConfigServer getConfigServer(String key, long timeNonce) throws PageException {
    	configServer.checkAccess(key,timeNonce);
    	return configServer;
    }
    
    public String getServerId() {
        return configServer.getId();
    }
    
    public String getServerIdPro() {
        return configServer.getIdPro();
    }

    public String getServerSecurityKey() {
        return configServer.getSecurityKey();
    }
    
    public Resource getServerConfigDir() {
        return configServer.getConfigDir();
    }
    

    /**
     * @return Returns the accessor.
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * @param securityManager The accessor to set.
     */
    protected void setSecurityManager(SecurityManager securityManager) {
    	((SecurityManagerImpl)securityManager).setRootDirectory(getRootDirectory());
        this.securityManager = securityManager;
    }
    
    @Override
    public CFXTagPool getCFXTagPool() throws SecurityException {
        if(securityManager.getAccess(SecurityManager.TYPE_CFX_USAGE)==SecurityManager.VALUE_YES) return super.getCFXTagPool();
        throw new SecurityException("no access to cfx functionality", "disabled by security settings");
    }

    /**
     * @return Returns the rootDir.
     */
    public Resource getRootDirectory() {
        return rootDir;
    }

    @Override
    public String getUpdateType() {
        return configServer.getUpdateType();
    }

    @Override
    public URL getUpdateLocation() {
        return configServer.getUpdateLocation();
    }

    @Override
    public LockManager getLockManager() {
        return lockManager;
    }

	/**
	 * @return the compiler
	 */
	public CFMLCompilerImpl getCompiler() {
		return compiler;
	}
	
	 public Page getBaseComponentPage(PageContext pc) throws PageException {
	        if(baseComponentPage==null) {
	            baseComponentPage=((PageSourceImpl)getBaseComponentPageSource(pc)).loadPage(pc);
				
	        }
	        return baseComponentPage;
	    }
	    public void resetBaseComponentPage() {
	        baseComponentPage=null;
	    }
	    


		

	    public Mapping getServerTagMapping() {
	    	if(serverTagMapping==null){
	    		serverTagMapping=getConfigServerImpl().tagMapping.cloneReadOnly(this);
	    	}
			return serverTagMapping;
		}
	    public Mapping getServerFunctionMapping() {
	    	if(serverFunctionMapping==null){
	    		serverFunctionMapping=getConfigServerImpl().functionMapping.cloneReadOnly(this);
	    	}
			return serverFunctionMapping;
		}
	    private Map<String,Mapping> applicationMappings=new ReferenceMap(ReferenceMap.SOFT,ReferenceMap.SOFT);

	    
	    
		private TagHandlerPool tagHandlerPool=new TagHandlerPool(this);
		
		// FYI used by Extensions, do not remove
		public Mapping getApplicationMapping(String virtual, String physical) {
			return getApplicationMapping("application",virtual, physical,null,true,false);
		}

		public Mapping getApplicationMapping(String type,String virtual, String physical,String archive,boolean physicalFirst, boolean ignoreVirtual) {
			String key=type+":"+
				virtual.toLowerCase()
				+":"+(physical==null?"":physical.toLowerCase())
				+":"+(archive==null?"":archive.toLowerCase())
				+":"+physicalFirst;
			key=Long.toString(HashUtil.create64BitHash(key),Character.MAX_RADIX);
			
			
			Mapping m=applicationMappings.get(key);
			
			if(m==null){
				m=new MappingImpl(
					this,virtual,
					physical,
					archive,ConfigImpl.INSPECT_UNDEFINED,physicalFirst,false,false,false,true,ignoreVirtual,null
					);
				applicationMappings.put(key,m);
			}
			
			return m;
		}

		public String getLabel() {
			String hash=getHash();
			String label=hash;
			Map<String, String> labels = configServer.getLabels();
			if(labels!=null) {
				String l = labels.get(hash);
				if(!StringUtil.isEmpty(l)) {
					label=l;
				}
			}
			return label;
		}
		
		public String getHash() {
			return SystemUtil.hash(getServletContext());
		}

		public KeyLock<String> getContextLock() {
			if(contextLock==null) {
				contextLock=new KeyLock<String>();
			}
			return contextLock;
		}


		protected void setGatewayEntries(Map<String, GatewayEntry> gatewayEntries) {
			try {
				getGatewayEngine().addEntries(this,gatewayEntries);
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		public GatewayEngineImpl getGatewayEngine() {
			if(gatewayEngine==null){
				gatewayEngine=new GatewayEngineImpl(this);
			}
			return gatewayEngine;
		}
		public void setGatewayEngine(GatewayEngineImpl gatewayEngine) {
			this.gatewayEngine=gatewayEngine;
		}

		public TagHandlerPool getTagHandlerPool() {
			return tagHandlerPool;
		}

		public DebuggerPool getDebuggerPool() {
			if(debuggerPool==null){
				Resource dir = getConfigDir().getRealResource("debugger");
				dir.mkdirs();
				debuggerPool=new DebuggerPool(dir);
			}
			return debuggerPool;
		}
		

		public ThreadQueue getThreadQueue() {
			return configServer.getThreadQueue();
		}

		@Override
		public int getLoginDelay() {
			return configServer.getLoginDelay();
		}

		@Override
		public boolean getLoginCaptcha() {
			return configServer.getLoginCaptcha();
		}

		@Override
		public boolean getRememberMe() {
			return configServer.getRememberMe();
		}
		
		@Override
		public Resource getSecurityDirectory(){
			return configServer.getSecurityDirectory();
		}
		
		@Override
		public boolean isMonitoringEnabled(){
			return configServer.isMonitoringEnabled();
		}
		

		
		public RequestMonitor[] getRequestMonitors(){
			return configServer.getRequestMonitors();
		}
		
		public RequestMonitor getRequestMonitor(String name) throws PageException{
			return configServer.getRequestMonitor(name);
		}
		
		public IntervallMonitor[] getIntervallMonitors(){
			return configServer.getIntervallMonitors();
		}

		public IntervallMonitor getIntervallMonitor(String name) throws PageException{
			return configServer.getIntervallMonitor(name);
		}
		
		@Override
		public void checkPermGenSpace(boolean check) {
			configServer.checkPermGenSpace(check);
		}

		@Override
		public Cluster createClusterScope() throws PageException {
			return configServer.createClusterScope();
		}

		@Override
		public boolean hasServerPassword() {
			return configServer.hasPassword();
		}
		
		public void updatePassword(boolean server, String passwordOld, String passwordNew) throws PageException, IOException, SAXException {
			Password.updatePassword(server?configServer:this,passwordOld,passwordNew);
		}
		
		public void updatePassword(boolean server, Password passwordOld, Password passwordNew) throws PageException, IOException, SAXException {
			Password.updatePassword(server?configServer:this,passwordOld,passwordNew);
		}
		
		public Password updatePasswordIfNecessary(boolean server,String passwordRaw) {
			ConfigImpl config=server?configServer:this;
			return Password.updatePasswordIfNecessary(config,config.password,passwordRaw);
		}

		@Override
		public Resource getConfigServerDir() {
			return configServer.getConfigDir();
		}

		public Map<String, String> getAllLabels() {
			return configServer.getLabels();
		}

		@Override
		public boolean allowRequestTimeout() {
			return configServer.allowRequestTimeout();
		}
		
		public CFMLWriter getCFMLWriter(PageContext pc, HttpServletRequest req, HttpServletResponse rsp) {
			// FUTURE  move interface CFMLWriter to Loader and load dynaicly from lucee-web.xml
	        if(writerType==CFML_WRITER_WS)
	            return new CFMLWriterWS		(pc,req,rsp,-1,false,closeConnection(),isShowVersion(),contentLength());
	        else if(writerType==CFML_WRITER_REFULAR) 
	            return new CFMLWriterImpl			(pc,req,rsp,-1,false,closeConnection(),isShowVersion(),contentLength());
	        else
	            return new CFMLWriterWSPref	(pc,req,rsp,-1,false,closeConnection(),isShowVersion(),contentLength());
	    }

		
		public ActionMonitorCollector getActionMonitorCollector() {
			return configServer.getActionMonitorCollector();
		}

		@Override
		public boolean getFullNullSupport() {
			return configServer.getFullNullSupport();
		}
		public boolean hasIndividualSecurityManager() {
			return configServer.hasIndividualSecurityManager(getId());
	    }
		public String getServerApiKey() {
			return configServer.getApiKey();
	    }
		
		public CacheHandlerFactoryCollection getCacheHandlerFactories(){
			if(cacheHandlerFactoryCollection==null)
				cacheHandlerFactoryCollection=new CacheHandlerFactoryCollection(this);
			return cacheHandlerFactoryCollection;
		}
		

		public int getServerPasswordType() {
			return configServer.getPasswordType();
		}
		public String getServerPasswordSalt() {
			return configServer.getPasswordSalt();
		}
		public int getServerPasswordOrigin() {
			return configServer.getPasswordOrigin();
		}
		
		public String getServerSalt() {
			return configServer.getSalt();
		}

		public Password isServerPasswordEqual(String password, boolean hashIfNecessary) {
			return configServer.isPasswordEqual(password, hashIfNecessary);
		}

		public boolean isDefaultPassword() {
			if(password==null) return false;
			return password==configServer.defaultPassword;
		}
}
