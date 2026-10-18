package com.macoshapps.lootvault;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
  final int bg=0xff0e1523, panel=0xff1c293b, accent=0xff345fe8, white=Color.WHITE, muted=0xffa8b8cf;
  LinearLayout root,content,nav; JSONObject db=new JSONObject(); String screen="home", selected=""; int photoRequest=14;
  String language="pl"; Uri pendingPhoto; String editingId=null;
  String t(String pl,String en,String es){return language.equals("en")?en:language.equals("es")?es:pl;}
  public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(bg);getWindow().setNavigationBarColor(bg);getWindow().getDecorView().setSystemUiVisibility(0);load();seedDemo();show("home");}
  void load(){try{db=new JSONObject(getPreferences(0).getString("db","{}"));if(!db.has("types"))db.put("types",new JSONArray());if(!db.has("items"))db.put("items",new JSONArray());language=db.optString("language","pl");}catch(Exception e){db=new JSONObject();try{db.put("types",new JSONArray());db.put("items",new JSONArray());}catch(Exception ignored){}}}

  void seedDemo(){
    try{
      if(db.optBoolean("demoSeeded",false))return;
      String[][] samples={
        {"Przedmioty","Laptop","Słuchawki","Plecak","Zegarek","Aparat fotograficzny"},
        {"Gry i aplikacje","Minecraft","Stardew Valley","Terraria","Hollow Knight","Blender"},
        {"Książki","Sapiens","Atomowe nawyki","Medytacje","Bogaty ojciec, biedny ojciec","Władca Pierścieni"}
      };
      for(String[] group:samples){
        String id="demo-"+group[0];boolean found=false;
        for(int i=0;i<types().length();i++)if(types().optJSONObject(i).optString("id").equals(id))found=true;
        if(!found){JSONObject type=new JSONObject();type.put("id",id);type.put("name",group[0]);types().put(type);}
        for(int i=1;i<group.length;i++){
          JSONObject item=new JSONObject();
          item.put("id",UUID.randomUUID().toString());item.put("type",id);
          item.put("name",group[i]);item.put("category","Przykładowe");item.put("notes","Przykładowy wpis. Możesz go edytować lub usunąć.");
          items().put(item);
        }
      }
      db.put("demoSeeded",true);save();
    }catch(Exception e){android.util.Log.e("LootVault","Sample data",e);}
  }
  void save(){try{db.put("language",language);}catch(Exception ignored){}getPreferences(0).edit().putString("db",db.toString()).apply();}
  GradientDrawable shape(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
  TextView text(String s,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(s);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
  LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
  void pad(View v,int n){v.setPadding(n,n,n,n);}
  void add(LinearLayout parent,View child){parent.addView(child,new LinearLayout.LayoutParams(-1,-2));}
  void space(LinearLayout p,int n){View v=new View(this);p.addView(v,new LinearLayout.LayoutParams(1,n));}
  TextView button(String title,Runnable r){TextView v=text(title,16,white,true);pad(v,18);v.setGravity(Gravity.CENTER);v.setBackground(shape(accent,22));v.setOnClickListener(w->r.run());return v;}
  void show(String page){screen=page;root=column();root.setBackgroundColor(bg);setContentView(root);ScrollView scroll=new ScrollView(this);content=column();pad(content,18);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    if(page.equals("home"))home();else if(page.equals("resources"))resources();else if(page.equals("stats"))stats();else if(page.equals("settings"))settings();else if(page.equals("list"))list();navigation();}
  void heading(String s){add(content,text("LOOTVAULT",11,muted,true));space(content,8);add(content,text(s,28,white,true));space(content,18);}
  JSONArray types(){return db.optJSONArray("types");}JSONArray items(){return db.optJSONArray("items");}
  String typeName(String id){JSONArray a=types();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x.optString("id").equals(id))return x.optString("name");}return id;}
  void tile(String title,String subtitle,int color,Runnable action){LinearLayout l=column();pad(l,20);l.setBackground(shape(color,24));add(l,text(title,20,white,true));space(l,8);add(l,text(subtitle,13,muted,false));add(content,l);l.setOnClickListener(v->action.run());space(content,12);}
  void home(){heading(t("Twoja kolekcja","Your collection","Tu colección"));tile(t("Wszystkie zasoby","All assets","Todos los recursos"),items().length()+" "+t("zasobów","assets","recursos"),0xff263d68,()->{selected="";show("list");});tile(t("Typy zasobów","Asset types","Tipos de recursos"),types().length()+" "+t("kolekcji","collections","colecciones"),panel,()->show("resources"));add(content,button(t("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->chooseType()));}
  void resources(){heading(t("Twoje zasoby","Your assets","Tus recursos"));tile(t("Wszystkie zasoby","All assets","Todos los recursos"),items().length()+" "+t("zasobów","assets","recursos"),0xff263d68,()->{selected="";show("list");});
    JSONArray a=types();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);String id=x.optString("id"),name=x.optString("name");int count=0;for(int j=0;j<items().length();j++)if(items().optJSONObject(j).optString("type").equals(id))count++;tile("▣  "+name,count+" "+t("zasobów","assets","recursos"),panel,()->{selected=id;show("list");});}
    add(content,button(t("+ Nowy typ zasobu","+ New asset type","+ Nuevo tipo de recurso"),()->newType()));}
  void list(){heading(selected.isEmpty()?t("Wszystkie zasoby","All assets","Todos los recursos"):typeName(selected));JSONArray a=items();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(!selected.isEmpty()&&!selected.equals(x.optString("type")))continue;String id=x.optString("id");tile(x.optString("name"),typeName(x.optString("type"))+"  •  "+x.optString("category"),panel,()->itemDetail(id));}add(content,button(t("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->{if(selected.isEmpty())chooseType();else editItem(selected,null);}));}
  void stats(){heading(t("Statystyki","Statistics","Estadísticas"));tile(t("Liczba zasobów","Total assets","Total de recursos"),""+items().length(),panel,()->{});tile(t("Typy zasobów","Asset types","Tipos de recursos"),""+types().length(),panel,()->{});}
  void settings(){heading(t("Ustawienia","Settings","Ajustes"));add(content,text(t("Język","Language","Idioma"),18,white,true));space(content,8);add(content,button("Polski / English / Español",()->new AlertDialog.Builder(this).setItems(new String[]{"Polski","English","Español"},(d,w)->{language=new String[]{"pl","en","es"}[w];save();show("settings");}).show()));space(content,20);
    add(content,button(t("Eksport JSON","Export JSON","Exportar JSON"),()->{Intent in=new Intent(Intent.ACTION_CREATE_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);in.putExtra(Intent.EXTRA_TITLE,"lootvault-backup.json");startActivityForResult(in,11);}));
    space(content,12);add(content,button(t("Import JSON","Import JSON","Importar JSON"),()->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,12);}));
    space(content,28);add(content,text("LootVault • Macosh Apps\nVersion 0.2.0",13,muted,false));}
  void navigation(){
    nav=new LinearLayout(this);nav.setOrientation(LinearLayout.HORIZONTAL);
    nav.setBackgroundColor(panel);
    String[] icons={"⌂","▦","▥","⚙"};
    String[] labels={t("Pulpit","Home","Inicio"),t("Zasoby","Assets","Recursos"),t("Statystyki","Stats","Estadísticas"),t("Ustawienia","Settings","Ajustes")};
    String[] pages={"home","resources","stats","settings"};
    for(int i=0;i<4;i++){
      final String page=pages[i];boolean active=page.equals(screen);
      LinearLayout cell=column();cell.setGravity(Gravity.CENTER);
      cell.setPadding(3,10,3,10);
      TextView iconView=text(icons[i],28,active?0xffa6c5ff:muted,true);
      iconView.setGravity(Gravity.CENTER);
      cell.addView(iconView,new LinearLayout.LayoutParams(-1,40));
      TextView label=text(labels[i],12,active?white:muted,active);
      label.setGravity(Gravity.CENTER);
      label.setSingleLine(true);label.setTextSize(11);
      cell.addView(label,new LinearLayout.LayoutParams(-1,25));
      nav.addView(cell,new LinearLayout.LayoutParams(0,86,1));
      cell.setOnClickListener(v->show(page));
    }
    root.addView(nav,new LinearLayout.LayoutParams(-1,86));
  }
  void newType(){LinearLayout form=column();pad(form,20);EditText name=field(t("Nazwa typu","Type name","Nombre del tipo"));add(form,name);new AlertDialog.Builder(this).setTitle(t("Nowy typ zasobu","New asset type","Nuevo tipo de recurso")).setView(form).setPositiveButton(t("Zapisz","Save","Guardar"),(d,w)->{String s=name.getText().toString().trim();if(s.isEmpty())return;JSONObject x=new JSONObject();try{x.put("id",UUID.randomUUID().toString());x.put("name",s);types().put(x);save();show("resources");}catch(Exception ignored){}}).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();}
  EditText field(String hint){EditText e=new EditText(this);e.setTextColor(white);e.setHintTextColor(muted);e.setHint(hint);e.setSingleLine(true);return e;}
  void chooseType(){JSONArray a=types();if(a.length()==0){new AlertDialog.Builder(this).setMessage(t("Najpierw utwórz typ zasobu.","Create an asset type first.","Primero crea un tipo de recurso.")).setPositiveButton("OK",(d,w)->newType()).show();return;}String[] names=new String[a.length()];for(int i=0;i<a.length();i++)names[i]=a.optJSONObject(i).optString("name");new AlertDialog.Builder(this).setTitle(t("Wybierz typ","Choose type","Elige tipo")).setItems(names,(d,w)->editItem(a.optJSONObject(w).optString("id"),null)).show();}
  void editItem(String type,JSONObject existing){LinearLayout form=column();pad(form,20);EditText name=field(t("Nazwa","Name","Nombre")),category=field(t("Kategoria (opcjonalna)","Category (optional)","Categoría (opcional)")),notes=field(t("Notatki","Notes","Notas"));if(existing!=null){name.setText(existing.optString("name"));category.setText(existing.optString("category"));notes.setText(existing.optString("notes"));}add(form,name);add(form,category);add(form,notes);
    new AlertDialog.Builder(this).setTitle(existing==null?t("Dodaj zasób","Add asset","Añadir recurso"):t("Edytuj zasób","Edit asset","Editar recurso")).setView(form).setPositiveButton(t("Zapisz","Save","Guardar"),(d,w)->{if(name.getText().toString().trim().isEmpty())return;try{JSONObject x=existing==null?new JSONObject():existing;if(existing==null){x.put("id",UUID.randomUUID().toString());x.put("type",type);}x.put("name",name.getText().toString());x.put("category",category.getText().toString());x.put("notes",notes.getText().toString());if(existing==null)items().put(x);save();selected=type;show("list");}catch(Exception ignored){}}).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();}
  void itemDetail(String id){JSONArray a=items();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(!x.optString("id").equals(id))continue;new AlertDialog.Builder(this).setTitle(x.optString("name")).setMessage(typeName(x.optString("type"))+"\n"+x.optString("category")+"\n"+x.optString("notes")).setPositiveButton(t("Edytuj","Edit","Editar"),(d,w)->editItem(x.optString("type"),x)).setNeutralButton(t("Usuń","Delete","Eliminar"),(d,w)->new AlertDialog.Builder(this).setMessage(t("Usunąć zasób?","Delete asset?","¿Eliminar recurso?")).setPositiveButton(t("Usuń","Delete","Eliminar"),(dd,ww)->{JSONArray next=new JSONArray();for(int j=0;j<a.length();j++)if(!a.optJSONObject(j).optString("id").equals(id))next.put(a.optJSONObject(j));try{db.put("items",next);}catch(Exception ignored){}save();show("list");}).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show()).setNegativeButton(t("Zamknij","Close","Cerrar"),null).show();break;}}
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{Uri uri=data.getData();if(req==11){java.io.OutputStream out=getContentResolver().openOutputStream(uri);out.write(db.toString(2).getBytes("UTF-8"));out.close();Toast.makeText(this,"JSON exported",Toast.LENGTH_SHORT).show();}if(req==12){java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)bytes.write(buf,0,n);in.close();JSONObject imported=new JSONObject(bytes.toString("UTF-8"));if(!imported.has("types")||!imported.has("items"))throw new Exception("Invalid backup");new AlertDialog.Builder(this).setMessage(t("Import zastąpi obecne dane. Kontynuować?","Import will replace current data. Continue?","La importación reemplazará los datos actuales. ¿Continuar?")).setPositiveButton("OK",(d,w)->{db=imported;language=db.optString("language","pl");save();show("settings");}).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();}}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
}
