package com.netormium.client.util;
public final class TabManager {
 public static final int MAX_TABS=6;
 public static final class Tab {
  public String url,title,sessionId; public int scrollY;
  Tab(String u,String t,String s,int y){url=u;title=t;sessionId=s;scrollY=y;}
 }
 private final Tab[] tabs=new Tab[MAX_TABS]; private int count=1,active=0;
 public TabManager(){tabs[0]=new Tab("","New Tab","",0);}
 public int size(){return count;} public int activeIndex(){return active;}
 public Tab current(){return tabs[active];}
 public Tab get(int i){return i>=0&&i<count?tabs[i]:null;}
 public int add(String u,String t){if(count==MAX_TABS)return -1;tabs[count]=new Tab(u==null?"":u,t==null?"New Tab":t,"",0);active=count++;return active;}
 public boolean select(int i){if(i<0||i>=count)return false;active=i;return true;}
 public int next(){active=(active+1)%count;return active;}
 public void updateCurrent(String u,String t,int y){if(u!=null)current().url=u;if(t!=null&&t.length()>0)current().title=t;current().scrollY=Math.max(0,y);}
 public void setCurrentScroll(int y){current().scrollY=Math.max(0,y);}
 public void setCurrentSessionId(String s){current().sessionId=s==null?"":s;}
 public String[] labels(){String[] a=new String[count];for(int i=0;i<count;i++)a[i]=(i==active?"* ":"")+(i+1)+". "+tabs[i].title;return a;}
 public void restore(String[] u,String[] t,String[] s,int[] y,int a){
  count=0;int n=Math.min(MAX_TABS,u==null?0:u.length);
  for(int i=0;i<n;i++)tabs[count++]=new Tab(u[i]==null?"":u[i],t!=null&&i<t.length&&t[i]!=null?t[i]:"New Tab",s!=null&&i<s.length&&s[i]!=null?s[i]:"",y!=null&&i<y.length?Math.max(0,y[i]):0);
  if(count==0){count=1;tabs[0]=new Tab("","New Tab","",0);}active=Math.max(0,Math.min(count-1,a));
 }
}
