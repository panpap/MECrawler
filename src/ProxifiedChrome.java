package mobileEmulator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.SessionNotFoundException;
import org.openqa.selenium.remote.UnreachableBrowserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxifiedChrome{
    private static final int SERVER_PORT=4444;
    private static final long WAITTIME=60000; //millisec
    private final Proxy proxy;	
    private BrowserMobProxyServer server;
    private final String resultsDir;
    private final String throttleType;
    final static Logger logger = LoggerFactory.getLogger(ProxifiedChrome.class);

    public ProxifiedChrome(String throttleType,String dir) throws IOException{
        this.resultsDir=dir;
        this.throttleType=throttleType;
        Integer[] throttle=translateThrottling();
        this.proxy=startProxy(throttle);
    }
    
    public void close(){
        System.out.println("I'm closing server");
        this.server.stop();
    }

    public void runTest(String myurl,String device,String extension,int replay,Boolean caching) throws IOException{
        String print="";String domain;
        if (device==null)
            print+=" without mobile emulation";
        else
            print+=" with mobile emulation ("+device+")";
        if (extension==null)
            print+=" without extension";
        else{
            String[] tmp=extension.split("/");
            print+=" with "+tmp[tmp.length-1]+" extension";
        }
        System.out.println("Crawling "+myurl+print+" "+replay);
        if (myurl.contains("://")){
            domain=myurl.split("://")[1];
        }else{
            domain=myurl;
            myurl="http://"+myurl;
        }
	// create a new HAR with the label: domain
        this.server.newHar(domain);
 	openBrowser(myurl,device,extension,caching);
        // get the HAR data
	storeHarFile(domain,device,extension,replay);
    }

    private Proxy startProxy(Integer[] throttle){
        // start the proxy
        this.server = new BrowserMobProxyServer(SERVER_PORT);
        this.server.start(0);
        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(server); 
        //throttling
        if (throttle!=null){
            this.server.setLatency(throttle[2]); //latencyMs
            this.server.setDownstreamKbps(throttle[1]); //downstreamKbps
            this.server.setUpstreamKbps(throttle[0]); //upstreamKbps
        }
        return seleniumProxy;
    }

    private void storeHarFile(String domain,String device, String extension, int replay) throws IOException{
        Har har = this.server.getHar();
        String outputFile=getOutputFilename(domain,device,extension,replay);
        File harFile = new File(this.resultsDir+"/"+outputFile+".har");
        har.writeTo(harFile);
    }

    private void openBrowser(String myurl,String device,String extension,Boolean caching) throws IOException{
        long startTime = System.currentTimeMillis();
        WebDriver driver = null;
        try{
          driver = startChromeDriver(this.proxy,device,extension,caching);
     //   driver.manage().timeouts().pageLoadTimeout(WAITTIME,TimeUnit.MILLISECONDS);
   /*     if(extension!=null){
            String[] tmp = extension.split("/");
            String temp=tmp[tmp.length-1];
            if(temp.contains("-"))
                temp=temp.split("-")[0];
            if(temp.contains("_"))
                temp=temp.split("_")[0];
            System.out.println("Blacklisting "+"http://.*\\."+temp+".*");
            this.server.blacklistRequests("http://.*\\."+temp+".*", 200); 
        }*/
        
            if(caching==false)
                driver.manage().deleteAllCookies();
            safeFetch(myurl,driver);
        }catch(WebDriverException e){
            dumpToErrorLog("There was an exception for: "+myurl,e);
        } finally {
            try{
                if (driver!=null){
                    driver.close();
                    driver.quit();
                }
            }catch(SessionNotFoundException e){}
        }
        System.out.println("Elapsed time "+(System.currentTimeMillis() - startTime)+" ms");
    }
    
    private void safeFetch(String url, final WebDriver driver){
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                driver.get(Thread.currentThread().getName());
            }
        }, url);
        t.start();
        try
        {
          //  t.join(WAITTIME);
            Thread.sleep(WAITTIME);
        }
        catch (InterruptedException e)
        { // ignore
        }
        if (t.isAlive())
        { // Thread still alive, we need to abort
          System.out.println("RAISE timeout on loading page " + url);
          t.interrupt();
 //         closeWindow(driver);
  //        System.out.println("JSCLOSED");
          driver.close();
          System.out.println("driverCLOSED");
          driver.quit();
          System.out.println("driverQUIT " + url);
        }
    }
    
    private ChromeOptions mobileEmulation(String device,ChromeOptions chromeOptions){
        Map<String, String> mobileEmulation = new HashMap<String, String>();
        mobileEmulation.put("deviceName", device);
        chromeOptions.setExperimentalOption("mobileEmulation", mobileEmulation);
        return chromeOptions;
    }
            
    private ChromeDriver startChromeDriver(Proxy proxy,String device,String extension,Boolean caching) throws IOException{
        //Selenium
        ChromeOptions chromeOptions = new ChromeOptions();    //chromeOptions.put("binary", "/usr/bin/google-chrome");
        //chromeOptions.addArguments("--incognito");
        if (extension!=null)
            chromeOptions.addExtensions(new File(extension));  //addition of browser extension
        if (device!=null)
            chromeOptions=mobileEmulation(device,chromeOptions); //mobile emulation
        DesiredCapabilities capabilities = DesiredCapabilities.chrome();
        //NO CACHING
        if (caching==false)
            chromeOptions.addArguments("--disable-application-cache");
        //chromeOptions.addArguments("--no-sandbox");
        capabilities.setCapability(CapabilityType.PROXY, proxy);
        capabilities.setCapability(ChromeOptions.CAPABILITY, chromeOptions);        
        return new ChromeDriver(capabilities);
    }

    private String getOutputFilename(String domain,String device,String extension,int run){
        String output=domain+"_";
        if(this.throttleType!=null)
            output=output+"_"+this.throttleType;
        if(device!=null)
            output=output+"_"+device.replace(" ", "_");
        if(extension!=null)
        {
            if (extension.contains("ghostery"))
                output=output+"_ghostery";
            else if (extension.contains("adblock"))
                output=output+"_adblock";
            else if (extension.contains("disconnect"))
                output=output+"_disconnect";
            else
                output=output+"_unknown";
        }
        return output+"_"+run;
    }

    private Integer[] translateThrottling()
    {
        Integer[] throttle = new Integer[3];
        if(this.throttleType==null)
            return null;
        else
        {
            switch (this.throttleType) {
                case "Offline":
                    throttle[0]=0; //upstreamKbps
                    throttle[1]=0; //downstreamKbps
                    throttle[2]=0; //latencyMS
                    break;
                case "GPRS":
                    throttle[0]=50; //upstreamKbps
                    throttle[1]=20; //downstreamKbps
                    throttle[2]=500; //latencyMS
                    break;
                case "Regular2G":
                    throttle[0]=250; //upstreamKbps
                    throttle[1]=50; //downstreamKbps
                    throttle[2]=300; //latencyMS
                    break;
                case "Good2G":
                    throttle[0]=450; //upstreamKbps
                    throttle[1]=150; //downstreamKbps
                    throttle[2]=150; //latencyMS
                    break;
                case "Regular3G":
                    throttle[0]=750; //upstreamKbps
                    throttle[1]=250; //downstreamKbps
                    throttle[2]=100; //latencyMS
                    break;
                case "Good3G":
                    throttle[0]=1000; //upstreamKbps
                    throttle[1]=750; //downstreamKbps
                    throttle[2]=40; //latencyMS
                    break;
                case "Regular4G":
                    throttle[0]=4000; //upstreamKbps
                    throttle[1]=3000; //downstreamKbps
                    throttle[2]=20; //latencyMS
                    break;
                case "DSL":
                    throttle[0]=2000; //upstreamKbps
                    throttle[1]=1000; //downstreamKbps
                    throttle[2]=5; //latencyMS
                    break;
                case "Wifi":
                    throttle[0]=30000; //upstreamKbps
                    throttle[1]=15000; //downstreamKbps
                    throttle[2]=2; //latencyMS
                    break;
                default:
                    System.out.println("WARNING: Wrong throttling choise... ignoring...");
                    return null;
            }
            return throttle;
        }
    }

    private void dumpToErrorLog(String string,Exception e) throws FileNotFoundException {
        System.err.println("ERROR: "+string);
        PrintWriter out = new PrintWriter(new FileOutputStream(new File(this.resultsDir+"/errorLog.log"), true));
        out.append("\n"+string+"\n"+e+"\n");
        out.flush();
        out.close();
    }

    private void closeWindow(WebDriver driver) {
        System.out.println("Force closing");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        String script = "window.onbeforeunload = null;" + "window.close();";
        js.executeScript(script);
    }
}