package com.netormium.client.util;
import javax.microedition.rms.RecordStore;
/** Persistent lightweight preferences for the Netormium MIDlet. */
public final class BrowserPreferences {
 private static final String STORE="NetormiumPrefs";
 private static final int VERSION=1;
 public boolean loadDataSaver(boolean fallback) {
  RecordStore rs=null;
  try {
   rs=RecordStore.openRecordStore(STORE,false);
   if(rs.getNumRecords()<1)return fallback;
   byte[] d=rs.getRecord(1);
   if(d==null||d.length<2||(d[0]&255)!=VERSION)return fallback;
   return d[1]!=0;
  } catch(Exception ignored){return fallback;}
  finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
 public void saveDataSaver(boolean enabled) {
  RecordStore rs=null;
  try {
   rs=RecordStore.openRecordStore(STORE,true);
   byte[] d=new byte[]{(byte)VERSION,(byte)(enabled?1:0)};
   if(rs.getNumRecords()<1)rs.addRecord(d,0,d.length);
   else rs.setRecord(1,d,0,d.length);
  } catch(Exception ignored){}
  finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
}
