package com.netormium.client.util;
import java.io.*;
import javax.microedition.rms.RecordStore;
public final class HistoryStore {
 private static final String STORE="NetormiumHistory";
 public void save(BrowserHistory h){
  RecordStore rs=null;
  try{rs=RecordStore.openRecordStore(STORE,true);String[] a=h.entries();ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
   o.writeInt(1);o.writeInt(a.length);o.writeInt(h.index());for(int i=0;i<a.length;i++)o.writeUTF(a[i]==null?"":(a[i].length()>2048?a[i].substring(0,2048):a[i]));o.flush();
   byte[] d=b.toByteArray();if(rs.getNumRecords()==0)rs.addRecord(d,0,d.length);else rs.setRecord(1,d,0,d.length);
  }catch(Exception ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
 public void load(BrowserHistory h){
  RecordStore rs=null;try{rs=RecordStore.openRecordStore(STORE,false);if(rs.getNumRecords()==0)return;DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));
   if(in.readInt()!=1)return;int n=Math.min(100,Math.max(0,in.readInt()));int ix=in.readInt();String[] a=new String[n];for(int i=0;i<n;i++)a[i]=in.readUTF();h.restore(a,ix);
  }catch(Exception ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
}
