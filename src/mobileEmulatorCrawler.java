package mobileEmulator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.BufferedReader;
import java.io.IOException; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
        
public class mobileEmulatorCrawler {
    private static int replays=1;
    
    private static void looper(String file,String throttle,String device,String extension,String resultsDir) throws IOException, InterruptedException{	
        try{
            ProxifiedChrome proxySel=new ProxifiedChrome(throttle,resultsDir);
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            String url;
            while ((url = br.readLine()) != null) {
                for(int i=0; i<replays;i++){
                    //mobile+no extension
                    if (extension!=null)
                        proxySel.runTest(url,device,null,i);
                    //mobile+extension
                    proxySel.runTest(url,device,extension,i);
                    if(device!=null){
                        //no mobile+no extension
                        if (extension!=null) 
                            proxySel.runTest(url,null,null,i);
                        //no mobile+extension
                        proxySel.runTest(url,null,extension,i);
                    }
                }
            }
            proxySel.close();
        }      
        catch (FileNotFoundException ex) {
            System.err.println("ERROR: Could not find file "+file);
            System.exit(-1);
        }
    }
    
    public static void main(String []args) throws Exception {
        Settings settings = new Settings();
        new JCommander(settings, args);
        String cwd="./";
        if (settings.dir!=null)
            cwd=settings.dir;
        String filename=settings.file;
        if(settings.file.contains("/"))
            filename=settings.file.split("/")[1];
        if (filename.contains("."))
            filename=filename.split("\\.")[0];
        System.out.println(filename);
        File dir = new File(cwd+"/"+filename+"_results");
        boolean successful = dir.mkdir();
        System.setProperty("webdriver.chrome.driver", cwd+"/bin/chromedriver");                
        looper(settings.file,settings.throttle,settings.device,settings.extension,dir.getAbsolutePath());
    }
}

class Settings {
    @Parameter(names = "-file", description = "URL list to crawl", required = true)
    public String file;

    @Parameter(names = "-throttle", description = "Throttling", required = false)
    public String throttle;

    @Parameter(names = "-device", description = "Mobile device type to emulate", required = false)
    public String device;

    @Parameter(names = "-extension", description = "Browser extension to load", required = false)
    public String extension;
    
    @Parameter(names = "-dir", description = "Change folder with required resourcers", required = false)
    public String dir;
}
