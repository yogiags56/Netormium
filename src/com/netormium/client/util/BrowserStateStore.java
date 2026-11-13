package com.netormium.client.util;
import java.io.*;
import javax.microedition.rms.RecordStore;
public final class BrowserStateStore {
 private static final String STORE="NetormiumState";
 public void save(TabManager tabs){
  RecordStore rs=null;
  try{rs=RecordStore.openRecordStore(STORE,true);ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(b);
   o.writeInt(1);o.writeInt(tabs.size());o.writeInt(tabs.activeIndex());
   for(int i=0;i<tabs.size();i++){TabManager.Tab t=tabs.get(i);o.writeUTF(limit(t.url,2048));o.writeUTF(limit(t.title,256));o.writeUTF(limit(t.sessionId,64));o.writeInt(t.scrollY);}
   o.flush();byte[] d=b.toByteArray();if(rs.getNumRecords()==0)rs.addRecord(d,0,d.length);else rs.setRecord(1,d,0,d.length);
  }catch(Exception ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
 public void load(TabManager tabs){
  RecordStore rs=null;
  try{rs=RecordStore.openRecordStore(STORE,false);if(rs.getNumRecords()==0)return;DataInputStream in=new DataInputStream(new ByteArrayInputStream(rs.getRecord(1)));
   if(in.readInt()!=1)return;int n=Math.min(TabManager.MAX_TABS,Math.max(0,in.readInt()));int a=in.readInt();
   String[] u=new String[n],t=new String[n],s=new String[n];int[] y=new int[n];
   for(int i=0;i<n;i++){u[i]=in.readUTF();t[i]=in.readUTF();s[i]=in.readUTF();y[i]=in.readInt();}tabs.restore(u,t,s,y,a);
  }catch(Exception ignored){}finally{try{if(rs!=null)rs.closeRecordStore();}catch(Exception ignored){}}
 }
 private String limit(String s,int n){if(s==null)return "";return s.length()>n?s.substring(0,n):s;}
}
