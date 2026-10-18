package com.macoshapps.lootvault;
import android.content.*;import android.util.*;import org.json.*;import java.io.*;import java.util.*;
public final class VaultStore {
 public final Context context; public JSONObject data;
 public VaultStore(Context c){context=c;load();}
 public JSONArray types(){return data.optJSONArray("types");}
 public JSONArray items(){return data.optJSONArray("items");}
 public JSONArray snapshots(){return data.optJSONArray("snapshots");}
 public void load(){
  try{
   File f=new File(context.getFilesDir(),"vault.json");
   if(f.exists()){byte[] b=new byte[(int)f.length()];try(FileInputStream in=new FileInputStream(f)){int off=0,n;while(off<b.length&&(n=in.read(b,off,b.length-off))>0)off+=n;}data=new JSONObject(new String(b,"UTF-8"));}
   else{String old=context.getSharedPreferences("MainActivity",0).getString("db","");if(old.isEmpty())old=context.getSharedPreferences("MainActivity",0).getString("db","");data=old.isEmpty()?new JSONObject():new JSONObject(old);}
   if(!data.has("types"))data.put("types",new JSONArray());
   if(!data.has("items"))data.put("items",new JSONArray());
   if(!data.has("snapshots"))data.put("snapshots",new JSONArray());
   if(!data.has("schema"))data.put("schema",2);
   seedSamples();save();
  }catch(Exception e){Log.e("LootVault","load",e);data=new JSONObject();try{data.put("types",new JSONArray());data.put("items",new JSONArray());data.put("snapshots",new JSONArray());}catch(Exception ignored){}}
 }

 /** Demo data is clearly marked and excluded from the real financial dashboard. */
 private void seedSamples(){
  try{
   if(data.optBoolean("starterCreated",false)||types().length()>0||items().length()>0)return;
   String[][] samples={
    {"Przedmioty","Laptop","Słuchawki","Plecak","Zegarek","Aparat fotograficzny"},
    {"Gry i aplikacje","Minecraft","Stardew Valley","Terraria","Hollow Knight","Blender"},
    {"Książki","Sapiens","Atomowe nawyki","Medytacje","Władca Pierścieni","Zbrodnia i kara"}
   };
   for(String[] group:samples){
    String typeId=id();JSONObject type=new JSONObject();type.put("id",typeId);type.put("name",group[0]);types().put(type);
    for(int i=1;i<group.length;i++){JSONObject x=new JSONObject();x.put("id",id());x.put("type",typeId);x.put("name",group[i]);x.put("category","Przykładowe");x.put("demo",true);x.put("quantity",1);x.put("purchase",0);x.put("value",0);items().put(x);}
   }
   data.put("starterCreated",true);
  }catch(Exception e){Log.e("LootVault","demo",e);}
 }
 public void save(){try{
  File f=new File(context.getFilesDir(),"vault.json");File tmp=new File(context.getFilesDir(),"vault.tmp");
  try(FileOutputStream out=new FileOutputStream(tmp)){out.write(data.toString().getBytes("UTF-8"));out.getFD().sync();}
  if(!tmp.renameTo(f)){try(FileInputStream in=new FileInputStream(tmp);FileOutputStream out=new FileOutputStream(f)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}tmp.delete();}
 }catch(Exception e){Log.e("LootVault","save",e);}}
 public static String id(){return UUID.randomUUID().toString();}
 public JSONObject byId(String id){JSONArray a=items();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&id.equals(x.optString("id")))return x;}return null;}
 public String typeName(String id){JSONArray a=types();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x!=null&&id.equals(x.optString("id")))return x.optString("name");}return "";}
 public void addType(String name){try{JSONObject x=new JSONObject();x.put("id",id());x.put("name",name);types().put(x);save();}catch(Exception ignored){}}
 public void deleteItem(String id){try{JSONArray a=new JSONArray();for(int i=0;i<items().length();i++){JSONObject x=items().optJSONObject(i);if(x!=null&&!id.equals(x.optString("id")))a.put(x);}data.put("items",a);snapshot();save();}catch(Exception ignored){}}
 public static double number(JSONObject x,String key,double fallback){if(!x.has(key))return fallback;return x.optDouble(key,fallback);}
 public static double quantity(JSONObject x){return Math.max(0,number(x,"quantity",1));}
 public static double price(JSONObject x,String key){return Math.max(0,number(x,key,0));}
 public double sum(String key){java.math.BigDecimal total=java.math.BigDecimal.ZERO;for(int i=0;i<items().length();i++){JSONObject x=items().optJSONObject(i);if(x!=null&&!x.optBoolean("demo",false)){java.math.BigDecimal q=java.math.BigDecimal.valueOf(quantity(x));java.math.BigDecimal unit=java.math.BigDecimal.valueOf(price(x,key));total=total.add(q.multiply(unit));}}return total.setScale(2,java.math.RoundingMode.HALF_UP).doubleValue();}
 public int realCount(){int n=0;for(int i=0;i<items().length();i++)if(!items().optJSONObject(i).optBoolean("demo",false))n++;return n;}
 public void snapshot(){try{JSONObject x=new JSONObject();x.put("time",System.currentTimeMillis());x.put("value",sum("value"));x.put("purchase",sum("purchase"));snapshots().put(x);if(snapshots().length()>1500){JSONArray keep=new JSONArray();for(int i=snapshots().length()-1500;i<snapshots().length();i++)keep.put(snapshots().optJSONObject(i));data.put("snapshots",keep);}}catch(Exception ignored){}}
 public String exportJson(){try{JSONObject out=new JSONObject(data.toString());out.put("schema",2);out.put("exportedAt",System.currentTimeMillis());return out.toString(2);}catch(Exception e){return data.toString();}}
 public void importJson(String json)throws Exception{JSONObject incoming=new JSONObject(json);if(incoming.optJSONArray("types")==null||incoming.optJSONArray("items")==null)throw new Exception("Invalid JSON backup");JSONArray a=incoming.optJSONArray("items");for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x==null||x.optString("id").isEmpty())throw new Exception("Invalid asset");}if(incoming.optJSONArray("snapshots")==null)incoming.put("snapshots",new JSONArray());data=incoming;save();}
}
