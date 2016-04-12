package mobileEmulator;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
        
public class mobileEmulatorCrawler {
    
    
    private static void looper(int run, String file, String throttle, String device, String extension, String absolutePath, Boolean caching) throws IOException, InterruptedException {
       Worker worker[] = new Worker[5];
       int k=0;
        //no mobile+no extension (vanilla)
        //runList(i,file,throttle,null,null,absolutePath,caching);
        worker[k] = new Worker(file,null,null,run,caching,throttle,absolutePath);
        worker[k].start();
//worker[k].join();
        k++;
        //mobile+extension
        if (extension!=null){
           // if (extension.contains(".crx")){
                //runList(i,file,throttle,device,extension,absolutePath,caching); 
                worker[k] = new Worker(file,device,extension,run,caching,throttle,absolutePath);
                worker[k].start();k++;
//worker[k].join();

                if(device!=null){//no mobile+extension
            //        runList(i,file,throttle,null,extension,absolutePath,caching);
                    worker[k] = new Worker(file,null,extension,run,caching,throttle,absolutePath);
                    worker[k].start();k++;
//worker[k].join();
                }
           /* }else{
         //       allExtensions(i,file,throttle,device,extension,absolutePath,caching);
                worker[k] = new Worker(file,device,extension,run,caching,throttle,absolutePath);
                worker[k].start();k++;
                if(device!=null)//no mobile+extension
          //          allExtensions(i,file,throttle,null,extension,absolutePath,caching);
                    worker[k] = new Worker(file,null,extension,run,caching,throttle,absolutePath);
                    worker[k].start();k++;
            }*/
        }else{   //mobile+no extension 
            if(device!=null){
               // runList(i,file,throttle,device,null,absolutePath,caching);
                worker[k] = new Worker(file,device,null,run,caching,throttle,absolutePath);
                worker[k].start();k++;
//worker[k].join();

            }
        }
        for(int i=0;i<k;i++){
            try
            {
                worker[i].join(60000);
            }
            catch (InterruptedException e)
            { // ignore
            }
            if (worker[i].isAlive())
            { // Thread still alive, we need to abort
                System.out.println("Kill me "+i);
                worker[i].interrupt(); 
            }
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
        Timestamp currentTimestamp = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
        File dir = new File(cwd+"/"+filename+"_"+currentTimestamp+"_results_");
        boolean successful = dir.mkdir();
        System.setProperty("webdriver.chrome.driver", cwd+"/bin/chromedriver"); 
        int replays=1;
        Boolean caching=true;
        if (settings.replays!=null)
            replays=settings.replays;
        if (settings.replays!=null)
            replays=settings.replays;
        if (settings.caching!=null)
            caching=Boolean.parseBoolean(settings.caching);
        System.out.println("Input Given:\n--------------\nReplays: "+replays);
        for(int i=0; i<replays;i++)
            looper(i,settings.file,settings.throttle,settings.device,settings.extension,dir.getAbsolutePath(),caching);
    }
}

class Worker extends Thread{
    private final String file, device, extension,  throttle, resultsDir;
    private final int run;
    private final Boolean caching;
    
    public Worker(String file,String device,String extension,int run,Boolean caching,String throttle,String resultsDir)
    {
        this.file=file;
        this.device=device;
        this.extension=extension; 
        this.throttle=throttle;
        this.resultsDir=resultsDir;
        this.run=run;
        this.caching=caching;
    }
    
    @Override
    public void run()
    {
        try {
            System.out.println("New Thread");
            if ((this.extension==null) || (this.extension.contains(".crx")))
                runList(this.run,this.file,this.throttle,this.device,this.extension,this.resultsDir,this.caching);
            else
                allExtensions(this.run,this.file,this.throttle,this.device,this.extension,this.resultsDir,this.caching);
        } catch (IOException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
 
    private static void runList(int run, String file,String throttle,String device,String extension,String resultsDir, Boolean caching) throws IOException, InterruptedException{	
        System.out.println("File: "+file+"\nExtention: "+ extension+"\nDevice: "+device+"\nThrottle: "+throttle+"\n");
        try{    
            InputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            String url;   
            while ((url = br.readLine()) != null) {
                ProxifiedChrome proxySel=new ProxifiedChrome(throttle,resultsDir);
                proxySel.runTest(url,device,extension,run,caching);
                Thread.sleep(500);
                proxySel.close();
            }
        }
        catch (FileNotFoundException ex) {
            System.err.println("ERROR: Could not find file "+file);
            System.exit(-1);
        }
    }
    
    private static void allExtensions(int run, String file, String throttle, String device, String extensionPath, String absolutePath, Boolean caching) throws IOException, InterruptedException{
        File folder = new File(extensionPath);
        File[] listOfFiles = folder.listFiles();        
        System.out.println("Load all "+listOfFiles.length+" extensions of "+extensionPath);
        if (listOfFiles.length>0){
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()){
                    //System.out.println(listOfFiles[i].getPath());
                    runList(run,file,throttle,device,listOfFiles[i].getPath(),absolutePath,caching);
                }
            }
        }
        else{
           System.err.println("NO EXTENSIONS WERE FOUND");
           System.exit(-1);
        }
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
    
    @Parameter(names = "-r", description = "Number of back-to-back reruns", required = false)
    public Integer replays;
    
    @Parameter(names = "-c", description = "Use Caching and cookies", required = false)
    public String caching;
}
