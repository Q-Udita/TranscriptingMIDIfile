import java.io.*;
import java.util.*;
import javax.sound.midi.*;
import java.lang.*;
import java.util.concurrent.*;


public class Controller {

public static void main(String[] args) throws Exception {
        
        Scanner input = new Scanner(System.in); 
       // List<String> it = new ArrayList<String>(); 
       // List<Instrument> ist = new ArrayList<Instrument>();      
        Queue<String> MIDIfile = new LinkedList<String>();
        List<String> files = new ArrayList<String>();
        Queue<Integer> inum = new LinkedList<Integer>();
       
        
        System.out.println("Specify the name of the directory of MIDI files");
        String dir=input.next();
        File folder = new File(dir);
        File[] listOfFiles = folder.listFiles();
        if(listOfFiles.length==0)
         System.out.println("TRY AGAIN");
       for (int i = 0; i < listOfFiles.length; i++) {
          if (listOfFiles[i].isFile()) {
             System.out.println((i+1)+". " + listOfFiles[i].getName());
             files.add(listOfFiles[i].getName());
          } else if (listOfFiles[i].isDirectory()) {
             System.out.println("Directory: " + listOfFiles[i].getName());
           }
        }
        
        String inst[] = new String[131];
       //for (int i=0;i<files.size();i++) 
         // System.out.println("Print "+files.get(i));
       
        
        System.out.println("Choose the patterns to be played. and 0 to continue.");
         int m= input.nextInt(); 
             while (m!=0) {
              m--;
              String path="/home/kiit/music/Western_Classical/"+dir+"/"+files.get(m); 
              //String path=new File(files.get(m)).getAbsolutePath();                  
              MIDIfile.add(path);
              //System.out.println("Print: "+path);
              m= input.nextInt();
             }
       
        Synthesizer synth = MidiSystem.getSynthesizer();
        synth.open();
        MidiChannel channels[] = synth.getChannels();
        Soundbank bank = synth.getDefaultSoundbank();
        synth.loadAllInstruments(bank);
        Instrument instrs[] = synth.getLoadedInstruments();
          
  	for (int i=0; i < instrs.length; i++) {
   	     System.out.println((i+1)+". "+instrs[i].getName());
    	     inst[i+1] = instrs[i].getName();
	}
            
///////////Selecting instruments to play///////////////////////
         
        System.out.println("Enter instrument number for each pattern and 0 to continue ");
           int k=input.nextInt();
       while(k!=0) {             
             inum.add(k);
              k=input.nextInt();
       }       
        synth.close();
        int i=0;
       /* System.out.println("Enter processing delay in milliseconds:");
        float delt=input.nextFloat();
       */ 
        ExecutorService executor= Executors.newFixedThreadPool(MIDIfile.size());
        while(MIDIfile.size()!=0)        
         executor.submit(new Ex4(MIDIfile.remove(),inum.remove(),(++i))); 
         
        executor.shutdown();
         try {
          executor.awaitTermination(1,TimeUnit.DAYS);  
        }
        catch(InterruptedException e) {
        }
 }                        
}          

         
