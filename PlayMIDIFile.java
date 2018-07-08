import java.io.*;
import java.util.*;
import javax.sound.midi.*;
import java.lang.*;
import java.util.concurrent.*;

public class Ex4 implements Runnable {

    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;
    public static final int SET_TEMPO = 0x51; 
    private String file;
    private int s;
    private int id;
    
    public Ex4(String mfile,int ist,int id) {
     this.file=mfile;
     this.s=ist;
     this.id=id;
    } 
    public static long tickTomillis(long ticks,int bpm) {
         
         float kMillisecondsPerQuarterNote=0;
         float kMillisecondsPerTick=0;
         long dTMills=0;
         kMillisecondsPerQuarterNote = 60000/bpm; 
	 kMillisecondsPerTick = kMillisecondsPerQuarterNote /1000;//(denom=PPQN)  
         dTMills = (long)(ticks*kMillisecondsPerTick);
         return (dTMills);
    }
    
    public static long ReadVarLen(String file) throws IOException {
         
         FileInputStream in =null;
         long value=0;
         int currentByte=0;
         try {
          in = new FileInputStream(file);                
          if(((value=in.read()) & 0X80)!=0) {
           value&=0X7F;
           do {
           value=(value <<7)+((currentByte=in.read())&0X7F);
           }while((currentByte & 0X80)!=0);
         }
         }
         finally {
         
          if (in != null) {
            in.close();
          }
         }
         return (value);
    }  
    
