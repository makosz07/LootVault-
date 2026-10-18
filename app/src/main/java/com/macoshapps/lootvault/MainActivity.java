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
  void show(String page){screen=page;root=column();root.setBackgroundColor(bg);root.setFitsSystemWindows(true);root.setPadding(0,0,0,dp(20));setContentView(root);ScrollView scroll=new ScrollView(this);content=column();pad(content,18);scroll.setFillViewport(true);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    if(page.equals("home"))home();else if(page.equals("resources"))resources();else if(page.equals("stats"))stats();else if(page.equals("settings"))settings();else if(page.equals("list"))list();navigation();}
  int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
  void heading(String s){add(content,text("LOOTVAULT",11,muted,true));space(content,8);add(content,text(s,28,white,true));space(content,18);}
  JSONArray types(){return db.optJSONArray("types");}JSONArray items(){return db.optJSONArray("items");}
  String typeName(String id){JSONArray a=types();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(x.optString("id").equals(id))return x.optString("name");}return id;}
  void tile(String title,String subtitle,int color,Runnable action){LinearLayout l=column();pad(l,20);l.setBackground(shape(color,24));add(l,text(title,20,white,true));space(l,8);add(l,text(subtitle,13,muted,false));add(content,l);l.setOnClickListener(v->action.run());space(content,12);}

  double amount(JSONObject x,String k){return x.optDouble(k,0);}
  double qty(JSONObject x){return Math.max(0,x.optDouble("quantity",1));}
  double total(String key){double sum=0;for(int i=0;i<items().length();i++){JSONObject x=items().optJSONObject(i);sum+=qty(x)*amount(x,key);}return sum;}
  String money(double n){return String.format(java.util.Locale.forLanguageTag("pl-PL"),"%,.2f zł",n);}
  String percent(double n){return String.format(java.util.Locale.forLanguageTag("pl-PL"),"%+.1f%%",n);}
  int deltaColor(double d){return d>0?0xff42d995:d<0?0xffff7777:muted;}
  String delta(double cost,double value){double d=value-cost;return (d>0?"+":"")+money(d)+(cost>0?"  ("+percent(d/cost*100)+")":"");}
  void financialCard(String label,double value,double cost){
    LinearLayout card=column();pad(card,22);card.setBackground(shape(panel,22));
    add(card,text(label,15,muted,false));space(card,8);add(card,text(money(value),30,white,true));space(card,9);
    add(card,text(t("Wynik względem ceny zakupu: ","Return on cost: ","Resultado frente al coste: ")+delta(cost,value),15,deltaColor(value-cost),true));
    add(content,card);space(content,14);
  }
  void home(){
    heading(t("Mój skarbiec","My vault","Mi tesoro"));
    financialCard(t("Całkowita wartość majątku","Total asset value","Valor total del patrimonio"),total("value"),total("purchase"));
    tile(t("Zainwestowano","Acquisition cost","Coste de adquisición"),money(total("purchase")),panel,()->show("stats"));
    tile(t("Wszystkie zasoby","All assets","Todos los recursos"),items().length()+" "+t("wpisów","entries","registros"),0xff263d68,()->{selected="";show("list");});
    add(content,button(t("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->chooseType()));space(content,18);
    add(content,text(t("Ostatnio dodane","Recently added","Añadidos recientemente"),21,white,true));space(content,12);
    for(int i=items().length()-1;i>=Math.max(0,items().length()-4);i--){
      JSONObject x=items().optJSONObject(i);String id=x.optString("id");
      tile(x.optString("name"),money(qty(x)*amount(x,"value")),panel,()->itemDetail(id));
    }
  }
  void resources(){
    heading(t("Twoje zasoby","Your assets","Tus recursos"));
    tile(t("Wszystkie zasoby","All assets","Todos los recursos"),items().length()+" "+t("wpisów","entries","registros"),0xff263d68,()->{selected="";show("list");});
    JSONArray a=types();
    for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);String id=x.optString("id"),name=x.optString("name");int count=0;double value=0;
      for(int j=0;j<items().length();j++){JSONObject item=items().optJSONObject(j);if(item.optString("type").equals(id)){count++;value+=qty(item)*amount(item,"value");}}
      tile("▣  "+name,count+" • "+money(value),panel,()->{selected=id;show("list");});
    }
    add(content,button(t("+ Nowy typ zasobu","+ New asset type","+ Nuevo tipo de recurso"),()->newType()));
  }
  void list(){
    heading(selected.isEmpty()?t("Wszystkie zasoby","All assets","Todos los recursos"):typeName(selected));
    JSONArray a=items();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(!selected.isEmpty()&&!selected.equals(x.optString("type")))continue;String id=x.optString("id");
      double cost=qty(x)*amount(x,"purchase"),value=qty(x)*amount(x,"value");
      tile(x.optString("name"),typeName(x.optString("type"))+" • "+money(value)+"\n"+delta(cost,value),panel,()->itemDetail(id));
    }
    add(content,button(t("+ Dodaj zasób","+ Add asset","+ Añadir recurso"),()->{if(selected.isEmpty())chooseType();else editItem(selected,null);}));
  }
  void stats(){
    heading(t("Statystyki majątku","Wealth statistics","Estadísticas patrimoniales"));
    financialCard(t("Aktualna wartość","Current value","Valor actual"),total("value"),total("purchase"));
    tile(t("Koszt nabycia","Acquisition cost","Coste de adquisición"),money(total("purchase")),panel,()->{});
    tile(t("Liczba zasobów","Total assets","Total de recursos"),""+items().length(),panel,()->{});
    add(content,text(t("Historia wartości majątku","Asset value history","Historial del patrimonio"),19,white,true));space(content,12);
    JSONArray snaps=db.optJSONArray("snapshots");
    if(snaps==null||snaps.length()<2)add(content,text(t("Wykres pojawi się po zapisaniu kolejnych zmian wartości.","The chart will appear after further valuation updates.","El gráfico aparecerá tras nuevas valoraciones."),14,muted,false));
    else {HistoryChart chart=new HistoryChart(this,snaps);content.addView(chart,new LinearLayout.LayoutParams(-1,dp(210)));}
    space(content,16);add(content,text(t("Wartość według typów","Value by asset type","Valor por tipo"),19,white,true));
    for(int i=0;i<types().length();i++){JSONObject type=types().optJSONObject(i);String id=type.optString("id");double sum=0,cost=0;for(int j=0;j<items().length();j++){JSONObject x=items().optJSONObject(j);if(id.equals(x.optString("type"))){sum+=qty(x)*amount(x,"value");cost+=qty(x)*amount(x,"purchase");}}tile(type.optString("name"),money(sum)+" • "+delta(cost,sum),panel,()->{selected=id;show("list");});}
  }
  class HistoryChart extends View {
    JSONArray points; android.graphics.Paint p=new android.graphics.Paint(3);
    HistoryChart(Context c,JSONArray a){super(c);points=a;setBackground(shape(panel,20));}
    protected void onDraw(android.graphics.Canvas c){super.onDraw(c);int n=points.length();if(n<2)return;
      float left=dp(20),top=dp(20),right=getWidth()-dp(20),bottom=getHeight()-dp(26);
      double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;
      for(int i=0;i<n;i++){double v=points.optJSONObject(i).optDouble("value");min=Math.min(min,v);max=Math.max(max,v);}
      if(max==min){max+=1;min-=1;}else{double margin=(max-min)*.12;max+=margin;min-=margin;}
      p.setColor(0xff41546b);p.setStrokeWidth(dp(1));c.drawLine(left,bottom,right,bottom,p);
      p.setColor(0xff5fa8ff);p.setStrokeWidth(dp(3));p.setStyle(android.graphics.Paint.Style.STROKE);
      android.graphics.Path path=new android.graphics.Path();
      for(int i=0;i<n;i++){float x=left+(right-left)*i/(n-1f);float y=(float)(bottom-(points.optJSONObject(i).optDouble("value")-min)/(max-min)*(bottom-top));if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
      c.drawPath(path,p);p.setStyle(android.graphics.Paint.Style.FILL);
      p.setTextSize(dp(12));p.setColor(white);c.drawText(money(points.optJSONObject(n-1).optDouble("value")),left,dp(16),p);
    }
  }
  void recordSnapshot(){try{
    JSONArray history=db.optJSONArray("snapshots");if(history==null){history=new JSONArray();db.put("snapshots",history);}
    JSONObject p=new JSONObject();p.put("time",System.currentTimeMillis());p.put("value",total("value"));p.put("purchase",total("purchase"));history.put(p);
    if(history.length()>500){JSONArray trim=new JSONArray();for(int i=history.length()-500;i<history.length();i++)trim.put(history.optJSONObject(i));db.put("snapshots",trim);}
    save();
  }catch(Exception e){android.util.Log.e("LootVault","Snapshot",e);}}
  void settings(){heading(t("Ustawienia","Settings","Ajustes"));add(content,text(t("Język","Language","Idioma"),18,white,true));space(content,8);add(content,button("Polski / English / Español",()->new AlertDialog.Builder(this).setItems(new String[]{"Polski","English","Español"},(d,w)->{language=new String[]{"pl","en","es"}[w];save();show("settings");}).show()));space(content,20);
    add(content,button(t("Eksport JSON","Export JSON","Exportar JSON"),()->{Intent in=new Intent(Intent.ACTION_CREATE_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);in.putExtra(Intent.EXTRA_TITLE,"lootvault-backup.json");startActivityForResult(in,11);}));
    space(content,12);add(content,button(t("Import JSON","Import JSON","Importar JSON"),()->{Intent in=new Intent(Intent.ACTION_OPEN_DOCUMENT);in.setType("application/json");in.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(in,12);}));
    space(content,28);add(content,text("LootVault • Macosh Apps\nVersion 0.3.0",13,muted,false));}
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

  double parseAmount(EditText e,double fallback){try{return Math.max(0,Double.parseDouble(e.getText().toString().trim().replace(",",".")));}catch(Exception ignored){return fallback;}}
  void editItem(String type,JSONObject existing){
    ScrollView sc=new ScrollView(this);LinearLayout form=column();pad(form,dp(16));sc.addView(form);
    EditText name=field(t("Nazwa","Name","Nombre")),category=field(t("Kategoria","Category","Categoría")),
      notes=field(t("Notatki","Notes","Notas")),quantity=field(t("Ilość","Quantity","Cantidad")),
      purchase=field(t("Cena zakupu za sztukę (zł)","Purchase price per unit (PLN)","Precio de compra por unidad (PLN)")),
      value=field(t("Aktualna wartość za sztukę (zł)","Current value per unit (PLN)","Valor actual por unidad (PLN)")),
      location=field(t("Miejsce przechowywania","Storage location","Ubicación"));
    quantity.setInputType(8194);purchase.setInputType(8194);value.setInputType(8194);
    if(existing!=null){name.setText(existing.optString("name"));category.setText(existing.optString("category"));notes.setText(existing.optString("notes"));quantity.setText(""+qty(existing));purchase.setText(""+amount(existing,"purchase"));value.setText(""+amount(existing,"value"));location.setText(existing.optString("location"));}
    else quantity.setText("1");
    for(EditText e:new EditText[]{name,category,quantity,purchase,value,location,notes}){add(form,e);space(form,dp(9));}
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle(existing==null?t("Dodaj zasób","Add asset","Añadir recurso"):t("Edytuj zasób","Edit asset","Editar recurso")).setView(sc)
      .setPositiveButton(t("Zapisz","Save","Guardar"),null).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).create();
    dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{
      if(name.getText().toString().trim().isEmpty()){name.setError(t("Wpisz nazwę","Enter a name","Introduce un nombre"));return;}
      try{
        JSONObject x=existing==null?new JSONObject():existing;
        if(existing==null){x.put("id",UUID.randomUUID().toString());x.put("type",type);}
        double previous=amount(x,"value"),current=parseAmount(value,0);
        x.put("name",name.getText().toString().trim());x.put("category",category.getText().toString().trim());x.put("notes",notes.getText().toString());
        x.put("quantity",parseAmount(quantity,1));x.put("purchase",parseAmount(purchase,0));x.put("value",current);x.put("location",location.getText().toString());
        if(existing==null)items().put(x);
        if(existing==null||previous!=current){JSONArray history=x.optJSONArray("history");if(history==null){history=new JSONArray();x.put("history",history);}JSONObject h=new JSONObject();h.put("time",System.currentTimeMillis());h.put("value",current);history.put(h);}
        recordSnapshot();selected=type;dialog.dismiss();show("list");
      }catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
    }));dialog.show();
  }
  void updateValuation(JSONObject x){
    EditText input=field(t("Nowa wartość za sztukę (zł)","New value per unit (PLN)","Nuevo valor por unidad (PLN)"));
    input.setInputType(8194);input.setText(""+amount(x,"value"));
    LinearLayout wrapper=column();pad(wrapper,dp(20));add(wrapper,input);
    new AlertDialog.Builder(this).setTitle(t("Aktualizuj wycenę","Update valuation","Actualizar valoración")).setView(wrapper)
      .setPositiveButton(t("Zapisz","Save","Guardar"),(d,w)->{
        try{double next=parseAmount(input,amount(x,"value"));x.put("value",next);JSONArray h=x.optJSONArray("history");if(h==null){h=new JSONArray();x.put("history",h);}JSONObject entry=new JSONObject();entry.put("time",System.currentTimeMillis());entry.put("value",next);h.put(entry);recordSnapshot();show("list");}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
      }).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();
  }
  void itemDetail(String id){
    JSONArray a=items();for(int i=0;i<a.length();i++){JSONObject x=a.optJSONObject(i);if(!x.optString("id").equals(id))continue;
      double cost=qty(x)*amount(x,"purchase"),value=qty(x)*amount(x,"value");
      JSONArray history=x.optJSONArray("history");
      StringBuilder info=new StringBuilder();
      info.append(typeName(x.optString("type"))).append("\n").append(x.optString("category")).append("\n\n");
      info.append(t("Cena zakupu: ","Purchase: ","Compra: ")).append(money(cost)).append("\n");
      info.append(t("Wartość: ","Value: ","Valor: ")).append(money(value)).append("\n");
      info.append(t("Wynik: ","Return: ","Resultado: ")).append(delta(cost,value)).append("\n");
      info.append(t("Ilość: ","Quantity: ","Cantidad: ")).append(qty(x)).append("\n");
      info.append(t("Lokalizacja: ","Location: ","Ubicación: ")).append(x.optString("location")).append("\n\n");
      info.append(x.optString("notes")).append("\n\n");
      info.append(t("Historia wycen: ","Valuation history: ","Historial de valoraciones: ")).append(history==null?0:history.length());
      new AlertDialog.Builder(this).setTitle(x.optString("name")).setMessage(info.toString())
        .setPositiveButton(t("Nowa wycena","New valuation","Nueva valoración"),(d,w)->updateValuation(x))
        .setNeutralButton(t("Edytuj","Edit","Editar"),(d,w)->editItem(x.optString("type"),x))
        .setNegativeButton(t("Więcej","More","Más"),(d,w)->new AlertDialog.Builder(this).setItems(new String[]{t("Usuń zasób","Delete asset","Eliminar recurso"),t("Zamknij","Close","Cerrar")},(dd,which)->{
          if(which!=0)return;new AlertDialog.Builder(this).setMessage(t("Usunąć zasób?","Delete asset?","¿Eliminar recurso?"))
            .setPositiveButton(t("Usuń","Delete","Eliminar"),(ddd,ww)->{JSONArray next=new JSONArray();for(int j=0;j<a.length();j++)if(!a.optJSONObject(j).optString("id").equals(id))next.put(a.optJSONObject(j));try{db.put("items",next);}catch(Exception ignored){}recordSnapshot();show("list");})
            .setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();
        }).show()).show();break;
    }
  }
  @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{Uri uri=data.getData();if(req==11){java.io.OutputStream out=getContentResolver().openOutputStream(uri);out.write(db.toString(2).getBytes("UTF-8"));out.close();Toast.makeText(this,"JSON exported",Toast.LENGTH_SHORT).show();}if(req==12){java.io.InputStream in=getContentResolver().openInputStream(uri);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1)bytes.write(buf,0,n);in.close();JSONObject imported=new JSONObject(bytes.toString("UTF-8"));if(!imported.has("types")||!imported.has("items"))throw new Exception("Invalid backup");new AlertDialog.Builder(this).setMessage(t("Import zastąpi obecne dane. Kontynuować?","Import will replace current data. Continue?","La importación reemplazará los datos actuales. ¿Continuar?")).setPositiveButton("OK",(d,w)->{db=imported;language=db.optString("language","pl");save();show("settings");}).setNegativeButton(t("Anuluj","Cancel","Cancelar"),null).show();}}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}}
}