    public void run()  {
               
         int tempo,bpm=0;
         long dTMills=0;
         long ticks;
         long PPQ;
         List<String> it = new ArrayList<String>(); 
         List<Instrument> ist = new ArrayList<Instrument>();      
        // Queue<String> MIDIfile = new LinkedList<String>();
          
         String inst[] = new String[131];
         Scanner input = new Scanner(System.in);
      
/////////////Setting a sequencer//////////////////////////////
        
         try {
	
///////////Opening synth and loading sound bank/////////////////
	
          Synthesizer synth = MidiSystem.getSynthesizer();
          synth.open();
          MidiChannel channels[] = synth.getChannels();
          Soundbank bank = synth.getDefaultSoundbank();
          synth.loadAllInstruments(bank);
          Instrument instrs[] = synth.getLoadedInstruments();
          
  	   for (int i=0; i < instrs.length; i++) {
    			
    			inst[i+1] = instrs[i].getName();
	   }
            
///////////Selecting instruments to play///////////////////////
           
             it.add(inst[s]);
             

            for (int i=0; i < instrs.length; i++) {
            
		if (instrs[i].getName().equals(it.get(0))) 
                    
                    ist.add(instrs[i]); 
                    
            }

          
   	    
   	 
///////////To change the default instruments///////////////	
        
           Instrument st=ist.get(0);          
           Patch instrumentPatch =st.getPatch();
           channels[0].programChange(instrumentPatch.getBank(),instrumentPatch.getProgram());
           Sequencer sequencer;
	   sequencer = MidiSystem.getSequencer(); 
           Sequence sequence = MidiSystem.getSequence(new File(file));
           
           System.out.println("Track "+id+" start:"); 
           sequencer.setSequence(sequence);
           
           PPQ = ReadVarLen(file);
         //  System.out.println("PPQ value: "+PPQ);    
           Queue<MIDI> OnQueue = new LinkedList<MIDI>();
           Queue<MIDI> OffQueue = new LinkedList<MIDI>();
           List<MIDI> Off = new ArrayList<MIDI>();

          
           int trackNumber = 0,k=0;
           for (Track track :  sequence.getTracks()) {
            
             trackNumber++;
             System.out.println();
             for (int i=0; i < track.size(); i++) { 
                
                MidiEvent event = track.get(i);
                ticks=event.getTick();
                MidiMessage message = event.getMessage();
 
 ////////////Extracting BPM from meta track///////////////
                
                if (message instanceof MetaMessage) {
                   MetaMessage mm = (MetaMessage) message;
                   if(mm.getType()==SET_TEMPO){ 
                      
                      byte[] data = mm.getData();
		      tempo = (data[0] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[2] & 0xff);
		      bpm = 60000000 / tempo;
		     
               	 }
                }         
                if (message instanceof ShortMessage) {
                    
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == NOTE_ON) {
			
                        int key = sm.getData1();                
                        int velocity = sm.getData2();
                        MIDI m=new MIDI();
                        m.timestamp=ticks;
                        m.messg="on";
                        m.k=key;
                        m.vel=velocity;
                        OnQueue.add(m); ///storing On Events sorted acc. to increasing timestamp  
                    } 


                    
                    else if (sm.getCommand() == NOTE_OFF) {
                      
                        int key = sm.getData1();
                        int velocity = sm.getData2();
                        MIDI n=new MIDI();
                        n.timestamp=ticks;
                        n.messg="off";
                        n.k=key;
                        n.vel=velocity;
                        Off.add(n);  ///storing Off Events sorted acc. to increasing timestamp                     
                    }                                               
                }
                else {
                   // System.out.println("Other message: " + message.getClass());
                }
               
            }
          
        }
        
        System.out.println();
        int size = OnQueue.size();

//////////////storing off events sorted acc. to corresponding note on events////////////////
        
         trackNumber = 0;
         k=0;
         int c=0;
         long initdelayMillis=0,millis1=0;
          for (Track track :  sequence.getTracks()) {
            
             trackNumber++;
             System.out.println();
             for (int i=0; i < track.size(); i++) { 
                
                MidiEvent event = track.get(i);
                ticks=event.getTick();
                MidiMessage message = event.getMessage();
                         
                if (message instanceof ShortMessage) {
                    
                    ShortMessage sm = (ShortMessage) message;
                    if (sm.getCommand() == NOTE_ON) {
                        int key = sm.getData1();                
                        int velocity = sm.getData2();
                        MIDI o=new MIDI();
                        
                        if(Off.size()!=0) {
                        for(int j=0;j<Off.size();j++) {
                             o=Off.get(j);
                             if(o.k==key) {
                               Off.remove(j);
                               break;
                             }  
                        }
                        OffQueue.add(o);
                        }
                    }
                }
            }
        }           
        
        
//////////////evaluating note duration//////////////

        long ts1=0;                       
        while(OnQueue.size()!=0){
        
        MIDI head = OnQueue.peek(); 
        long ts=head.timestamp;
        if(ts!=initdelayMillis) 
           initdelayMillis=0;
        List<MIDI> listOn = new ArrayList<MIDI> ();
        List<MIDI> listOff = new ArrayList<MIDI> ();
        
        while(OnQueue.peek().timestamp==ts) {             
            MIDI h1=OnQueue.remove();
            listOn.add(h1);
            MIDI h2=OffQueue.remove();
            listOff.add(h2);
            if(OnQueue.peek()==null)
            break;
        }
        long max=0;
        for(int i=0;i<listOff.size();i++) {
          if(listOff.get(i).timestamp>max)
           max=listOff.get(i).timestamp;
        }
               
        long delay=ts-ts1;
        long delayMillis=tickTomillis(delay,bpm);
        if(delayMillis!=0)
         Thread.sleep(delayMillis);       
        ts1=max;        
        ExecutorService executor= Executors.newFixedThreadPool(listOn.size());

        for(int i=0;i<listOn.size();i++) {
         
         long deltime=(listOff.get(i).timestamp)-(listOn.get(i).timestamp);
    
////////Tick to millisec conversion/////////////
         
         dTMills=tickTomillis(deltime,bpm); ////Duration of a note in millis        
         int key=listOn.get(i).k;
         int vel=listOn.get(i).vel;         
         channels[0].noteOn(key,vel);         
         System.out.println();      
         executor.submit(new Processor(i,dTMills));         
        }
        executor.shutdown();
        
         
        try {
          executor.awaitTermination(1,TimeUnit.DAYS);  
        }
        catch(InterruptedException e) {
        }
        for(int i=0;i<listOn.size();i++) {
          int key=listOn.get(i).k;
          
           channels[0].noteOff(key); 
        }  
        
                     
    }
    
    }
    catch (Exception exc) {
           
       exc.printStackTrace();
    }
    
   }
      
  }  
